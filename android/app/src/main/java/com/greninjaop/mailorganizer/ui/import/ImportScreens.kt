package com.greninjaop.mailorganizer.ui.import

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Brush
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
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.MessageRecord
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
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
    // System Back mirrors the on-screen Back (2026-10-17 fix): it
    // skips the animated import and goes to the inbox — it must never
    // pop the back stack (which, with a lone import entry, finishes
    // the Activity and reads as "Back relaunches the app").
    BackHandler {
        viewModel.skipImport()
        onDone()
    }
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
private val M3AvatarBlue = Color(0xFF1B5E8A)
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
                    TranspiringMailRowView(item = primaryMail, account = account, alpha = 1f)
                }
                if (secondaryMail != null) {
                    Spacer(Modifier.height(16.dp))
                    TranspiringMailRowView(item = secondaryMail, account = account, alpha = 0.55f)
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
 * One transpiring mail row, to the reference screenshots (the supplied code
 * does not contain this composable's body): a small ACCOUNT avatar (the
 * account the mail is being imported into, 32dp) + the sender avatar (48dp)
 * + the sender/timestamp line, subject, snippet.
 *
 * The sender avatar uses a deterministic brand-style (bg, fg) pair picked
 * from the sender address. The screenshots' per-sender colours come from
 * Gemini's hardcoded sample data (Google blue, Adobe white/red, Swiggy
 * orange, ...), so a deterministic palette is the faithful generalisation
 * for real senders — the look is the same, no sample data is copied.
 */
