package com.greninjaop.mailorganizer.ui.import

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greninjaop.mailorganizer.R
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.mail.SenderAvatar
import kotlinx.coroutines.delay
import kotlin.math.atan2

/**
 * Import flow (2026-10-17): frosted transfer host.
 *
 * The card designs, the card-to-card morph and the conduit animation are
 * copied exactly from the Gemini reference the user supplied (Connect Gmail
 * card screenshot + transfer-card code). What is NOT copied:
 *
 * - The fake progress loop (progress += 0.0036 every 16ms) — progress comes
 *   from the real sync ([ImportViewModel]).
 * - The six sample mails — the transpiring rows show the mail that just
 *   arrived ([ImportViewModel.ImportUiState.Importing.recentMails]).
 * - The hardcoded "c47011882@gmail.com" — the real connected account email.
 * - Any phone-chrome code (status bar / navigation buttons) — the Gemini
 *   app rendered its own phone frame; this app is edge-to-edge, so the host
 *   is just the app background + frosted scrim + card.
 * - A blurred inbox behind the overlay — the import route is a full-screen
 *   nav destination, so there is no composed inbox behind it to blur. The
 *   visual result is the same dark backdrop + card as the reference.
 *
 * The card colors are the reference's hardcoded dark palette (not the
 * theme), because the reference is a dark design and the user asked for a
 * picture-perfect copy.
 *
 * Behaviour preserved from the old flow: "Back" on the Connect card is the
 * old "Not now" (background sync + go to the inbox); Continue is enabled
 * only when the sync actually completes ([ImportViewModel.ImportUiState.Done]);
 * on error the card keeps Try again / Continue anyway.
 */
