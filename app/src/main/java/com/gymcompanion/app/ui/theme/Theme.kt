package com.gymcompanion.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Nothing OS — Official Brand Palette ───────────────────────────────────────
// Foundation carries layout and text; Primary colours are accents only.
// Dark mode only: instrument panel in a dark room. OLED black, white data.

// ── Foundation ────────────────────────────────────────────────────────────────
val NothingBlack  = Color(0xFF000000)   // Pure Black — screen background (OLED)
val NothingDeep   = Color(0xFF111111)   // surface — cards, FABs
val NothingCardSurface = Color(0xFF171717) // surface carte Nothing — gris très sombre neutre, plat
val NothingDark   = Color(0xFF111111)   // dialog surface (élévation = bordure #333333)
val NothingDark2  = Color(0xFF1A1A1A)   // raised surface — inputs imbriqués
val WindowGrey    = Color(0xFFB1B3B3)   // Window Grey
val NGrey         = Color(0xFFDCD7D2)   // N-Grey
val NothingWhite  = Color(0xFFFFFFFF)   // Pure White — display uniquement

// ── Borders (solid greys — defines form without colour) ───────────────────────
val NothingBorder        = Color(0xFF222222)  // default card edge
val NothingBorderMid     = Color(0xFF333333)  // active / focused / dialog edge
val NothingBorderStrong  = Color(0xFF4D4D4D)  // selected state
val NothingDivider       = Color(0xFF222222)  // section dividers

// ── Text hierarchy (grey scale IS the hierarchy) ──────────────────────────────
// NothingGrey1 = primary body · NothingGrey2 = secondary labels · NothingGrey3 = disabled
val NothingGrey1   = NGrey               // #DCD7D2
val NothingGrey2   = WindowGrey          // #B1B3B3
val NothingGrey3   = Color(0xFF777777)   // texte secondaire discret : jamais une information essentielle

// Couleurs de visualisation. Elles complètent les accents Nothing sans transformer
// chaque zone en signal d'alerte.
val DataBlue        = Color(0xFF63A7FF)
val DataMint        = Color(0xFF59D6B4)
val DataLavender    = Color(0xFFB3A5FF)
val DataOrange      = Color(0xFFFF9F43)
val DataAmber       = Color(0xFFFFC857)
val DataCoral       = Color(0xFFE76F51)
val PetCream        = Color(0xFFFFD7A3)
val PetCreamShade   = Color(0xFFB97843)

// ── Primary accents (official) — never decorative, one moment per screen ──────
val NothingRed     = Color(0xFFC8102E)   // N-Red — STATUS: streak, "today", over-limit
val NothingYellow  = Color(0xFFFFC700)   // N-Yellow — INTELLIGENCE: AI / scan auto-fill only
val NothingBlue    = Color(0xFF002F6C)   // N-Blue — PROGRESS: rings / bars only

// ── Legacy semantic aliases (kept so screens compile unchanged) ───────────────
val Primary         = NothingWhite
val PrimaryVariant  = NothingGrey1
val Secondary       = NothingYellow
val SecondaryVariant = Color(0xFFFFDE55)
val Background      = NothingBlack
val Surface         = NothingDeep
val SurfaceVariant  = NothingDark
val OnBackground    = NothingWhite
val OnSurface       = NothingWhite
val OnSurfaceVariant = NothingGrey2
val BorderSubtle    = NothingBorder
val Error           = NothingRed

// ── Nutrition macro accents (data values only, labels stay grey) ──────────────
val ProteinColor = Color(0xFFEAC7A4)     // ivoire chaud : protéines
val CarbsColor   = DataAmber              // glucides : jaune doux
val FatColor     = DataCoral              // lipides : corail chaud
val CalorieColor = DataOrange             // calories : orange principal

// ── Special ───────────────────────────────────────────────────────────────────
val StreakColor    = NothingRed
val StreakGlow     = Color(0xFF8A0A1E)
val FatArcColor    = NothingRed
val MuscleArcColor = DataLavender

// ── Kept for compatibility with older references ──────────────────────────────
val AccentPurple   = Color(0xFF7B6FFF)   // deprecated — do not use in new code
val AccentTeal     = Color(0xFF00D4AA)   // deprecated — do not use in new code
val AccentOrange   = NothingRed          // remapped — streaks/calories now official red
val AccentRed      = NothingRed
val AccentAmber    = NothingYellow

// ── Material 3 colour scheme ──────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = DataMint,
    onPrimary            = NothingBlack,
    primaryContainer     = NothingDark2,
    onPrimaryContainer   = NothingGrey1,
    secondary            = DataMint,
    onSecondary          = NothingBlack,
    secondaryContainer   = Color(0xFF163A31),
    onSecondaryContainer = DataMint,
    tertiary             = DataLavender,
    onTertiary           = NothingWhite,
    background           = NothingBlack,
    onBackground         = NothingWhite,
    surface              = NothingDeep,
    onSurface            = NothingWhite,
    surfaceVariant       = NothingDark,
    onSurfaceVariant     = NothingGrey2,
    error                = NothingRed,
    onError              = NothingWhite,
    outline              = NothingBorder,
    outlineVariant       = NothingBorderMid,
    scrim                = Color(0xCC000000)
)

@Composable
fun GymCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
