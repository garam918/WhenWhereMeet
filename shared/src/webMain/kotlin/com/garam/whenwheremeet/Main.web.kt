package com.garam.whenwheremeet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeViewport
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import whenwheremeet.shared.generated.resources.Res
import whenwheremeet.shared.generated.resources.pretendard_bold
import whenwheremeet.shared.generated.resources.pretendard_extra_bold
import whenwheremeet.shared.generated.resources.pretendard_medium
import whenwheremeet.shared.generated.resources.pretendard_regular
import whenwheremeet.shared.generated.resources.pretendard_semi_bold

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "app") {
        WebFontPreloader { App() }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun WebFontPreloader(content: @Composable () -> Unit) {
    val regular by preloadFont(Res.font.pretendard_regular, FontWeight.Normal)
    val medium by preloadFont(Res.font.pretendard_medium, FontWeight.Medium)
    val semiBold by preloadFont(Res.font.pretendard_semi_bold, FontWeight.SemiBold)
    val bold by preloadFont(Res.font.pretendard_bold, FontWeight.Bold)
    val extraBold by preloadFont(Res.font.pretendard_extra_bold, FontWeight.ExtraBold)

    if (regular != null && medium != null && semiBold != null && bold != null && extraBold != null) {
        content()
    } else {
        Box(Modifier.fillMaxSize().background(Color(0xFFF7F8FC)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF5146E5))
        }
    }
}