@Composable
fun ImportFlowScreen(
    viewModel: ImportViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    when (val s = state) {
        is ImportViewModel.ImportUiState.Loading -> {
            Surface(
                modifier = modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        else -> {
            ImportFrostedTransferHost(
                state = s,
                onImportMails = viewModel::startImport,
                onBack = {
                    // Old "Not now": skip the animated import but still sync
                    // in the background, so the inbox populates.
                    viewModel.skipImport()
                    onDone()
                },
                onContinue = onDone,
                onRetry = viewModel::startImport,
                modifier = modifier,
            )
        }
    }
}

// Material 3 dark palette from the reference (matches the screenshot).
private val M3AppBackground = Color(0xFF101418)
private val M3FrostedScrim = Color(0xB30E1115)
private val M3CardSurface = Color(0xFF1C1F24)
private val M3OnSurface = Color(0xFFE2E2E6)
private val M3OnSurfaceBody = Color(0xFFC4C6D0)
private val M3OnSurfaceMuted = Color(0xFF8E9099)
private val M3ConnectedBlue = Color(0xFF8AB4F8)
private val M3PrimaryPillBlue = Color(0xFF9CCAFF)
private val M3OnPrimaryPillNavy = Color(0xFF003258)
private val M3OutlineStroke = Color(0xFF3C4043)
private val M3TimestampBlue = Color(0xFF7FCFFF)

private enum class TransferStep {
    CONNECT_GMAIL, // Card 1: "Import mails" + Back
    TRANSFERRING_MAILS, // Card 2: conduit + transpiring mails + Continue
}

/**
 * Frosted host: dark backdrop + scrim + one centred card, morphing between
 * the Connect card and the transfer card. Tapping Continue fades the
 * overlay out (the reference's COMPLETED exit) and only then navigates.
 */
@Composable
private fun ImportFrostedTransferHost(
    state: ImportViewModel.ImportUiState,
    onImportMails: () -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var finishing by remember { mutableStateOf(false) }
    val step = when (state) {
        is ImportViewModel.ImportUiState.Intro -> TransferStep.CONNECT_GMAIL
        else -> TransferStep.TRANSFERRING_MAILS
    }
    val isOverlayVisible = !finishing
    LaunchedEffect(finishing) {
        if (finishing) {
            delay(340)
            onContinue()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(M3AppBackground)) {
        AnimatedVisibility(
            visible = isOverlayVisible,
            enter = fadeIn(tween(260)),
            exit = fadeOut(tween(320)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(M3FrostedScrim)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = M3CardSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 24.dp,
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(380, delayMillis = 60, easing = FastOutSlowInEasing)) +
                                slideInVertically(
                                    animationSpec = tween(420, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 9 },
                                ) +
                                scaleIn(
                                    initialScale = 0.97f,
                                    animationSpec = tween(420, easing = FastOutSlowInEasing),
                                )) togetherWith
                                (fadeOut(tween(200, easing = FastOutLinearInEasing)) +
                                    slideOutVertically(
                                        animationSpec = tween(220, easing = FastOutLinearInEasing),
                                        targetOffsetY = { -it / 12 },
                                    ) +
                                    scaleOut(targetScale = 0.98f, animationSpec = tween(200)))
                        },
                        label = "m3CardLaunchMorph",
                    ) { currentStep ->
                        when (currentStep) {
                            TransferStep.CONNECT_GMAIL -> {
                                val account =
                                    (state as ImportViewModel.ImportUiState.Intro).account
                                ConnectGmailCardContent(
                                    connectedEmail = account.emailAddress,
                                    onImportMails = onImportMails,
                                    onBack = onBack,
                                )
                            }
                            TransferStep.TRANSFERRING_MAILS -> {
                                UnifiedTransferSingleCardContent(
                                    state = state,
                                    onContinueWhenComplete = { finishing = true },
                                    onRetry = onRetry,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Connect Gmail card featuring the "Import mails" button + Material 3 Import icon.
 * Exact copy of the reference (texts, sizes, colors, spacing).
 */
@Composable
private fun ConnectGmailCardContent(
    connectedEmail: String,
    onImportMails: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connect Gmail",
            color = M3OnSurface,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Mailstack needs permission to read your Gmail so it can organize your mailbox on-device. Your emails never leave your phone except to fetch them from Google.",
            color = M3OnSurfaceBody,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Permission requested: read your Gmail messages (gmail.readonly). Mailstack cannot send, delete, or modify your emails. You can disconnect anytime from Settings → Accounts.",
            color = M3OnSurfaceMuted,
            fontSize = 12.sp,
            lineHeight = 17.5.sp,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Connected as $connectedEmail",
            color = M3ConnectedBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        // Primary Pill Button with Import Logo + "Import mails"
        Button(
            onClick = onImportMails,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = M3PrimaryPillBlue,
                contentColor = M3OnPrimaryPillNavy,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ImportMailGlyphIcon(tint = M3OnPrimaryPillNavy)
                Spacer(Modifier.width(8.dp))
                Text("Import mails", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, M3OutlineStroke),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = M3OnSurface),
        ) {
            Text("Back", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Vector Canvas icon for the "Import mails" button (Arrow into inbox tray).
 * Exact copy of the reference.
 */
@Composable
private fun ImportMailGlyphIcon(tint: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val stroke = 2.1.dp.toPx()
        val w = size.width
        val h = size.height
        // Vertical shaft
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.14f),
            end = Offset(w * 0.5f, h * 0.64f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        // Arrowhead
        val arrowPath = Path().apply {
            moveTo(w * 0.29f, h * 0.44f)
            lineTo(w * 0.5f, h * 0.65f)
            lineTo(w * 0.71f, h * 0.44f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Bottom tray
        val trayPath = Path().apply {
            moveTo(w * 0.17f, h * 0.64f)
            lineTo(w * 0.17f, h * 0.78f)
            quadraticTo(w * 0.17f, h * 0.86f, w * 0.25f, h * 0.86f)
            lineTo(w * 0.75f, h * 0.86f)
            quadraticTo(w * 0.83f, h * 0.86f, w * 0.83f, h * 0.78f)
            lineTo(w * 0.83f, h * 0.64f)
        }
        drawPath(
            path = trayPath,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * Single unified card (reference):
 * - Top: header + fluid conduit & envelope transfer animation
 * - Bottom (inside the SAME card): live transpiring mail rows + Continue button
 *   (Continue is dimmed blue & disabled until the sync completes, then full #9CCAFF).
 *
 * On error the conduit is replaced by the error message and the button row
 * becomes Try again / Continue anyway — the reference has no error state,
 * but the flow must never dead-end on a failed import.
 */
@Composable
private fun UnifiedTransferSingleCardContent(
    state: ImportViewModel.ImportUiState,
    onContinueWhenComplete: () -> Unit,
    onRetry: () -> Unit,
) {
    val isComplete = state is ImportViewModel.ImportUiState.Done
    val importing = state as? ImportViewModel.ImportUiState.Importing
    val error = state as? ImportViewModel.ImportUiState.Error
    val account = when (state) {
        is ImportViewModel.ImportUiState.Importing -> state.account
        is ImportViewModel.ImportUiState.Done -> state.account
        is ImportViewModel.ImportUiState.Error -> state.account
        else -> null
    }
    val progress: Float? = when (state) {
        is ImportViewModel.ImportUiState.Importing -> state.progress
        is ImportViewModel.ImportUiState.Done -> 1f
        else -> null
    }
    val percentInt = ((progress ?: 0f) * 100).toInt()
    val recentMails: List<MessageRecord> = when (state) {
        is ImportViewModel.ImportUiState.Importing -> state.recentMails
        is ImportViewModel.ImportUiState.Done -> state.recentMails
        else -> emptyList()
    }

    // Smoothly animate the Continue button from slightly dimmed blue (40% alpha) to 100% #9CCAFF
    val buttonContainerColor by animateColorAsState(
        targetValue = if (isComplete) M3PrimaryPillBlue else M3PrimaryPillBlue.copy(alpha = 0.40f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "continueBtnBg",
    )
    val buttonContentColor by animateColorAsState(
        targetValue = if (isComplete) M3OnPrimaryPillNavy else M3OnPrimaryPillNavy.copy(alpha = 0.72f),
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "continueBtnFg",
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Top section of the single card: header + conduit (or error message)
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = when {
                            error != null -> "Import didn't finish"
                            isComplete -> "Mailbox Organized"
                            else -> "Importing Mails"
                        },
                        color = M3OnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = account?.emailAddress.orEmpty(),
                        color = M3ConnectedBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (error == null) {
                    if (progress != null) {
                        Text(
                            text = "$percentInt%",
                            color = M3PrimaryPillBlue,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        // No estimate yet (still connecting): show the honest
                        // stage text instead of a fabricated percentage.
                        Text(
                            text = importing?.stageText.orEmpty(),
                            color = M3PrimaryPillBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = error.message,
                    color = M3OnSurfaceBody,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                )
            } else {
                GmailToMailstackFluidConduitCanvas(
                    progress = progress,
                    processed = importing?.processed ?: 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                )
            }
        }

        // Hairline divider inside the single card
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 22.dp),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.06f),
        )

        // 2. Bottom section of the SAME single card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            if (error != null) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = M3PrimaryPillBlue,
                        contentColor = M3OnPrimaryPillNavy,
                    ),
                ) {
                    Text("Try again", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onContinueWhenComplete,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, M3OutlineStroke),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = M3OnSurface),
                ) {
                    Text("Continue anyway", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Transpiring mails: the two most recently arrived real mails.
                // Empty until the first batch lands — the card grows via
                // animateContentSize when they arrive.
                val primaryMail = recentMails.getOrNull(0)
                val secondaryMail = recentMails.getOrNull(1)
                if (primaryMail != null) {
                    TranspiringMailRowView(item = primaryMail, alpha = 1f)
                }
                if (secondaryMail != null) {
                    Spacer(Modifier.height(10.dp))
                    TranspiringMailRowView(item = secondaryMail, alpha = 0.55f)
                }
                if (primaryMail != null) {
                    Spacer(Modifier.height(18.dp))
                }

                // Continue button: unclickable & slightly dimmed blue until
                // the sync completes, then normal #9CCAFF & clickable.
                Button(
                    onClick = onContinueWhenComplete,
                    enabled = isComplete,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonContainerColor,
                        contentColor = buttonContentColor,
                        disabledContainerColor = buttonContainerColor,
                        disabledContentColor = buttonContentColor,
                    ),
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * One transpiring mail row: sender avatar + sender/timestamp line, subject,
 * snippet. Same structure as the reference's sample rows, but the data is a
 * real [MessageRecord] — avatar via the app's [SenderAvatar], timestamp via
 * [MailFormatting.relativeTime]. The blue timestamp is the reference's accent.
 */
@Composable
private fun TranspiringMailRowView(item: MessageRecord, alpha: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SenderAvatar(
            key = item.fromAddress,
            displayName = item.fromName,
            emailAddress = item.fromAddress,
            size = 40.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = MailFormatting.senderDisplay(item.fromName, item.fromAddress),
                    color = M3OnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = MailFormatting.relativeTime(
                        item.timestampEpochMs,
                        System.currentTimeMillis(),
                    ),
                    color = M3TimestampBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = MailFormatting.subjectDisplay(item.subject),
                color = M3OnSurface,
                fontSize = 13.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.snippet.orEmpty(),
                color = M3OnSurfaceMuted,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    // The reference passes alpha per row (primary 1f, secondary 0.55f);
    // applied to the row via graphicsLayer above.
}

/**
 * Fluid conduit between the Gmail logo (left) and the Mailstack logo (right).
 *
 * The reference calls this composable but its body was not in the supplied
 * code, so it is built to the reference's description: a fluid tube with
 * envelopes travelling Gmail → Mailstack. The tube fills with blue up to
 * the real [progress]; a dashed stream crawls along it for the fluid feel;
 * envelopes spawn from REAL progress (one per ~8 imported messages, the
 * same honesty rule as the old flying-mail animation) — never on a timer.
 *
 * Logos: the Gmail "M" ([R.drawable.ic_gmail_logo], vector traced from the
 * official icon and pixel-verified) in a white circle on the left, the
 * app's own round launcher icon on the right. Both 56dp circles, vertically
 * centred, exactly at the conduit ends.
 */
@Composable
private fun GmailToMailstackFluidConduitCanvas(
    progress: Float?,
    processed: Int,
    modifier: Modifier = Modifier,
) {
    val envelopes = remember { mutableStateListOf<ConduitEnvelope>() }
    var lastProcessed by remember { mutableStateOf(0) }
    var spawnSeed by remember { mutableStateOf(0L) }

    // Spawn envelopes from real progress — never on a timer.
    LaunchedEffect(processed) {
        val delta = (processed - lastProcessed).coerceAtLeast(0)
        lastProcessed = processed
        if (delta > 0 || envelopes.isEmpty()) {
            val count = if (delta > 0) (delta / ENVELOPE_PER_MESSAGES).coerceIn(1, 6) else 2
            repeat(count) { i ->
                spawnSeed++
                envelopes.add(
                    ConduitEnvelope(
                        id = spawnSeed,
                        birthNanos = System.nanoTime() + i * 120_000_000L,
                    ),
                )
            }
        }
    }

    // Frame clock: drives envelope motion + the crawling flow dashes.
    var frameNanos by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        withInfiniteAnimationFrameNanos { frameNanos = it }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawConduitScene(
                nowNanos = frameNanos,
                progress = progress,
                envelopes = envelopes,
            )
        }
        // Gmail logo, left — white circle, M cropped with even padding.
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = CONDUIT_LOGO_INSET)
                .size(CONDUIT_LOGO_SIZE)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_gmail_logo),
                contentDescription = "Gmail logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp, 30.dp),
            )
        }
        // Mailstack logo, right — the app's own round launcher icon.
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_round),
            contentDescription = "Mailstack logo",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = CONDUIT_LOGO_INSET)
                .size(CONDUIT_LOGO_SIZE)
                .clip(CircleShape),
        )
    }
}

private data class ConduitEnvelope(
    val id: Long,
    val birthNanos: Long,
    val durationMs: Long = 1500L,
)

private const val ENVELOPE_PER_MESSAGES = 8

/** Conduit geometry: logo circles sit exactly at the tube ends. */
private val CONDUIT_LOGO_SIZE = 56.dp
private val CONDUIT_LOGO_INSET = 4.dp
private val CONDUIT_TUBE_WIDTH = 12.dp
private val CONDUIT_WAVE = 16.dp

private fun DrawScope.drawConduitScene(
    nowNanos: Long,
    progress: Float?,
    envelopes: MutableList<ConduitEnvelope>,
) {
    val w = size.width
    val cy = size.height / 2f
    val startX = (CONDUIT_LOGO_INSET + CONDUIT_LOGO_SIZE).toPx()
    val endX = w - (CONDUIT_LOGO_INSET + CONDUIT_LOGO_SIZE).toPx()
    val span = (endX - startX).coerceAtLeast(0f)
    val wave = CONDUIT_WAVE.toPx()

    // Gentle S-wave conduit: cubic bezier, control points offset in
    // opposite directions so the tube feels fluid, not rigid.
    val p0 = Offset(startX, cy)
    val p1 = Offset(startX + span * 0.25f, cy - wave)
    val p2 = Offset(startX + span * 0.75f, cy + wave)
    val p3 = Offset(endX, cy)
    val path = Path().apply {
        moveTo(p0.x, p0.y)
        cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    }
    val tubeWidth = CONDUIT_TUBE_WIDTH.toPx()

    // Track: faint tube.
    drawPath(
        path,
        color = Color.White,
        alpha = 0.10f,
        style = Stroke(width = tubeWidth, cap = StrokeCap.Round),
    )
    // Fill: blue up to the real progress, via a dash whose length is the
    // filled fraction of the sampled path length.
    val totalLength = cubicBezierLength(p0, p1, p2, p3)
    val fillLength = ((progress ?: 0f).coerceIn(0f, 1f) * totalLength)
    if (fillLength > 0f) {
        drawPath(
            path,
            color = M3PrimaryPillBlue,
            alpha = 0.55f,
            style = Stroke(
                width = tubeWidth,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(fillLength, (totalLength - fillLength).coerceAtLeast(1f)),
                    0f,
                ),
            ),
        )
    }
    // Stream: thin dashed line crawling along the tube (fluid motion).
    // Dash period 20dp; the phase loops seamlessly.
    val dashPhase = -((nowNanos / 1_000_000L / 30L) % 20) * 1.dp.toPx()
    drawPath(
        path,
        color = Color.White,
        alpha = 0.45f,
        style = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(8.dp.toPx(), 12.dp.toPx()),
                dashPhase,
            ),
        ),
    )

    // Envelopes in flight along the same bezier.
    val alive = envelopes.filter { env ->
        (nowNanos - env.birthNanos) < env.durationMs * 1_000_000L
    }
    if (alive.size != envelopes.size) {
        envelopes.removeAll { it !in alive }
    }
    for (env in alive) {
        val t = ((nowNanos - env.birthNanos).toFloat() / (env.durationMs * 1_000_000L))
            .coerceIn(0f, 1f)
        val pos = cubicBezierPoint(p0, p1, p2, p3, t)
        val tangent = cubicBezierTangent(p0, p1, p2, p3, t)
        val angleDeg = Math.toDegrees(atan2(tangent.y, tangent.x).toDouble()).toFloat()
        val alpha = when {
            t < 0.08f -> t / 0.08f
            t > 0.85f -> (1f - t) / 0.15f
            else -> 1f
        }
        drawConduitEnvelope(
            center = pos,
            angleDeg = angleDeg,
            alpha = alpha,
            body = Color.White,
            flap = M3PrimaryPillBlue,
        )
    }
}

private fun cubicBezierPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x,
        u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y,
    )
}

private fun cubicBezierTangent(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        3f * u * u * (p1.x - p0.x) + 6f * u * t * (p2.x - p1.x) + 3f * t * t * (p3.x - p2.x),
        3f * u * u * (p1.y - p0.y) + 6f * u * t * (p2.y - p1.y) + 3f * t * t * (p3.y - p2.y),
    )
}