@Composable
private fun TranspiringMailRowView(
    item: MessageRecord,
    account: AccountRecord?,
    alpha: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Account avatar (small): the account receiving the mail.
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(M3AvatarBlue),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = MailFormatting.avatarInitial(
                    account?.displayName,
                    account?.emailAddress.orEmpty(),
                ),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        // Sender avatar (48dp, brand-style palette).
        val (avatarBg, avatarFg) = remember(item.fromAddress) {
            senderAvatarColors(item.fromAddress)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = MailFormatting.avatarInitial(item.fromName, item.fromAddress),
                color = avatarFg,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = MailFormatting.subjectDisplay(item.subject),
                color = M3OnSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = item.snippet.orEmpty(),
                color = M3OnSurfaceMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    // The reference passes alpha per row (primary 1f, secondary 0.55f);
    // applied to the row via graphicsLayer above.
}

/**
 * Deterministic brand-style (background, letter) pairs for the
 * transpiring-row sender avatars, mirroring the reference screenshots'
 * look (coloured disc + white letter, or white disc + coloured letter).
 */
private val TranspiringSenderPalette = listOf(
    Color(0xFF1B5E8A) to Color.White, // blue disc, white letter
    Color.White to Color(0xFFEB1000), // white disc, red letter
    Color(0xFFFC8019) to Color.White, // orange disc, white letter
    Color.White to Color(0xFFE50914), // white disc, red letter
    Color.White to Color(0xFF7B68EE), // white disc, purple letter
    Color(0xFF34A853) to Color.White, // green disc, white letter
    Color.White to Color(0xFF4285F4), // white disc, blue letter
    Color(0xFF7B68EE) to Color.White, // purple disc, white letter
)

private fun senderAvatarColors(key: String): Pair<Color, Color> {
    val index = kotlin.math.abs(key.hashCode()) % TranspiringSenderPalette.size
    return TranspiringSenderPalette[index]
}

/**
 * Conduit between the Gmail logo (left) and the Mailstack node (right).
 *
 * The reference calls this composable but its body was not in the supplied
 * code, so it is built to the reference screenshots:
 *
 * - One smooth ARC: a quadratic-bezier hill from the Gmail circle's right
 *   edge to the Mailstack circle's left edge, peaking 35dp above the
 *   centre line. NOT a tube, NOT a wave — the envelopes ride ON the arc.
 * - Track: the full arc, thin (6dp), faint white. Fill: #9CCAFF, 5dp,
 *   dash-length = progress x arc length, growing Gmail -> Mailstack.
 * - Soft glow: a wide low-alpha stroke behind the track and the fill,
 *   plus a radial glow dot at the fill's leading edge, and halos behind
 *   both nodes.
 * - Progress RING around the Mailstack node: 4dp #9CCAFF arc starting at
 *   the top, sweeping clockwise by progress x 360 deg.
 * - Mailstack node: the reference's illustration (white envelope + dots +
 *   list badge on a pale blue/green disc), NOT the launcher icon.
 * - Envelopes: white 36x24 cards with a coloured dot (red/green/blue,
 *   cycled by spawn id) + a grey line, tangent-aligned, spawned from
 *   REAL progress (one per ~8 imported messages, the same honesty rule
 *   as the old flying-mail animation) — never on a timer.
 *
 * Logos: the Gmail "M" ([R.drawable.ic_gmail_logo], vector traced from the
 * official icon and pixel-verified) in a white circle on the left, 56dp,
 * vertically centred, exactly at the arc start. The Mailstack node is the
 * same 56dp circle, vertically centred, exactly at the arc end.
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

    // Frame clock: drives envelope motion.
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
        // The Mailstack node (illustration + progress ring) is drawn
        // inside the Canvas, at the arc end, so there is no Image here.
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
    }
}

private data class ConduitEnvelope(
    val id: Long,
    val birthNanos: Long,
    val durationMs: Long = 1500L,
)

private const val ENVELOPE_PER_MESSAGES = 8

/** Conduit geometry: logo circles sit exactly at the arc ends. */
private val CONDUIT_LOGO_SIZE = 56.dp
// Inset chosen so the Mailstack progress ring (gap + stroke) ends 1dp
// inside the canvas: -inset + gap + stroke = -1dp.
private val CONDUIT_LOGO_INSET = 8.dp
private val CONDUIT_TRACK_WIDTH = 6.dp
private val CONDUIT_FILL_WIDTH = 5.dp
private val CONDUIT_ARC_PEAK = 35.dp
private val CONDUIT_RING_GAP = 3.dp
private val CONDUIT_RING_WIDTH = 4.dp
private val CONDUIT_ENVELOPE_W = 36.dp
private val CONDUIT_ENVELOPE_H = 24.dp

/** Flying-envelope dot colours, cycled by spawn id (Gmail palette). */
private val ConduitEnvelopeDotColors = listOf(
    Color(0xFFEA4335), // red
    Color(0xFF34A853), // green
    Color(0xFF4285F4), // blue
)

private fun DrawScope.drawConduitScene(
    nowNanos: Long,
    progress: Float?,
    envelopes: MutableList<ConduitEnvelope>,
) {
    val w = size.width
    val cy = size.height / 2f
    val logoR = (CONDUIT_LOGO_SIZE / 2f).toPx()
    val startX = (CONDUIT_LOGO_INSET + CONDUIT_LOGO_SIZE).toPx()
    val endX = w - (CONDUIT_LOGO_INSET + CONDUIT_LOGO_SIZE).toPx()
    val gmailCenter = Offset(startX - logoR, cy)
    val msCenter = Offset(endX + logoR, cy)
    val midX = (startX + endX) / 2f

    // Smooth arc: quadratic bezier hill. The curve's peak (t = 0.5) lands
    // halfway between the centre line and the control point, so the
    // control point sits at twice the peak height.
    val p0 = Offset(startX, cy)
    val p1 = Offset(midX, cy - 2f * CONDUIT_ARC_PEAK.toPx())
    val p2 = Offset(endX, cy)
    val path = Path().apply {
        moveTo(p0.x, p0.y)
        quadraticTo(p1.x, p1.y, p2.x, p2.y)
    }
    val totalLength = quadBezierLength(p0, p1, p2)
    val progressClamped = (progress ?: 0f).coerceIn(0f, 1f)

    // Soft halos behind both nodes.
    drawHalo(gmailCenter, logoR + 12.dp.toPx(), Color.White, 0.14f)
    drawHalo(msCenter, logoR + 12.dp.toPx(), M3PrimaryPillBlue, 0.12f)

    // Track: the full arc, halo + thin faint line.
    drawPath(
        path,
        color = Color.White,
        alpha = 0.05f,
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
    )
    drawPath(
        path,
        color = Color.White,
        alpha = 0.12f,
        style = Stroke(width = CONDUIT_TRACK_WIDTH.toPx(), cap = StrokeCap.Round),
    )

    // Fill: #9CCAFF, glow + line, dash-length = progress x arc length,
    // growing from the Gmail end towards Mailstack.
    val fillLength = progressClamped * totalLength
    if (fillLength > 0f) {
        val dash = PathEffect.dashPathEffect(
            floatArrayOf(fillLength, (totalLength - fillLength).coerceAtLeast(1f)),
            0f,
        )
        drawPath(
            path,
            color = M3PrimaryPillBlue,
            alpha = 0.18f,
            style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash),
        )
        drawPath(
            path,
            color = M3PrimaryPillBlue,
            style = Stroke(width = CONDUIT_FILL_WIDTH.toPx(), cap = StrokeCap.Round, pathEffect = dash),
        )
        // Leading-edge glow dot at the fill's head.
        drawHalo(
            center = quadBezierPoint(p0, p1, p2, progressClamped),
            radius = 16.dp.toPx(),
            color = M3PrimaryPillBlue,
            peakAlpha = 0.35f,
        )
    }

    // Mailstack node: illustration + progress ring.
    drawMailstackNode(msCenter, logoR, progressClamped)

    // Envelopes in flight along the same arc.
    val alive = envelopes.filter { env ->
        (nowNanos - env.birthNanos) < env.durationMs * 1_000_000L
    }
    if (alive.size != envelopes.size) {
        envelopes.removeAll { it !in alive }
    }
    for (env in alive) {
        val t = ((nowNanos - env.birthNanos).toFloat() / (env.durationMs * 1_000_000L))
            .coerceIn(0f, 1f)
        val pos = quadBezierPoint(p0, p1, p2, t)
        val tangent = quadBezierTangent(p0, p1, p2, t)
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
            dot = ConduitEnvelopeDotColors[
                (kotlin.math.abs(env.id) % ConduitEnvelopeDotColors.size).toInt()
            ],
        )
    }
}

