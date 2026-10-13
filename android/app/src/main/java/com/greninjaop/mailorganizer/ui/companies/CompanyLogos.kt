package com.greninjaop.mailorganizer.ui.companies

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.R
import com.greninjaop.mailorganizer.ui.mail.SenderAvatar
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Pure domain → logo mapping logic (unit-testable; only depends on R
 * constants, no Android framework calls).
 *
 * Resolution order used by [CompanyLogo]: bundled artwork first, then the
 * user's ICONS-ASSETS repo ([CompanyLogoLoader]), then a letter avatar.
 */
object CompanyLogoResolver {

    /** Filenames that don't follow the "<first-label>.png" convention. */
    private val remoteOverrides = mapOf(
        "amazonaws.com" to "amazon-web-services.png",
    )

    fun normalizeDomain(domain: String): String {
        var d = domain.trim().lowercase()
        if (d.startsWith("www.")) d = d.removePrefix("www.")
        return d
    }

    /** Bundled drawable for a domain, or null when nothing is bundled. */
    fun bundledRes(domain: String): Int? = when (normalizeDomain(domain)) {
        "adobe.com" -> R.drawable.company_adobe
        "amazon.com" -> R.drawable.company_amazon
        "amazonaws.com" -> R.drawable.company_amazon_web_services
        "apple.com" -> R.drawable.company_apple
        "clickup.com" -> R.drawable.company_clickup
        "duolingo.com" -> R.drawable.company_duolingo
        "devpost.com" -> R.drawable.company_devpost
        "alibabacloud.com" -> R.drawable.company_alibabacloud
        // 2026-10-13: official favicon artwork bundled for the most common
        // senders (all full-color, shown as-is).
        "canva.com" -> R.drawable.company_canva
        "dribbble.com" -> R.drawable.company_dribbble
        "google.com" -> R.drawable.company_google
        "youtube.com" -> R.drawable.company_youtube
        "youtu.be" -> R.drawable.company_youtube
        "spotify.com" -> R.drawable.company_spotify
        "reddit.com" -> R.drawable.company_reddit
        "netflix.com" -> R.drawable.company_netflix
        "linkedin.com" -> R.drawable.company_linkedin
        "glasp.co" -> R.drawable.company_glasp
        "openrouter.ai" -> R.drawable.company_openrouter
        "github.com" -> R.drawable.company_github
        "x.com" -> R.drawable.company_x
        "twitter.com" -> R.drawable.company_x
        // 2026-10-13: Kick (official K-mark) + Reddit's mail domain
        // (Reddit notifications come from redditmail.com, not reddit.com).
        "kick.com" -> R.drawable.company_kick
        "redditmail.com" -> R.drawable.company_reddit
        else -> null
    }

    /**
     * True for bundled artwork that is a black glyph on transparency — it
     * must be tinted to the theme instead of shown as-is.
     */
    fun isMonochrome(resId: Int): Boolean =
        resId == R.drawable.company_devpost || resId == R.drawable.company_alibabacloud

    /**
     * Resolves a domain to a filename from `company_icon_names.txt`.
     *
     * Only exact base-name matches are returned: `-dark`/`-light` variants
     * are never picked, so a variant-only match resolves to null.
     */
    fun remoteFile(domain: String, availableNames: Set<String>): String? {
        val normalized = normalizeDomain(domain)
        val parts = normalized.split(".").filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val label = parts.takeLast(2).first()
        if (label.isBlank()) return null
        val candidate = remoteOverrides[normalized] ?: "$label.png"
        return candidate.takeIf { it in availableNames }
    }
}

/**
 * Three-layer company logo fetch: in-memory LRU → disk cache under
 * `cacheDir/company_logos` → the user's ICONS-ASSETS repo on GitHub.
 *
 * All failures return null silently; the caller falls back to the letter
 * avatar. Network always runs on Dispatchers.IO.
 */
object CompanyLogoLoader {

    private const val REPO_BASE =
        "https://raw.githubusercontent.com/greninja-op/ICONS-ASSETS/main/apps-and-dashboards/png/"
    private const val NAMES_ASSET = "company_icon_names.txt"
    private const val DISK_DIR = "company_logos"
    private const val MAX_BYTES = 2 * 1024 * 1024
    private const val TIMEOUT_MS = 10_000

