package com.example.ai

import com.example.model.ActionProposalType
import com.example.model.OrderEntity
import com.example.model.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class GeminiActionProposalSafetyTest {
    private val service = GeminiService()
    private val products = listOf(
        ProductEntity(
            id = "P-001",
            name = "Pure Cotton Anarkali Kurti",
            sku = "APP-KT-001",
            category = "Apparel",
            costPricePaise = 45000L,
            sellingPricePaise = 89900L,
            physicalStock = 35.0
        ),
        ProductEntity(
            id = "P-002",
            name = "Cotton Kurti Blue",
            sku = "APP-KT-002",
            category = "Apparel",
            costPricePaise = 40000L,
            sellingPricePaise = 79900L,
            physicalStock = 12.0
        )
    )

    @Test
    fun doesNotGuessQuantityForStockAction() {
        assertNull(
            service.parseActionProposal(
                "add stock for Pure Cotton Anarkali Kurti",
                products,
                emptyList<OrderEntity>()
            )
        )
    }

    @Test
    fun doesNotChooseFirstProductWhenNameIsAmbiguous() {
        assertNull(
            service.parseActionProposal(
                "add 10 units of cotton kurti to stock",
                products,
                emptyList<OrderEntity>()
            )
        )
    }

    @Test
    fun acceptsExplicitQuantityAndUniqueProduct() {
        val proposal = service.parseActionProposal(
            "add 10 units of Pure Cotton Anarkali Kurti to stock",
            products,
            emptyList<OrderEntity>()
        )
        assertNotNull(proposal)
        assertEquals(ActionProposalType.ADD_STOCK, proposal?.type)
        assertEquals(10.0, proposal?.quantity ?: 0.0, 0.0)
    }

    @Test
    fun doesNotInventExpenseAmount() {
        assertNull(
            service.parseActionProposal(
                "record chai expense",
                products,
                emptyList<OrderEntity>()
            )
        )
    }

    @Test
    fun doesNotGuessWhichOrderToConfirm() {
        assertNull(
            service.parseActionProposal(
                "confirm the order",
                products,
                emptyList<OrderEntity>()
            )
        )
    }

    @Test
    fun doesNotInventReorderQuantity() {
        assertNull(
            service.parseActionProposal(
                "reorder Pure Cotton Anarkali Kurti",
                products,
                emptyList<OrderEntity>()
            )
        )
    }
}
