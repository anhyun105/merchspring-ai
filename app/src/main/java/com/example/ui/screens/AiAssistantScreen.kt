package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val enableThinking by viewModel.enableThinkingMode.collectAsState()
    val enableSearch by viewModel.enableSearchGrounding.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Controls Header: Thinking Mode & Search Grounding Toggles
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thinking Mode Toggle (gemini-4-argon with High Thinking)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = enableThinking,
                        onClick = { viewModel.enableThinkingMode.value = !enableThinking },
                        label = {
                            Text(
                                if (enableThinking) "High Thinking ON" else "High Thinking",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (enableThinking) BrandTeal else LightSlate
                            )
                        }
                    )
                }

                // Google Search Grounding Toggle
                FilterChip(
                    selected = enableSearch,
                    onClick = { viewModel.enableSearchGrounding.value = !enableSearch },
                    label = {
                        Text(
                            if (enableSearch) "Search ON" else "Google Search",
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.TravelExplore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (enableSearch) BrandTeal else LightSlate
                        )
                    }
                )
            }
        }

        // Chat Message Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    msg = msg,
                    onExecuteProposal = { viewModel.executeActionProposal(it) }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = BrandTeal,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (enableThinking) "Using Gemini high-reasoning mode..." else "Analyzing business database...",
                            fontSize = 12.sp,
                            color = BrandTeal
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips (English, Hindi, and Hinglish)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "10 unit kurti stock add karo",
                "Aaj ki bikri aur cash summary",
                "Dead stock list dikhao",
                "Record chai expense 150",
                "Masala Chai ka reorder PO banao",
                "Draft polite payment reminder in Hinglish",
                "Summarize today's sales & profits",
                "Which products are below reorder threshold?"
            )
            items(suggestions) { prompt ->
                Surface(
                    onClick = {
                        viewModel.sendAiPrompt(prompt)
                    },
                    color = SurfaceDark,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark)
                ) {
                    Text(
                        text = prompt,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                        color = BrandTealLight
                    )
                }
            }
        }

        // Message Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask in English, Hindi, or Hinglish...", fontSize = 13.sp) },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(20.dp)
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isThinking) {
                        val text = inputText
                        inputText = ""
                        viewModel.sendAiPrompt(text)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(BrandTeal, RoundedCornerShape(24.dp)),
                enabled = inputText.isNotBlank() && !isThinking
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MidnightNavy,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    msg: ChatMessage,
    onExecuteProposal: (com.example.model.StructuredActionProposal) -> Unit = {}
) {
    val isUser = msg.role == "user"
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceDark, RoundedCornerShape(16.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI",
                    tint = BrandTeal,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(modifier = Modifier.widthIn(max = 330.dp)) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) BrandTealContainer else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isUser) BrandTeal.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
                ),
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "MerchSpring AI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                            if (msg.isThinking) {
                                Text("• High Thinking", fontSize = 9.sp, color = LightSlate)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = msg.content,
                        fontSize = 13.sp,
                        color = if (isUser) BrandTealLight else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    // Google Search grounding requires both the Search entry-point panel
                    // and visible source attribution. JavaScript and file access remain disabled.
                    msg.searchEntryHtml?.takeIf { !isUser && it.isNotBlank() }?.let { html ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.javaScriptEnabled = false
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    isVerticalScrollBarEnabled = false
                                    isHorizontalScrollBarEnabled = false
                                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                    loadDataWithBaseURL("https://www.google.com/", html, "text/html", "UTF-8", null)
                                    tag = html
                                }
                            },
                            update = { webView ->
                                if (webView.tag != html) {
                                    webView.loadDataWithBaseURL(
                                        "https://www.google.com/",
                                        html,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                    webView.tag = html
                                }
                            }
                        )
                    }

                    if (!isUser && msg.groundingSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sources",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        msg.groundingSources.forEach { source ->
                            Text(
                                text = source.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        runCatching { uriHandler.openUri(source.uri) }
                                    }
                                    .padding(vertical = 3.dp),
                                fontSize = 11.sp,
                                color = BrandTeal
                            )
                        }
                    }
                }
            }

            // If this message contains a structured business action proposal, render confirmation card
            msg.proposal?.let { proposal ->
                var isExecuted by remember { mutableStateOf(false) }

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandTeal.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = proposal.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandTeal
                            )
                            Surface(
                                color = if (isExecuted) Color(0xFF1B5E20) else StatusAmberContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isExecuted) "APPLIED" else "NEEDS CONFIRMATION",
                                    color = if (isExecuted) Color.White else StatusAmber,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = proposal.summary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Details breakdown
                        if (proposal.details.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                proposal.details.forEach { (k, v) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(k, fontSize = 11.sp, color = LightSlate)
                                        Text(v, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (!isExecuted) {
                            Text(
                                text = "⚠️ Consequential Action Safety: MerchSpring never alters business records automatically. Review and tap confirm to execute.",
                                fontSize = 10.sp,
                                color = LightSlate
                            )

                            Button(
                                onClick = {
                                    onExecuteProposal(proposal)
                                    isExecuted = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MidnightNavy, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm & Execute", color = MidnightNavy, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Text("Successfully applied to live business database.", fontSize = 11.sp, color = Color(0xFF4CAF50))
                            }
                        }
                    }
                }
            }
        }
    }
}

