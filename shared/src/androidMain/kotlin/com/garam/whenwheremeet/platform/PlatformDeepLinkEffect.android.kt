package com.garam.whenwheremeet.platform

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
import com.garam.whenwheremeet.domain.usecase.ExtractRoomCodeUseCase

@Composable
actual fun PlatformDeepLinkEffect(onRoomCode: (String) -> Unit) {
    val activity = LocalContext.current.findComponentActivity()
    val currentCallback = rememberUpdatedState(onRoomCode)
    val extractRoomCode = ExtractRoomCodeUseCase()

    LaunchedEffect(activity) {
        activity?.intent?.dataString
            ?.let(extractRoomCode::invoke)
            ?.let(currentCallback.value)
    }

    DisposableEffect(activity) {
        if (activity == null) return@DisposableEffect onDispose { }
        val listener = Consumer<Intent> { intent ->
            intent.dataString
                ?.let(extractRoomCode::invoke)
                ?.let(currentCallback.value)
        }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