    private val memCache = object : LruCache<String, Bitmap>(cacheSizeKb()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    private val namesLock = Any()
    private var cachedNames: Set<String>? = null

    private fun cacheSizeKb(): Int {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        return (maxKb / 8).coerceAtLeast(4 * 1024)
    }

    /** Filenames from `assets/company_icon_names.txt`, read once per process. */
    fun availableNames(context: Context): Set<String> {
        synchronized(namesLock) {
            cachedNames?.let { return it }
            val names = try {
                context.applicationContext.assets.open(NAMES_ASSET)
                    .bufferedReader()
                    .useLines { lines ->
                        lines.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                    }
            } catch (_: Exception) {
                emptySet()
            }
            cachedNames = names
            return names
        }
    }

    suspend fun load(context: Context, domain: String): Bitmap? =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            memCache.get(domain)?.let { return@withContext it }
            val file = CompanyLogoResolver.remoteFile(
                domain,
                availableNames(appContext),
            ) ?: return@withContext null
            val diskFile = diskFile(appContext, domain, file)
            if (diskFile.exists()) {
                decodeFile(diskFile)?.let { bitmap ->
                    memCache.put(domain, bitmap)
                    return@withContext bitmap
                }
                diskFile.delete() // corrupt entry — drop it so we refetch
            }
            val bitmap = download(file) ?: return@withContext null
            writeDisk(diskFile, bitmap)
            memCache.put(domain, bitmap)
            bitmap
        }

    private fun diskFile(context: Context, domain: String, file: String): File {
        val dir = File(context.cacheDir, DISK_DIR).apply { mkdirs() }
        return File(dir, sha1("$domain:$file") + ".png")
    }

    private fun download(file: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(REPO_BASE + file).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/*")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val contentType = connection.contentType ?: ""
            if (!contentType.startsWith("image/", ignoreCase = true)) return null
            val bytes = readCapped(connection, MAX_BYTES) ?: return null
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun readCapped(connection: HttpURLConnection, maxBytes: Int): ByteArray? {
        return try {
            connection.inputStream.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(32 * 1024)
                var total = 0
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) return null
                    out.write(buf, 0, n)
                }
                out.toByteArray()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeFile(file: File): Bitmap? = try {
        BitmapFactory.decodeFile(file.absolutePath)
    } catch (_: Exception) {
        null
    }

    private fun writeDisk(file: File, bitmap: Bitmap) {
        try {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
            file.delete()
        }
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Company logo in a circle (default 54dp), Gmail-style.
 *
 * Resolution order: bundled artwork (tinted on a tonal circle when the
 * bundle is a monochrome glyph) → ICONS-ASSETS repo via [CompanyLogoLoader]
 * with a crossfade → the existing letter-avatar style while loading or when
 * no logo exists. The logo itself carries no semantics; the row owns the
 * content description.
 */
@Composable
fun CompanyLogo(
    domain: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = MoSpacing.rowAvatar,
) {
    val bundled = remember(domain) { CompanyLogoResolver.bundledRes(domain) }
    if (bundled != null) {
        if (CompanyLogoResolver.isMonochrome(bundled)) {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = bundled),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size / 5)
                        .clip(CircleShape),
                )
            }
        } else {
            // Full-color artwork: NEVER crop or enlarge (2026-10-13) — the
            // whole logo is fitted inside the circle with padding, pasted
            // exactly as-is. Cropping cut wide logos (e.g. Adobe) in half.
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = bundled),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size / 6),
                )
            }
        }
        return
    }
    RemoteCompanyLogo(domain = domain, name = name, modifier = modifier, size = size)
}

@Composable
private fun RemoteCompanyLogo(
    domain: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp,
) {
    val context = LocalContext.current
    var bitmap by remember(domain) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(domain) {
        bitmap = CompanyLogoLoader.load(context, domain)
    }
    AnimatedContent(
        targetState = bitmap,
        label = "company-logo",
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        modifier = modifier.clearAndSetSemantics {},
    ) { loaded ->
        if (loaded != null) {
            // Remote artwork: same no-crop rule as bundled logos (2026-10-13).
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = loaded.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size / 6),
                )
            }
        } else {
            SenderAvatar(
                key = domain,
                displayName = name,
                emailAddress = domain,
                size = size,
            )
        }
    }
}
