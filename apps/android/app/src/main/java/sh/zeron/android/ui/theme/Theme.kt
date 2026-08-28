package sh.zeron.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Always-dark monochrome palette ported from the iOS/desktop theme
 * (apps/ios/Zeron/Theme/Theme.swift, crates/ui/src/theme.rs). Values are the
 * sampled sRGB results of the same oklch definitions.
 */
object ZeronColors {
    val bg = Color(0xFF060606)
    val surface = Color(0xFF0D0D0D)
    val surfaceRaised = Color(0xFF1C1C1C)
    val border = Color(0x14FFFFFF)
    val borderStrong = Color(0x24FFFFFF)
    val elementHover = Color(0x0FFFFFFF)

    val text = Color(0xFFEBEBEB)
    val textMuted = Color(0xFFA1A1A1)
    val textFaint = Color(0xFF7A7A7A)

    val accent = Color(0xFF7C86FF)       // indigo-400
    val accentStrong = Color(0xFF615CFB)  // indigo-500
    val danger = Color(0xFFFF6467)        // red-400
    val warning = Color(0xFFFFB800)       // amber-400
    val working = Color(0xFFF77FBE)       // pink-400
    val completed = Color(0xFF43D9A3)     // emerald-400

    val inlineCodeText = Color(0xFFC4B4FF) // violet-300
    val inlineCodeWash = Color(0x1F8B7BFF)
}

/** Desktop metrics: body 14/22, code 12.5/18 (Theme.swift comments). */
private val ZeronTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
)

val MonoStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.5.sp, lineHeight = 18.sp)

@Composable
fun ZeronTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ZeronColors.accent,
            onPrimary = ZeronColors.bg,
            secondary = ZeronColors.accentStrong,
            background = ZeronColors.bg,
            onBackground = ZeronColors.text,
            surface = ZeronColors.surface,
            onSurface = ZeronColors.text,
            surfaceVariant = ZeronColors.surfaceRaised,
            onSurfaceVariant = ZeronColors.textMuted,
            error = ZeronColors.danger,
            outline = ZeronColors.borderStrong,
            outlineVariant = ZeronColors.border,
        ),
        typography = ZeronTypography,
        content = content,
    )
}
