package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import whenwheremeet.shared.generated.resources.Res
import whenwheremeet.shared.generated.resources.pretendard_bold
import whenwheremeet.shared.generated.resources.pretendard_extra_bold
import whenwheremeet.shared.generated.resources.pretendard_medium
import whenwheremeet.shared.generated.resources.pretendard_regular
import whenwheremeet.shared.generated.resources.pretendard_semi_bold

val WwmIndigo = Color(0xFF5146E5)
val WwmIndigoBright = Color(0xFF3F35D3)
val WwmBackground = Color(0xFFF7F8FC)
val WwmNavBackground = Color.White
val WwmSoftIndigo = Color(0xFFEFEEFF)
val WwmBorder = Color(0xFFE6E7EE)
val WwmText = Color(0xFF171A24)
val WwmMuted = Color(0xFF676B7A)
val WwmMint = Color(0xFF21B981)
val WwmMintText = Color(0xFF11815B)
val WwmOrange = Color(0xFFF59E0B)
val WwmError = Color(0xFFEF5B5B)
val WwmSurfaceSubtle = Color(0xFFF0F1F6)

private val WwmColors: ColorScheme = lightColorScheme(
    primary = WwmIndigo,
    onPrimary = Color.White,
    primaryContainer = WwmSoftIndigo,
    onPrimaryContainer = WwmIndigoBright,
    secondary = WwmMint,
    onSecondary = Color.White,
    secondaryContainer = WwmMint.copy(alpha = 0.14f),
    onSecondaryContainer = WwmMintText,
    tertiaryContainer = WwmOrange,
    onTertiaryContainer = Color.White,
    background = WwmBackground,
    onBackground = WwmText,
    surface = Color.White,
    onSurface = WwmText,
    surfaceVariant = WwmSurfaceSubtle,
    onSurfaceVariant = WwmMuted,
    error = WwmError,
    onError = Color.White,
    outline = WwmBorder,
    outlineVariant = WwmBorder,
)

@Composable
private fun WwmTypography(): Typography {
    val fontFamily = FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semi_bold, FontWeight.SemiBold),
        Font(Res.font.pretendard_bold, FontWeight.Bold),
        Font(Res.font.pretendard_extra_bold, FontWeight.ExtraBold),
    )
    return Typography(
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 21.sp, lineHeight = 29.sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 27.sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    )
}

@Composable
fun WwmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WwmColors, typography = WwmTypography(), content = content)
}

@Composable
fun WwmTopBar(
    title: String = "언제어디",
    leadingText: String? = null,
    onLeadingClick: (() -> Unit)? = null,
    trailingText: String? = null,
    trailingIconContentDescription: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(Color.White).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
            leadingText?.let {
                Text(
                    it,
                    modifier = Modifier.clickable(enabled = onLeadingClick != null) { onLeadingClick?.invoke() }.padding(8.dp),
                    color = WwmText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Text(title, color = WwmIndigo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                trailingIcon != null -> IconButton(
                    onClick = { onTrailingClick?.invoke() },
                    enabled = onTrailingClick != null,
                    modifier = Modifier.size(40.dp)
                        .semantics {
                            trailingIconContentDescription?.let { contentDescription = it }
                        },
                ) { trailingIcon() }

                trailingText != null -> Box(
                    Modifier.background(WwmSoftIndigo, CircleShape)
                        .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(trailingText, color = WwmIndigo, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun RefreshActionIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val strokeWidth = 2.2.dp.toPx()
        drawArc(
            color = WwmIndigo,
            startAngle = 35f,
            sweepAngle = 285f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = WwmIndigo,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.83f, size.height * 0.18f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.83f, size.height * 0.42f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = WwmIndigo,
            start = androidx.compose.ui.geometry.Offset(size.width * 0.83f, size.height * 0.18f),
            end = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun WwmCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WwmBorder),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) { content() }
}

@Composable
fun WwmPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WwmIndigo),
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
}

@Composable
fun WwmOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WwmIndigo),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WwmIndigo),
    ) { Text(text) }
}

@Composable
fun WwmSectionHeader(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, color = WwmIndigo, fontSize = 18.sp)
        Text(title, color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WwmEmptyState(
    text: String,
    modifier: Modifier = Modifier,
    icon: String = "○",
) {
    Column(
        modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(1.dp, WwmBorder, RoundedCornerShape(16.dp)).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(42.dp).background(WwmSoftIndigo, CircleShape), contentAlignment = Alignment.Center) {
            Text(icon, color = WwmIndigo, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, color = WwmMuted, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
    }
}
