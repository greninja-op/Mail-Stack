package com.greninjaop.mailorganizer.ui.import

import android.net.Uri
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.R
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.theme.GlassCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlin.math.abs

/**
 * Import intro + animated import screens (2026-10-12).
 *
 * After a new account connects, the user gets an explicit "Import now" step.
 * The animation shows mail flying from the account avatar to the Mailstack
 * logo, driven by REAL sync progress (one envelope per ~8 messages) — the
 * motion is honest, never decorative filler. All chrome is strict Material 3:
 * Surface, M3 Button/TextButton, M3 LinearProgressIndicator, theme colors and
 * typography throughout.
 */
@Composable
fun ImportIntroScreen(
    account: AccountRecord,
    onImportNow: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 2026-10-13 (Breeze): transparent so the background artwork shows.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
    ) {
        // 2026-10-14 (Breeze glass everywhere): centred content block in a
        // GlassCard; transparent root + centering unchanged. Buttons untouched.
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MoSpacing.xl),
        ) {
        Column(
            modifier = Modifier
                .padding(MoSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "Mailstack logo",
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
            )
            Spacer(Modifier.height(MoSpacing.lg))
            Text(
                text = "Import your mail",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(MoSpacing.sm))
            Text(
                text = "Mailstack will fetch your Gmail messages onto this phone " +
                    "so you can browse and organize them offline. " +
                    "This usually takes a minute or two.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(MoSpacing.lg))
            AccountAvatar(account = account, size = 48.dp)
            Spacer(Modifier.height(MoSpacing.xs))
            Text(
                text = account.emailAddress,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(MoSpacing.xl))
            Button(
                onClick = onImportNow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import now")
            }
            Spacer(Modifier.height(MoSpacing.sm))
            TextButton(onClick = onNotNow) {
                Text("Not now")
            }
        }
        }
        }
    }
}

@Composable
fun ImportFlowScreen(
    viewModel: ImportViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    // 2026-10-13 (Breeze): transparent so the background artwork shows.
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Transparent,
    ) {
        when (val s = state) {
            is ImportViewModel.ImportUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ImportViewModel.ImportUiState.Intro -> {
                ImportIntroScreen(
                    account = s.account,
                    onImportNow = viewModel::startImport,
                    onNotNow = {
                        viewModel.skipImport()
                        onDone()
                    },
                )
            }
            is ImportViewModel.ImportUiState.Importing -> {
                ImportAnimationContent(
                    account = s.account,
                    stageText = s.stageText,
                    progress = s.progress,
                    processed = s.processed,
                    totalEstimate = s.totalEstimate,
                )
            }
            is ImportViewModel.ImportUiState.Done -> {
                ImportResultContent(
                    icon = {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(72.dp),
                        )
                    },
                    title = "Import complete",
                    message = s.message,
                    primaryLabel = "Continue",
                    onPrimary = onDone,
                    secondaryLabel = null,
                    onSecondary = null,
                )
            }
            is ImportViewModel.ImportUiState.Error -> {
                ImportResultContent(
                    icon = {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(72.dp),
                        )
                    },
                    title = "Import didn't finish",
                    message = s.message,
                    primaryLabel = "Try again",
                    onPrimary = viewModel::startImport,
                    secondaryLabel = "Continue anyway",
                    onSecondary = onDone,
                )
            }
        }
    }
}

@Composable
private fun ImportResultContent(
    icon: @Composable () -> Unit,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: (() -> Unit)?,
) {
    // 2026-10-14 (Breeze glass everywhere): centred content block in a
    // GlassCard; centering unchanged. Buttons untouched.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(MoSpacing.xl),
    ) {
    Column(
        modifier = Modifier
            .padding(MoSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Spacer(Modifier.height(MoSpacing.lg))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(MoSpacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(MoSpacing.xl))
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(MoSpacing.sm))
            TextButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
    }
    }
}

/**
 * The flying-mail animation. Envelopes travel along a curved path from the
 * account avatar (top) to the Mailstack logo (bottom); each envelope
 * represents ~8 actually-imported messages, spawned from live progress.
 */
