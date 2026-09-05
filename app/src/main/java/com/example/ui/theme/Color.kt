package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
//  Premium Finance UI Palette (Purple/Indigo Gradient)
//  Inspired by modern fintech apps with glassmorphism feel
// ============================================================

// --- Brand purple/indigo ---
val PremiumPrimary = Color(0xFF6366F1)       // Primary indigo
val PremiumPrimaryDark = Color(0xFF4F46E5)   // Darker indigo
val PremiumPrimaryLight = Color(0xFF818CF8)  // Lighter indigo
val PremiumSecondary = Color(0xFF7C3AED)     // Purple accent
val PremiumGradientStart = Color(0xFF7C6CFF) // Gradient start
val PremiumGradientMid = Color(0xFF6366F1)   // Gradient mid
val PremiumGradientEnd = Color(0xFF4338CA)   // Gradient end

// --- Surface containers ---
val PremiumSurface = Color(0xFFFFFFFF)       // White cards
val PremiumSurfaceVariant = Color(0xFFF0EDFF) // Light purple tint
val PremiumBackground = Color(0xFFF2F4F7)    // App background (cool grey)
val PremiumSurfaceTint = Color(0xFF6366F1)

// --- Status colors ---
val PremiumGreen = Color(0xFF10B981)         // Success / income
val PremiumGreenLight = Color(0xFFD1FAE5)
val PremiumRed = Color(0xFFEF4444)           // Expense / error
val PremiumRedLight = Color(0xFFFEE2E2)
val PremiumAmber = Color(0xFFF59E0B)         // Warning / bills
val PremiumAmberLight = Color(0xFFFEF3C7)
val PremiumBlue = Color(0xFF3B82F6)          // Info / cards
val PremiumBlueLight = Color(0xFFDBEAFE)

// --- Dark theme ---
val PremiumDarkBackground = Color(0xFF0F1119)
val PremiumDarkSurface = Color(0xFF1A1D2E)
val PremiumDarkSurfaceVariant = Color(0xFF2A2744)
val PremiumDarkOnSurface = Color(0xFFF0F2F5)
val PremiumDarkOnSurfaceVariant = Color(0xFF7A8199)
val PremiumDarkPrimary = Color(0xFF818CF8)
val PremiumDarkPrimaryContainer = Color(0xFF312E81)
val PremiumDarkGradientStart = Color(0xFF7C6CFF)
val PremiumDarkGradientEnd = Color(0xFF4338CA)

// --- Chart palette (color-blind safe) ---
val CategoryPaletteColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF3B82F6), // Blue
    Color(0xFFEF4444), // Red
    Color(0xFF14B8A6), // Teal
    Color(0xFF64748B), // Slate
    Color(0xFFF97316), // Orange
)

// --- Light theme neutrals ---
val PremiumLightOnSurface = Color(0xFF1A1D26)
val PremiumLightOnSurfaceVariant = Color(0xFF8B92A5)
val PremiumLightOutline = Color(0xFFE8ECF1)
val PremiumLightOutlineVariant = Color(0xFFE8ECF1)

// --- Dark theme neutrals ---
val PremiumDarkOutline = Color(0xFF262A3A)
val PremiumDarkOutlineVariant = Color(0xFF262A3A)

// ------------------------------------------------------------
//  Legacy aliases (kept for backward compatibility)
// ------------------------------------------------------------
val BcaNavy = PremiumPrimaryDark
val BcaNavyDark = PremiumPrimaryDark
val BcaBlue = PremiumPrimary
val BcaBlueLight = PremiumPrimaryLight
val BcaSky = PremiumSurfaceVariant
val BcaSkyBorder = PremiumLightOutlineVariant

val BcaRed = PremiumRed
val BcaRedDark = Color(0xFFB91C1C)
val RoseLightBg = PremiumRedLight
val RoseLightBorder = Color(0xFFFECACA)
val RosePrimary = PremiumRed
val RoseDarkText = Color(0xFF7F1D1D)

val BcaGreen = PremiumGreen
val BcaGreenLight = PremiumGreenLight
val BcaAmber = PremiumAmber
val BcaAmberLight = PremiumAmberLight

val SlateLightBackground = PremiumBackground
val SlateLightSurface = PremiumSurface
val SlateLightOnSurface = PremiumLightOnSurface

val SlateDarkBackground = PremiumDarkBackground
val SlateDarkSurface = PremiumDarkSurface
val SlateDarkOnSurface = PremiumDarkOnSurface

val IndigoPrimary = PremiumPrimary
val IndigoSecondary = PremiumSurfaceVariant
val IndigoTertiary = PremiumPrimaryLight
val IndigoPrimaryDark = PremiumDarkPrimary
val IndigoSecondaryDark = PremiumPrimaryDark
val IndigoTertiaryDark = PremiumPrimaryLight

val EmeraldSuccess = PremiumGreen
val BlueInfo = PremiumBlue