/** Path length by sampling (Compose has no PathMeasure). 64 samples is plenty. */
private fun cubicBezierLength(p0: Offset, p1: Offset, p2: Offset, p3: Offset): Float {
    var length = 0f
    var prev = p0
    for (i in 1..64) {
        val pt = cubicBezierPoint(p0, p1, p2, p3, i / 64f)
        length += (pt - prev).getDistance()
        prev = pt
    }
    return length
}

/** Sealed envelope glyph: white rounded body + blue flap chevron. */
private fun DrawScope.drawConduitEnvelope(
    center: Offset,
    angleDeg: Float,
    alpha: Float,
    body: Color,
    flap: Color,
) {
    val w = 40.dp.toPx()
    val h = 28.dp.toPx()
    rotate(angleDeg, center) {
        drawRoundRect(
            color = body,
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(6.dp.toPx()),
            alpha = alpha,
        )
        val flapPath = Path().apply {
            moveTo(center.x - w / 2f + 3.dp.toPx(), center.y - h / 2f + 4.dp.toPx())
            lineTo(center.x, center.y + 2.dp.toPx())
            lineTo(center.x + w / 2f - 3.dp.toPx(), center.y - h / 2f + 4.dp.toPx())
        }
        drawPath(
            flapPath,
            color = flap,
            style = Stroke(width = 2.5.dp.toPx()),
            alpha = alpha,
        )
    }
}
