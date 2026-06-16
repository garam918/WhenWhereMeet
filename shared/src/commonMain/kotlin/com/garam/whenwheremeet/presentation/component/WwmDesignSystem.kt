package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WwmIndigo = Color(0xFF3525CD)
val WwmIndigoBright = Color(0xFF4F46E5)
val WwmBackground = Color(0xFFF9F9FF)
val WwmNavBackground = Color(0xFFE7EEFE)
val WwmSoftIndigo = Color(0xFFE2DFFF)
val WwmBorder = Color(0xFFDCE2F3)
val WwmText = Color(0xFF151C27)
val WwmMuted = Color(0xFF464555)
val WwmMint = Color(0xFF6CF8BB)
val WwmMintText = Color(0xFF00714D)
val WwmOrange = Color(0xFFFFD7A3)

private val WwmColors: ColorScheme = lightColorScheme(
    primary = WwmIndigo,
    onPrimary = Color.White,
    primaryContainer = WwmSoftIndigo,
    onPrimaryContainer = WwmIndigo,
    secondary = WwmIndigoBright,
    secondaryContainer = WwmNavBackground,
    tertiaryContainer = WwmOrange,
    background = WwmBackground,
    onBackground = WwmText,
    surface = Color.White,
    onSurface = WwmText,
    surfaceVariant = Color(0xFFF0F3FF),
    onSurfaceVariant = WwmMuted,
    outline = WwmBorder,
    outlineVariant = WwmBorder,
)

@Composable
fun WwmTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = WwmColors, content = content)
}

@Composable
fun WwmTopBar(
    title: String = "어디서봐",
    leadingText: String = "☰",
    onLeadingClick: (() -> Unit)? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).background(WwmBackground).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            leadingText,
            modifier = Modifier.clickable(enabled = onLeadingClick != null) { onLeadingClick?.invoke() }.padding(8.dp),
            color = if (onLeadingClick == null) WwmIndigo else WwmMuted,
            fontSize = 20.sp,
        )
        Text(title, color = WwmIndigo, fontSize = 23.sp, fontWeight = FontWeight.Medium)
        if (trailingText == null) {
            Box(Modifier.height(32.dp).padding(horizontal = 16.dp))
        } else {
            Box(
                Modifier.background(Color.White, CircleShape)
                    .border(2.dp, WwmBorder, CircleShape)
                    .clickable(enabled = onTrailingClick != null) { onTrailingClick?.invoke() }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) { Text(trailingText, color = WwmIndigo, fontWeight = FontWeight.Bold) }
        }
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