/** Soft radial halo: [peakAlpha] at the centre, fading to transparent. */
private fun DrawScope.drawHalo(center: Offset, radius: Float, color: Color, peakAlpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = peakAlpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/**
 * The Mailstack node, to the reference screenshots: the illustration —
 * white envelope (blue flap, red + green dots, grey text lines, blue
 * "send" triangle) + a white checklist badge — on a pale diagonal disc
 * (blue bottom-left -> mint top-right) — plus the progress ring: a 4dp
 * #9CCAFF arc starting at the top, sweeping clockwise by
 * progress x 360 deg.
 */
private fun DrawScope.drawMailstackNode(center: Offset, radius: Float, progress: Float) {
    // Disc: pale diagonal gradient.
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFD9EAF6), Color(0xFFDDEBD9)),
            start = Offset(center.x - radius, center.y + radius),
            end = Offset(center.x + radius, center.y - radius),
        ),
        radius = radius,
        center = center,
    )

    // Envelope: white card, slightly left of centre.
    val ew = 34.dp.toPx()
    val eh = 24.dp.toPx()
    val ex = center.x - ew / 2f - 2.dp.toPx()
    val ey = center.y - eh / 2f + 1.dp.toPx()
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(ex, ey),
        size = Size(ew, eh),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
    // Flap: blue triangle at the envelope's top.
    val flapPath = Path().apply {
        moveTo(ex, ey)
        lineTo(ex + ew, ey)
        lineTo(ex + ew / 2f, ey + eh * 0.38f)
        close()
    }
    drawPath(flapPath, color = Color(0xFF4285F4))
    // Red + green dots, grey text lines.
    drawCircle(
        color = Color(0xFFEA4335),
        radius = 2.5.dp.toPx(),
        center = Offset(ex + ew * 0.16f, ey + eh * 0.58f),
    )
    drawCircle(
        color = Color(0xFF34A853),
        radius = 2.5.dp.toPx(),
        center = Offset(ex + ew * 0.16f, ey + eh * 0.78f),
    )
    drawLine(
        color = Color(0xFF9AA0A6),
        start = Offset(ex + ew * 0.32f, ey + eh * 0.58f),
        end = Offset(ex + ew * 0.84f, ey + eh * 0.58f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF9AA0A6),
        start = Offset(ex + ew * 0.32f, ey + eh * 0.78f),
        end = Offset(ex + ew * 0.84f, ey + eh * 0.78f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Blue "send" triangle on the envelope's left edge.
    val arrowPath = Path().apply {
        moveTo(ex - 3.dp.toPx(), ey + eh * 0.5f)
        lineTo(ex + 4.dp.toPx(), ey + eh * 0.32f)
        lineTo(ex + 4.dp.toPx(), ey + eh * 0.68f)
        close()
    }
    drawPath(arrowPath, color = Color(0xFF4285F4))

    // Badge: white disc bottom-right, mini checklist (dots + lines).
    val badgeCenter = Offset(center.x + radius * 0.45f, center.y + radius * 0.45f)
    val badgeR = radius * 0.34f
    drawCircle(color = Color.White, radius = badgeR, center = badgeCenter)
    drawCircle(
        color = Color(0xFFDADCE0),
        radius = badgeR,
        center = badgeCenter,
        style = Stroke(width = 1.dp.toPx()),
    )
    val badgeDots = listOf(Color(0xFFEA4335), Color(0xFF34A853), Color(0xFF4285F4))
    for (i in 0..2) {
        val dy = badgeCenter.y + (i - 1) * badgeR * 0.5f
        drawCircle(
            color = badgeDots[i],
            radius = 1.6.dp.toPx(),
            center = Offset(badgeCenter.x - badgeR * 0.45f, dy),
        )
        drawLine(
            color = Color(0xFF5F6368),
            start = Offset(badgeCenter.x - badgeR * 0.15f, dy),
            end = Offset(badgeCenter.x + badgeR * 0.5f, dy),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

    // Progress ring: starts at the top, sweeps clockwise by progress x 360 deg.
    val sweep = 360f * progress
    if (sweep > 0f) {
        val ringR = radius + CONDUIT_RING_GAP.toPx() + CONDUIT_RING_WIDTH.toPx() / 2f
        val ringTopLeft = Offset(center.x - ringR, center.y - ringR)
        val ringSize = Size(ringR * 2f, ringR * 2f)
        drawArc(
            color = M3PrimaryPillBlue,
            alpha = 0.20f,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = M3PrimaryPillBlue,
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = ringTopLeft,
            size = ringSize,
            style = Stroke(width = CONDUIT_RING_WIDTH.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** Quadratic bezier point. */
private fun quadBezierPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y,
    )
}

/** Quadratic bezier tangent (direction of travel). */
private fun quadBezierTangent(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        2f * u * (p1.x - p0.x) + 2f * t * (p2.x - p1.x),
        2f * u * (p1.y - p0.y) + 2f * t * (p2.y - p1.y),
    )
}

/** Path length by sampling (Compose has no PathMeasure). 64 samples is plenty. */
private fun quadBezierLength(p0: Offset, p1: Offset, p2: Offset): Float {
    var length = 0f
    var prev = p0
    for (i in 1..64) {
        val pt = quadBezierPoint(p0, p1, p2, i / 64f)
        length += (pt - prev).getDistance()
        prev = pt
    }
    return length
}

/**
 * Flying envelope, to the reference screenshots: white 36x24 card with a
 * V flap line, a coloured dot and a grey line — riding ON the arc,
 * tangent-aligned, with a soft glow behind it.
 */
private fun DrawScope.drawConduitEnvelope(
    center: Offset,
    angleDeg: Float,
    alpha: Float,
    dot: Color,
) {
    val w = CONDUIT_ENVELOPE_W.toPx()
    val h = CONDUIT_ENVELOPE_H.toPx()
    rotate(angleDeg, center) {
        // Soft glow behind the envelope.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f * alpha),
                    Color.White.copy(alpha = 0f),
                ),
                center = center,
                radius = 20.dp.toPx(),
            ),
            radius = 20.dp.toPx(),
            center = center,
        )
        // Shadow.
        drawRoundRect(
            color = Color.Black,
            alpha = 0.16f * alpha,
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f + 2.dp.toPx()),
            size = Size(w, h),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        // Body.
        drawRoundRect(
            color = Color.White,
            alpha = alpha,
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = Size(w, h),
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        // Flap V.
        val flapPath = Path().apply {
            moveTo(center.x - w / 2f + 2.dp.toPx(), center.y - h / 2f + 2.dp.toPx())
            lineTo(center.x, center.y - 1.dp.toPx())
            lineTo(center.x + w / 2f - 2.dp.toPx(), center.y - h / 2f + 2.dp.toPx())
        }
        drawPath(
            flapPath,
            color = Color(0xFFDADCE0),
            style = Stroke(width = 1.5.dp.toPx()),
            alpha = alpha,
        )
        // Coloured dot + grey line.
        drawCircle(
            color = dot,
            radius = 3.dp.toPx(),
            center = Offset(center.x - w * 0.28f, center.y + 1.dp.toPx()),
            alpha = alpha,
        )
        drawLine(
            color = Color(0xFF9AA0A6),
            start = Offset(center.x - w * 0.12f, center.y + 1.dp.toPx()),
            end = Offset(center.x + w * 0.38f, center.y + 1.dp.toPx()),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            alpha = alpha,
        )
    }
}
