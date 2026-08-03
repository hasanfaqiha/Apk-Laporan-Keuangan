package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
//  myBCA-Inspired Banking Palette
//  Clean, trustworthy navy/blue with a strong red accent,
//  mirroring the familiar BCA mobile banking experience.
// ============================================================

// --- Brand blues ---
val BcaNavy = Color(0xFF0A2342)          // Deep navy (primary brand)
val BcaNavyDark = Color(0xFF061527)      // Darker navy for gradients
val BcaBlue = Color(0xFF0D5FA6)          // BCA blue (primary)
val BcaBlueLight = Color(0xFF1B6FB8)     // Lighter blue (hover / tertiary)
val BcaSky = Color(0xFFEAF1F8)           // Soft blue tint (primary container)
val BcaSkyBorder = Color(0xFFC8DCEE)     // Blue-tinted border

// --- Brand red (warnings / critical actions) ---
val BcaRed = Color(0xFFE63329)
val BcaRedDark = Color(0xFFB91F16)
val RoseLightBg = Color(0xFFFFF0EF)      // Soft red background
val RoseLightBorder = Color(0xFFFFD8D4)  // Soft red border
val RosePrimary = BcaRed
val RoseDarkText = Color(0xFF8F201B)

// --- Status colors ---
val BcaGreen = Color(0xFF1E9E58)         // Success / income
val BcaGreenLight = Color(0xFFE3F6EC)
val BcaAmber = Color(0xFFF5A623)         // Warning / due soon
val BcaAmberLight = Color(0xFFFFF4E3)

// --- Light theme neutrals ---
val SlateLightBackground = Color(0xFFF4F6F9) // App background (cool light grey)
val SlateLightSurface = Color(0xFFFFFFFF)    // Cards / sheets
val SlateLightOnSurface = Color(0xFF17233B)  // Primary text (deep navy)

// --- Dark theme neutrals ---
val SlateDarkBackground = Color(0xFF0B1220)  // Rich navy-black
val SlateDarkSurface = Color(0xFF16202E)     // Card surface
val SlateDarkOnSurface = Color(0xFFF2F5FA)   // Crisp off-white

// ------------------------------------------------------------
//  Legacy aliases kept so existing composables keep compiling.
//  The values are now BCA-branded instead of Indigo.
// ------------------------------------------------------------
val IndigoPrimary = BcaBlue
val IndigoSecondary = BcaSky
val IndigoTertiary = BcaBlueLight

val IndigoPrimaryDark = Color(0xFF6DB1E8)
val IndigoSecondaryDark = BcaNavy
val IndigoTertiaryDark = Color(0xFFA9CFF0)

val EmeraldSuccess = BcaGreen
val BlueInfo = BcaBlue

// Chart palette used by analytics (BCA-friendly, colour-blind-safe order)
val CategoryPaletteColors = listOf(
    Color(0xFF0D5FA6), // BCA blue
    Color(0xFFE63329), // BCA red
    Color(0xFF1E9E58), // green
    Color(0xFFF5A623), // amber
    Color(0xFF7B61FF), // violet
    Color(0xFF00897B), // teal
    Color(0xFFEC407A), // pink
    Color(0xFF5C6BC0), // indigo
    Color(0xFF455A64), // slate
    Color(0xFFEF6C00)  // orange
)
