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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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

val WwmIndigo = Color(0xFF4F46E5)
val WwmIndigoBright = Color(0xFF4338CA)
val WwmBackground = Color(0xFFF8F7FF)
val WwmNavBackground = Color(0xFFF8F7FF)
val WwmSoftIndigo = Color(0xFFEDEBFF)
val WwmBorder = Color(0xFFE5E7EB)
val WwmText = Color(0xFF111827)
val WwmMuted = Color(0xFF6B7280)
val WwmMint = Color(0xFF22C55E)
val WwmMintText = Color(0xFF16A34A)
val WwmOrange = Color(0xFFF59E0B)
val WwmError = Color(0xFFEF4444)

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
    surfaceVariant = WwmBackground,
    onSurfaceVariant = WwmMuted,
    error = WwmError,
    onError = Color.White,
    outline = WwmBorder,
    outlineVariant = WwmBorder,
)

@Composable
private fun WwmTypography() = Typography(
    fontFamily = FontFamily(
        Font(Res.font.pretendard_regular, FontWeight.Normal),
        Font(Res.font.pretendard_medium, FontWeight.Medium),
        Font(Res.font.pretendard_semi_bold, FontWeight.SemiBold),
        Font(Res.font.pretendard_bold, FontWeight.Bold),
        Font(Res.font.pretendard_extra_bold, FontWeight.ExtraBold),
    )
)

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
        Modifier.fillMaxWidth().height(56.dp).background(WwmBackground).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
            leadingText?.let {
                Text(
                    it,
                    modifier = Modifier.clickable(enabled = onLeadingClick != null) { onLeadingClick?.invoke() }.padding(8.dp),
                    color = if (onLeadingClick == null) WwmIndigo else WwmMuted,
                    fontSize = 20.sp,
                )
            }
        }
        Text(title, color = WwmIndigo, fontSize = 23.sp, fontWeight = FontWeight.Medium)
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
            when {
                trailingIcon != null -> IconButton(
                    onClick = { onTrailingClick?.invoke() },
                    enabled = onTrailingClick != null,
                    modifier = Modifier.size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, WwmBorder, CircleShape)
                        .semantics {
                            trailingIconContentDescription?.let { contentDescription = it }
                        },
                ) { trailingIcon() }

                trailingText != null -> Box(
                    Modifier.background(Color.White, CircleShape)
                        .border(2.dp, WwmBorder, CircleShape)
                        .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
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
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WwmIndigo),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun WwmOutlineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(999.dp),
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
fun WwmEmptyState(text: String) {
    WwmCard(Modifier.fillMaxWidth()) {
        Text(
            text,
            Modifier.fillMaxWidth().padding(28.dp),
            color = WwmMuted,
            textAlign = TextAlign.Center,
        )
    }
}