@Composable
private fun ImportAnimationContent(
    account: AccountRecord,
    stageText: String,
    progress: Float?,
    processed: Int,
    totalEstimate: Int?,
) {
    val envelopes = remember { mutableStateListOf<FlyingEnvelope>() }
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
                    FlyingEnvelope(
                        id = spawnSeed,
                        birthNanos = System.nanoTime() + i * 120_000_000L,
                        // Stagger lanes for a natural arc spread.
                        lane = ((spawnSeed % 5) - 2) / 2f,
                    ),
                )
            }
        }
    }

    // Frame clock: drives envelope motion; recomposes the Canvas each frame.
    var frameNanos by remember { mutableLongStateOf(System.nanoTime()) }
    LaunchedEffect(Unit) {
        withInfiniteAnimationFrameNanos { frameNanos = it }
    }

    val textMeasurer = rememberTextMeasurer()
    val scheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val initial = remember(account.displayName, account.emailAddress) {
        com.greninjaop.mailorganizer.ui.mail.MailFormatting.avatarInitial(
            account.displayName, account.emailAddress,
        )
    }
    val avatarContainer = scheme.primaryContainer
    val avatarContent = scheme.onPrimaryContainer

    Column(Modifier.fillMaxSize()) {
        // Flight theater: fixed height; the Canvas draws the track, avatar
        // and envelopes, while the logo is a composable pinned to the same
        // geometric position the envelopes target.
        Box(
            Modifier
                .fillMaxWidth()
                .height(FLIGHT_HEIGHT)
                .padding(horizontal = MoSpacing.xl),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawFlightScene(
                    nowNanos = frameNanos,
                    envelopes = envelopes,
                    textMeasurer = textMeasurer,
                    initial = initial,
                    avatarContainer = avatarContainer,
                    avatarContent = avatarContent,
                    envelopeBody = scheme.primary,
                    envelopeFlap = scheme.onPrimary,
                    trackColor = scheme.outlineVariant,
                    labelStyle = typography.labelSmall,
                    labelColor = scheme.onSurfaceVariant,
                    email = account.emailAddress,
                )
            }
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "Mailstack logo",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LOGO_BOTTOM_PADDING)
                    .size(LOGO_SIZE)
                    .clip(CircleShape),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MoSpacing.xl),
        ) {
            Text("Importing your mail", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(MoSpacing.sm))
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(MoSpacing.xs))
            Text(
                text = importProgressLabel(progress, processed, totalEstimate, stageText),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun importProgressLabel(
    progress: Float?,
    processed: Int,
    totalEstimate: Int?,
    stageText: String,
): String {
    if (progress == null || totalEstimate == null || totalEstimate <= 0) return stageText
    val count = if (processed > totalEstimate) "$processed emails"
    else "$processed of ~$totalEstimate emails"
    return "${(progress * 100).toInt()}% · $count · $stageText"
}

private data class FlyingEnvelope(
    val id: Long,
    val birthNanos: Long,
    val lane: Float,
    val durationMs: Long = 1500L,
)

private const val ENVELOPE_PER_MESSAGES = 8

/** Fixed flight-theater height; avatar/logo/envelope geometry derives from it. */
private val FLIGHT_HEIGHT = 340.dp
private val LOGO_SIZE = 84.dp
private val LOGO_BOTTOM_PADDING = 20.dp

private fun DrawScope.drawFlightScene(
    nowNanos: Long,
    envelopes: MutableList<FlyingEnvelope>,
    textMeasurer: TextMeasurer,
    initial: String,
    avatarContainer: Color,
    avatarContent: Color,
    envelopeBody: Color,
    envelopeFlap: Color,
    trackColor: Color,
    labelStyle: androidx.compose.ui.text.TextStyle,
    labelColor: Color,
    email: String,
) {
    val w = size.width
    val avatarC = Offset(w / 2f, 64.dp.toPx())
    val avatarR = 30.dp.toPx()
    // Matches the logo Image composable: bottom-pinned, LOGO_SIZE, with
    // LOGO_BOTTOM_PADDING below it inside FLIGHT_HEIGHT.
    val logoC = Offset(w / 2f, (FLIGHT_HEIGHT - LOGO_BOTTOM_PADDING - LOGO_SIZE / 2).toPx())
    val logoSize = LOGO_SIZE.toPx()

    // Faint flight track: curved guide from avatar to logo.
    val track = Path().apply {
        moveTo(avatarC.x, avatarC.y + avatarR)
        quadraticTo(w / 2f, (avatarC.y + logoC.y) / 2f, logoC.x, logoC.y - logoSize / 2f)
    }
    drawPath(
        track,
        color = trackColor,
        style = Stroke(
            width = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx())),
        ),
        alpha = 0.6f,
    )

    // Account avatar.
    drawCircle(avatarContainer, avatarR, avatarC)
    val textLayout = textMeasurer.measure(
        text = initial,
        style = labelStyle.copy(color = avatarContent),
    )
    drawText(
        textLayout,
        topLeft = Offset(
            avatarC.x - textLayout.size.width / 2f,
            avatarC.y - textLayout.size.height / 2f,
        ),
    )
    val emailLayout = textMeasurer.measure(
        text = email,
        style = labelStyle.copy(color = labelColor),
    )
    drawText(
        emailLayout,
        topLeft = Offset(
            (w - emailLayout.size.width) / 2f,
            avatarC.y + avatarR + 6.dp.toPx(),
        ),
    )

    // Envelopes in flight along quadratic beziers (lane offsets the arc).
    val alive = envelopes.filter { env ->
        (nowNanos - env.birthNanos) < env.durationMs * 1_000_000L
    }
    if (alive.size != envelopes.size) {
        envelopes.removeAll { it !in alive }
    }
    for (env in alive) {
        val t = ((nowNanos - env.birthNanos).toFloat() / (env.durationMs * 1_000_000L))
            .coerceIn(0f, 1f)
        val p0 = Offset(avatarC.x, avatarC.y + avatarR + 4.dp.toPx())
        val p2 = Offset(logoC.x, logoC.y - logoSize / 2f - 4.dp.toPx())
        val p1 = Offset(w / 2f + env.lane * w * 0.28f, (p0.y + p2.y) / 2f)
        val pos = quadBezier(p0, p1, p2, t)
        val tangent = quadBezierTangent(p0, p1, p2, t)
        val angleDeg = Math.toDegrees(kotlin.math.atan2(tangent.y, tangent.x).toDouble()).toFloat()
        val alpha = when {
            t < 0.08f -> t / 0.08f
            t > 0.85f -> (1f - t) / 0.15f
            else -> 1f
        }
        val scale = 0.75f + 0.25f * (1f - t)
        drawFlyingEnvelope(
            center = pos,
            angleDeg = angleDeg,
            scale = scale,
            alpha = alpha,
            body = envelopeBody,
            flap = envelopeFlap,
        )
    }
}

