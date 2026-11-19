package com.ignatrozhko.governedworkflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ignatrozhko.governedworkflow.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

@OptIn(ExperimentalTextApi::class)
val SpaceGroteskFamily = FontFamily(
    spaceGrotesk(FontWeight.Medium),
    spaceGrotesk(FontWeight.SemiBold),
    spaceGrotesk(FontWeight.Bold),
)

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: FontWeight) =
    Font(R.font.space_grotesk, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

private val heading = TextStyle(fontFamily = SpaceGroteskFamily, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
private val ui = TextStyle(fontFamily = InterFamily, letterSpacing = 0.sp)

val CheckpointTypography = Typography(
    headlineMedium = heading.copy(fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = heading.copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = heading.copy(fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = heading.copy(fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    titleSmall = ui.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = ui.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = ui.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = ui.copy(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = ui.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = ui.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = ui.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

/** JetBrains Mono styles for code, paths, branches, phase numbers and the terminal. */
object MonoText {
    val code = TextStyle(fontFamily = MonoFamily, fontSize = 13.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = MonoFamily, fontSize = 14.sp, lineHeight = 21.sp)
    val small = TextStyle(fontFamily = MonoFamily, fontSize = 12.sp, lineHeight = 17.sp)
    val caption = TextStyle(fontFamily = MonoFamily, fontSize = 11.sp, lineHeight = 15.sp)
}
