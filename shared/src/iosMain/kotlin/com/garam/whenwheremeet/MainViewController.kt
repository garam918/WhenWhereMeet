package com.garam.whenwheremeet

import androidx.compose.ui.window.ComposeUIViewController
import com.garam.whenwheremeet.platform.submitIosDeepLinkUrl

fun MainViewController() = ComposeUIViewController { App() }

fun handleDeepLinkUrl(url: String) = submitIosDeepLinkUrl(url)