private fun quadBezier(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x,
        u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y,
    )
}

private fun quadBezierTangent(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    return Offset(
        2f * (1f - t) * (p1.x - p0.x) + 2f * t * (p2.x - p1.x),
        2f * (1f - t) * (p1.y - p0.y) + 2f * t * (p2.y - p1.y),
    )
}

/** Stylized envelope glyph: rounded body + flap chevron. */
private fun DrawScope.drawFlyingEnvelope(
    center: Offset,
    angleDeg: Float,
    scale: Float,
    alpha: Float,
    body: Color,
    flap: Color,
) {
    val w = 40.dp.toPx() * scale
    val h = 28.dp.toPx() * scale
    rotate(angleDeg, center) {
        // Body.
        drawRoundRect(
            color = body,
            topLeft = Offset(center.x - w / 2f, center.y - h / 2f),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx() * scale),
            alpha = alpha,
        )
        // Flap chevron.
        val flapPath = Path().apply {
            moveTo(center.x - w / 2f + 3.dp.toPx() * scale, center.y - h / 2f + 4.dp.toPx() * scale)
            lineTo(center.x, center.y + 2.dp.toPx() * scale)
            lineTo(center.x + w / 2f - 3.dp.toPx() * scale, center.y - h / 2f + 4.dp.toPx() * scale)
        }
        drawPath(
            flapPath,
            color = flap,
            style = Stroke(width = 2.5.dp.toPx() * scale),
            alpha = alpha,
        )
    }
}
