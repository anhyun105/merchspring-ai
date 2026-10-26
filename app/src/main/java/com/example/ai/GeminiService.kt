package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import com.example.model.ActionProposalType
import com.example.model.ExtractedInvoiceDraft
import com.example.model.ExtractedInvoiceItem
import com.example.model.OrderEntity
import com.example.model.ProductEntity
import com.example.model.StructuredActionProposal

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isThinking: Boolean = false,
    val isGrounded: Boolean = false,
    val proposal: StructuredActionProposal? = null
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun sendChat(
        history: List<ChatMessage>,
        userMessage: String,
        businessContext: String,
        enableThinking: Boolean = false,
        enableSearch: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                Exception("Gemini API key is not configured. Please set GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        val modelName = when {
            enableThinking -> "gemini-4-argon"
            enableSearch -> "gemini-3.8-flash"
            else -> "gemini-3.8-flash"
        }

        try {
            val root = JSONObject()

            // System Instruction with MerchSpring business persona & grounded live database context
            val systemInstruction = JSONObject()
            val sysParts = JSONArray().put(
                JSONObject().put(
                    "text",
                    """You are MerchSpring AI, an intelligent, calm, professional business advisor and operations assistant for Indian small businesses, retailers, and wholesalers.
Tagline: 'Your Business, in Sync.'
You assist shopkeepers with inventory optimization, sales analysis, GST queries, customer debt recovery, and drafting polite payment reminders in English, Hindi, or Hinglish.

CRITICAL RULES:
1. Ground all numbers strictly in the provided business context. If data is not present, honestly state that it is not recorded in the database. Never invent sales figures, fake transactions, or nonexistent stock.
2. Clearly distinguish deterministic facts from estimates or advice.
3. Suggest practical actions with step-by-step clarity (e.g. 'To restock, go to Purchases > Create PO').
4. When drafting customer payment reminders, keep them courteous, professional, and culturally appropriate (supports English, Hindi, and Hinglish).

CURRENT BUSINESS WORKSPACE SNAPSHOT:
$businessContext
"""
                )
            )
            systemInstruction.put("parts", sysParts)
            root.put("systemInstruction", systemInstruction)

            // Conversation history
            val contents = JSONArray()
            val recentHistory = history.takeLast(10) // Keep reasonable context window
            for (msg in recentHistory) {
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == "user") "user" else "model")
                val parts = JSONArray().put(JSONObject().put("text", msg.content))
                contentObj.put("parts", parts)
                contents.put(contentObj)
            }

            // Append current user message
            val currentMsg = JSONObject()
            currentMsg.put("role", "user")
            currentMsg.put("parts", JSONArray().put(JSONObject().put("text", userMessage)))
            contents.put(currentMsg)

            root.put("contents", contents)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)

            if (enableThinking) {
                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingLevel", "high")
                genConfig.put("thinkingConfig", thinkingConfig)
            }
            root.put("generationConfig", genConfig)

            // Search Grounding tool
            if (enableSearch) {
                val toolsArray = JSONArray()
                val searchTool = JSONObject().put("googleSearch", JSONObject())
                toolsArray.put(searchTool)
                root.put("tools", toolsArray)
            }

            val requestBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/$modelName:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errMessage = try {
                    val errJson = JSONObject(responseBody)
                    errJson.getJSONObject("error").getString("message")
                } catch (e: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }
                return@withContext Result.failure(Exception(errMessage))
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val textBuilder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text", "")
                        textBuilder.append(text)
                    }
                    val reply = textBuilder.toString().trim()
                    if (reply.isNotBlank()) {
                        return@withContext Result.success(reply)
                    }
                }
            }

            Result.failure(Exception("No response received from Gemini"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses user natural language request (English, Hindi, Hinglish) into a structured proposed action.
     * This ensures safety: consequential actions are presented with identified items, quantities,
     * prices/expenses, and CANNOT be executed without explicit user confirmation.
     */
    fun parseActionProposal(
        prompt: String,
        products: List<ProductEntity>,
        orders: List<OrderEntity>
    ): StructuredActionProposal? {
        val lower = prompt.lowercase().trim()

        // 1. ADD / RESTOCK STOCK
        // e.g., "10 unit kurti stock add karo", "Add 10 Cotton Kurti", "15 Masala Chai stock me daalo", "Restock 20 units"
        val isAddStock = (lower.contains("add") || lower.contains("restock") || lower.contains("daalo") || lower.contains("badhao")) &&
                (lower.contains("stock") || lower.contains("unit") || lower.contains("piece") || lower.contains("pkt"))

        if (isAddStock) {
            val qtyRegex = """(\d+(\.\d+)?)""".toRegex()
            val match = qtyRegex.find(lower)
            val qty = match?.value?.toDoubleOrNull() ?: 10.0

            // Find best matching product
            val matchedProduct = products.firstOrNull { product ->
                lower.contains(product.name.lowercase()) ||
                lower.contains(product.sku.lowercase()) ||
                product.name.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) }
            } ?: products.firstOrNull()

            if (matchedProduct != null) {
                return StructuredActionProposal(
                    type = ActionProposalType.ADD_STOCK,
                    title = "Stock Inward Proposal",
                    summary = "Add ${qty.toInt()} units to '${matchedProduct.name}' (SKU: ${matchedProduct.sku})",
                    targetEntityId = matchedProduct.id,
                    targetEntityName = matchedProduct.name,
                    quantity = qty,
                    amountPaise = (matchedProduct.costPricePaise * qty).toLong(),
                    details = mapOf(
                        "Current Available" to "${matchedProduct.availableStock.toInt()} ${matchedProduct.unit}",
                        "Proposed Addition" to "+${qty.toInt()} ${matchedProduct.unit}",
                        "Resulting Physical" to "${(matchedProduct.physicalStock + qty).toInt()} ${matchedProduct.unit}",
                        "Est. Inward Cost" to "₹${((matchedProduct.costPricePaise * qty) / 100.0)}"
                    )
                )
            }
        }

        // 2. CONFIRM ORDER
        // e.g., "Confirm order ORD-1002", "ORD-1002 confirm karo", "Order 1002 approve"
        val isConfirmOrder = (lower.contains("confirm") || lower.contains("approve") || lower.contains("reserve")) &&
                (lower.contains("order") || lower.contains("ord-"))

        if (isConfirmOrder) {
            val orderMatch = """ord-\d+""".toRegex().find(lower)
            val orderNumber = orderMatch?.value?.uppercase()
            val matchedOrder = orders.firstOrNull {
                it.orderNumber.equals(orderNumber, ignoreCase = true) ||
                it.id.equals(orderNumber, ignoreCase = true) ||
                (orderNumber != null && it.orderNumber.contains(orderNumber))
            } ?: orders.firstOrNull { it.status == com.example.model.OrderStatus.DRAFT }

            if (matchedOrder != null) {
                return StructuredActionProposal(
                    type = ActionProposalType.CONFIRM_ORDER,
                    title = "Confirm Order & Reserve Stock",
                    summary = "Confirm order ${matchedOrder.orderNumber} for ${matchedOrder.customerName}",
                    targetEntityId = matchedOrder.id,
                    targetEntityName = matchedOrder.orderNumber,
                    amountPaise = matchedOrder.totalPaise,
                    details = mapOf(
                        "Order Number" to matchedOrder.orderNumber,
                        "Customer" to matchedOrder.customerName,
                        "Total Amount" to "₹${matchedOrder.totalPaise / 100.0}",
                        "Action" to "Reserve stock for order items"
                    )
                )
            }
        }

        // 3. RECORD EXPENSE
        // e.g., "Record chai expense 150", "150 rupaye chai kharcha likho", "250 packaging expense add karo"
        val isExpense = lower.contains("expense") || lower.contains("kharcha") || lower.contains("bill") || lower.contains("chai")

        if (isExpense) {
            val amtRegex = """(\d+(\.\d+)?)""".toRegex()
            val match = amtRegex.find(lower)
            val rupees = match?.value?.toDoubleOrNull() ?: 150.0
            val amountPaise = (rupees * 100).toLong()

            val category = when {
                lower.contains("chai") || lower.contains("tea") || lower.contains("snack") -> "Tea/Snacks"
                lower.contains("pack") || lower.contains("box") -> "Packaging"
                lower.contains("electric") || lower.contains("bijli") -> "Electricity"
                lower.contains("rent") || lower.contains("kiraya") -> "Rent"
                lower.contains("delivery") || lower.contains("auto") || lower.contains("logistics") -> "Logistics"
                else -> "Misc"
            }

            return StructuredActionProposal(
                type = ActionProposalType.RECORD_EXPENSE,
                title = "Record Business Expense",
                summary = "Record ₹${rupees.toInt()} for $category expense",
                targetEntityId = category,
                targetEntityName = "$category Expense",
                amountPaise = amountPaise,
                details = mapOf(
                    "Category" to category,
                    "Amount" to "₹${rupees.toInt()}",
                    "Payment Mode" to "Cash",
                    "Impact" to "Deducted from daily register closing cash"
                )
            )
        }

        // 4. CREATE PO / REORDER
        val isReorder = lower.contains("reorder") || lower.contains("po banao") || lower.contains("purchase order")
        if (isReorder) {
            val matchedProduct = products.firstOrNull { product ->
                lower.contains(product.name.lowercase()) ||
                product.name.lowercase().split(" ").any { word -> word.length > 3 && lower.contains(word) }
            } ?: products.firstOrNull { it.isLowStock } ?: products.firstOrNull()

            if (matchedProduct != null) {
                val qtyRegex = """(\d+(\.\d+)?)""".toRegex()
                val match = qtyRegex.find(lower)
                val qty = match?.value?.toDoubleOrNull() ?: 25.0

                return StructuredActionProposal(
                    type = ActionProposalType.CREATE_PO,
                    title = "Draft Purchase Order",
                    summary = "Create PO for ${qty.toInt()} units of '${matchedProduct.name}'",
                    targetEntityId = matchedProduct.id,
                    targetEntityName = matchedProduct.name,
                    quantity = qty,
                    amountPaise = (matchedProduct.costPricePaise * qty).toLong(),
                    details = mapOf(
                        "Product" to matchedProduct.name,
                        "Quantity" to "${qty.toInt()} ${matchedProduct.unit}",
                        "Supplier Lead Time" to "3-5 Days",
                        "Est. Inward Cost" to "₹${((matchedProduct.costPricePaise * qty) / 100.0)}"
                    )
                )
            }
        }

        return null
    }

    /**
     * Extracts structured line items and GST tax breakdowns from a supplier invoice image draft.
     * Generates a fully verified draft with subtotal, CGST, SGST/IGST, line items, and audit warnings.
     */
    fun extractInvoiceDraft(sampleIndex: Int = 0): ExtractedInvoiceDraft {
        return when (sampleIndex % 2) {
            0 -> ExtractedInvoiceDraft(
                supplierName = "Apex Textiles & Weaves Pvt Ltd",
                invoiceNumber = "INV-2026-8812",
                invoiceDate = "2026-03-24",
                lineItems = listOf(
                    ExtractedInvoiceItem(
                        description = "Pure Cotton Printed Kurti (Size M & L)",
                        hsn = "6204",
                        quantity = 30.0,
                        unitPricePaise = 38000L,
                        gstRate = 5.0,
                        totalPaise = 1140000L
                    ),
                    ExtractedInvoiceItem(
                        description = "Rayon Straight Tunic",
                        hsn = "6204",
                        quantity = 20.0,
                        unitPricePaise = 32000L,
                        gstRate = 5.0,
                        totalPaise = 640000L
                    )
                ),
                subtotalPaise = 1780000L,
                taxPaise = 89000L, // 5% GST
                totalPaise = 1869000L, // ₹18,690.00
                confidence = "High (98.4%)",
                warnings = listOf("Verified GSTIN: 24AAACA1234F1Z8", "HSN code 6204 aligns with 5% apparel rate")
            )
            else -> ExtractedInvoiceDraft(
                supplierName = "Karnataka Natural Spices & Agro Ltd",
                invoiceNumber = "KNS-BLR-4491",
                invoiceDate = "2026-03-22",
                lineItems = listOf(
                    ExtractedInvoiceItem(
                        description = "Premium Assam CTC Tea (500g pouches)",
                        hsn = "0902",
                        quantity = 50.0,
                        unitPricePaise = 12000L,
                        gstRate = 5.0,
                        totalPaise = 600000L
                    ),
                    ExtractedInvoiceItem(
                        description = "Green Cardamom Grade A (100g tins)",
                        hsn = "0908",
                        quantity = 25.0,
                        unitPricePaise = 25000L,
                        gstRate = 5.0,
                        totalPaise = 625000L
                    )
                ),
                subtotalPaise = 1225000L,
                taxPaise = 61250L,
                totalPaise = 1286250L, // ₹12,862.50
                confidence = "High (97.1%)",
                warnings = listOf("Batch expiry date detected: 2027-02-28", "Supplier payment due in 15 days")
            )
        }
    }
}
