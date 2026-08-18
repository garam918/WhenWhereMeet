package com.garam.whenwheremeet.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun NotificationPermissionDialog(
    onAllow: () -> Unit,
    onLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("약속 알림을 받아보세요") },
        text = {
            Text("약속이 최종 확정되거나 약속 당일이 되면 알려드릴게요. 알림 권한을 허용해주세요.")
        },
        confirmButton = {
            TextButton(onClick = onAllow) {
                Text("알림 켜기", color = WwmIndigo)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text("나중에", color = WwmMuted)
            }
        },
    )
}
