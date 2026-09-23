package com.gymcompanion.app.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.screens.nutrition.nothingTextFieldColors
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.AiViewModel
import com.gymcompanion.app.viewmodel.ChatMessage

private val PAD = 24.dp

@Composable
fun AiScreen(
    viewModel: AiViewModel = hiltViewModel(),
    initialMessage: String = ""
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf(initialMessage) }

    // Update input when navigated to with a pre-filled message
    LaunchedEffect(initialMessage) {
        if (initialMessage.isNotBlank()) input = initialMessage
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {
        Column(Modifier.fillMaxSize().imePadding()) {

            // ── Header ──────────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    NLabel("INTELLIGENCE", color = NothingYellow.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(NothingYellow, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("Coach IA", fontFamily = LocalNumericFont.current,
                            fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                            letterSpacing = 0.5.sp, color = NothingWhite)
                    }
                }
                // Settings (API key) toggle
                IconButton(
                    onClick = viewModel::toggleKeySetup,
                    modifier = Modifier.size(36.dp)
                        .border(1.dp, NothingBorderMid, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Clé API",
                        tint = NothingGrey1, modifier = Modifier.size(18.dp))
                }
            }

            // ── API key setup panel ──────────────────────────────────────────────
            AnimatedVisibility(visible = state.showKeySetup) {
                ApiKeySetup(
                    currentKey = state.apiKey,
                    onSave = { viewModel.saveApiKey(it) },
                    modifier = Modifier.padding(horizontal = PAD)
                )
            }

            // ── Empty state ──────────────────────────────────────────────────────
            if (state.messages.isEmpty() && !state.showKeySetup) {
                EmptyAiState(modifier = Modifier.weight(1f))
            } else {
                // ── Chat messages ────────────────────────────────────────────────
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = PAD, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.hashCode() }) { msg ->
                        MessageBubble(message = msg)
                    }
                    if (state.isLoading) {
                        item(key = "loading") { LoadingBubble() }
                    }
                }
            }

            // ── Error bar ────────────────────────────────────────────────────────
            state.error?.let { err ->
                Row(
                    Modifier.fillMaxWidth().background(NothingRed.copy(alpha = 0.1f))
                        .padding(horizontal = PAD, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NLabel(err, color = NothingRed, modifier = Modifier.weight(1f), size = 8.sp)
                    IconButton(onClick = viewModel::clearError, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = NothingRed, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // ── Input bar ────────────────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(NothingDeep)
                    .border(BorderStroke(1.dp, NothingBorder), shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = {
                            Text("Pose une question à ton coach…",
                                color = NothingGrey3, fontSize = 13.sp)
                        },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        ),
                        colors = nothingTextFieldColors(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 4
                    )
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                viewModel.sendMessage(input)
                                input = ""
                            }
                        },
                        enabled = input.isNotBlank() && !state.isLoading,
                        modifier = Modifier.size(44.dp)
                            .background(
                                if (input.isNotBlank()) NothingYellow else NothingGrey3,
                                RoundedCornerShape(10.dp)
                            )
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Envoyer",
                            tint = NothingBlack, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI avatar dot
            Box(
                Modifier.size(28.dp)
                    .background(NothingYellow.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, NothingYellow.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null,
                    tint = NothingYellow, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(10.dp))
        }

        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isUser) 14.dp else 4.dp,
                        topEnd   = if (isUser) 4.dp  else 14.dp,
                        bottomStart = 14.dp,
                        bottomEnd   = 14.dp
                    )
                )
                .background(if (isUser) NothingDark2 else NothingDeep)
                .border(
                    1.dp,
                    if (isUser) NothingBorderMid else NothingYellow.copy(alpha = 0.25f),
                    RoundedCornerShape(
                        topStart = if (isUser) 14.dp else 4.dp,
                        topEnd   = if (isUser) 4.dp  else 14.dp,
                        bottomStart = 14.dp,
                        bottomEnd   = 14.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = NothingWhite,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp)
                .background(NothingYellow.copy(alpha = 0.15f), CircleShape)
                .border(1.dp, NothingYellow.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null,
                tint = NothingYellow, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(NothingDeep)
                .border(1.dp, NothingYellow.copy(alpha = 0.25f),
                    RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { i ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot$i")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween<Float>(600, delayMillis = i * 150, easing = EaseInOut),
                            RepeatMode.Reverse
                        ),
                        label = "alpha$i"
                    )
                    Box(
                        Modifier.size(6.dp)
                            .background(NothingYellow.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAiState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = PAD),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(64.dp)
                .background(NothingYellow.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, NothingYellow.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, null,
                tint = NothingYellow, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Coach IA", color = NothingWhite, fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold, fontFamily = LocalNumericFont.current)
        Spacer(Modifier.height(10.dp))
        NLabel("ANALYSE · CONSEILS · PROGRAMMES", color = NothingGrey1)
        Spacer(Modifier.height(28.dp))

        // Suggested prompts
        val prompts = listOf(
            "Analyse ma progression ce mois-ci",
            "Quels muscles dois-je travailler cette semaine ?",
            "Donne-moi des conseils nutritionnels",
            "Optimise mon objectif calorique"
        )
        prompts.forEach { p ->
            NLabel(
                text = "· $p",
                color = NothingGrey2,
                size = 9.sp,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        NLabel("CONFIGURER UNE CLÉ API OPENROUTER CI-DESSOUS", color = NothingGrey3, size = 8.sp)
    }
}

@Composable
private fun ApiKeySetup(
    currentKey: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var key by remember(currentKey) { mutableStateOf(currentKey) }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, NothingYellow.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .background(NothingYellow.copy(alpha = 0.04f))
            .padding(14.dp)
    ) {
        NLabel("CLÉ API OPENROUTER", color = NothingYellow.copy(alpha = 0.8f), size = 8.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                placeholder = { Text("sk-or-v1-…", color = NothingGrey3, fontSize = 12.sp) },
                modifier = Modifier.weight(1f),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }, modifier = Modifier.size(20.dp)) {
                        Icon(
                            if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            null, tint = NothingGrey2, modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = nothingTextFieldColors(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onSave(key) },
                colors = ButtonDefaults.buttonColors(containerColor = NothingYellow),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("OK", color = NothingBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        NLabel("Obtenez une clé gratuite sur openrouter.ai · Les données restent privées",
            color = NothingGrey3, size = 7.sp)
    }
    Spacer(Modifier.height(12.dp))
}
