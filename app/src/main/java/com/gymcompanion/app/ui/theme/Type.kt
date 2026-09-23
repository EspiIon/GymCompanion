package com.gymcompanion.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.gymcompanion.app.R

// ── Space Mono — substitute for Lettera Mono LL ───────────────────────────────
// Monospaced mechanical face — ALL data values + ALL CAPS labels.
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs
)

private val SpaceMonoFont = GoogleFont("Space Mono")

val MonoFamily = FontFamily(
    Font(googleFont = SpaceMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SpaceMonoFont, fontProvider = provider, weight = FontWeight.Bold)
)

// ── Space Grotesk — substitute for NType82 (body / headings) ──────────────────
// Same foundry (Colophon) as Nothing's real typefaces.
private val SpaceGroteskFont = GoogleFont("Space Grotesk")

val GroteskFamily = FontFamily(
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = SpaceGroteskFont, fontProvider = provider, weight = FontWeight.Bold)
)

// ── Doto — real dot-matrix font (NDot 57 substitute), hero numbers ONLY ───────
// Official rule: mechanical faces never mix sizes within a usage → ≥24sp only.
private val DotoFont = GoogleFont("Doto")

val DotoFamily = FontFamily(
    Font(googleFont = DotoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = DotoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = DotoFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Numeric font is swappable at runtime (Settings → "dot-matrix" toggle).
val LocalNumericFont = compositionLocalOf { DotoFamily }

/** Seuil sous lequel le dot-matrix est illisible → repli Space Mono. */
const val DOT_MIN_SP = 24

// ── Typography scale ──────────────────────────────────────────────────────────
// Display            → Space Mono / Doto (hero numbers, clock)
// Headline / Title   → Space Grotesk (NType82 substitute)
// Body               → Space Grotesk
// Label              → Space Mono ALL CAPS wide tracking (instrument panel)
val AppTypography = Typography(

    // ── Display — hero numbers (step count, calorie ring centre) ──────────────
    displayLarge = TextStyle(
        fontFamily   = DotoFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 57.sp,
        lineHeight   = 60.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily   = DotoFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 45.sp,
        lineHeight   = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily   = DotoFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 36.sp,
        lineHeight   = 40.sp,
        letterSpacing = (-0.25).sp
    ),

    // ── Headline — section titles ─────────────────────────────────────────────
    headlineLarge = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Bold,
        fontSize     = 32.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 26.sp,
        lineHeight   = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 22.sp,
        lineHeight   = 26.sp
    ),

    // ── Title — card headers, dialog titles ───────────────────────────────────
    titleLarge = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.SemiBold,
        fontSize     = 18.sp,
        lineHeight   = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 15.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 13.sp,
        lineHeight   = 18.sp
    ),

    // ── Body — descriptive text ───────────────────────────────────────────────
    bodyLarge = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 15.sp,
        lineHeight   = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 13.sp,
        lineHeight   = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily   = GroteskFamily,
        fontWeight   = FontWeight.Normal,
        fontSize     = 11.sp,
        lineHeight   = 15.sp,
        letterSpacing = 0.2.sp
    ),

    // ── Label — ALL CAPS metadata, Nothing instrument-panel tags ──────────────
    // Wide letter-spacing is the Nothing signature for labels
    labelLarge = TextStyle(
        fontFamily   = MonoFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 2.sp       // NDot-style wide tracking
    ),
    labelMedium = TextStyle(
        fontFamily   = MonoFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 10.sp,
        lineHeight   = 13.sp,
        letterSpacing = 1.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily   = MonoFamily,
        fontWeight   = FontWeight.Medium,
        fontSize     = 9.sp,
        lineHeight   = 12.sp,
        letterSpacing = 1.2.sp
    )
)
