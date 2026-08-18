package com.garam.whenwheremeet.platform

import com.tweener.alarmee.MobileAlarmeeService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock

internal class AlarmeeMeetingNotificationPlatform(
    private val alarmeeService: MobileAlarmeeService,
    private val platformName: String,
    private val requestSystemPermission: ((Boolean) -> Unit) -> Unit,
    private val firestore: FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId = FIRESTORE_DATABASE_ID),
) : MeetingNotificationPlatform {
    override val isSupported: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentUserId: String? = null
    private var currentInstallationId: String? = null
    private var isTokenCallbackRegistered = false

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        requestSystemPermission(onResult)
    }

    override suspend fun registerDevice(userId: String): Result<Unit> = runCatching {
        val installationId = alarmeeService.push.getInstallationId().getOrThrow()
        currentUserId = userId
        currentInstallationId = installationId

        if (!isTokenCallbackRegistered) {
            alarmeeService.push.onNewToken { refreshedToken ->
                val registeredUserId = currentUserId ?: return@onNewToken
                val registeredInstallationId = currentInstallationId ?: return@onNewToken
                scope.launch {
                    runCatching { saveDevice(registeredUserId, registeredInstallationId, refreshedToken) }
                }
            }
            isTokenCallbackRegistered = true
        }

        val token = alarmeeService.push.getToken().getOrThrow()
        saveDevice(userId, installationId, token)
    }

    override suspend fun unregisterDevice(userId: String) {
        val installationId = currentInstallationId
            ?: alarmeeService.push.getInstallationId().getOrNull()
        try {
            if (installationId != null) {
                firestore.collection(USERS)
                    .document(userId)
                    .collection(NOTIFICATION_DEVICES)
                    .document(installationId)
                    .delete()
            }
        } finally {
            alarmeeService.push.unregister()
            if (currentUserId == userId) {
                currentUserId = null
                currentInstallationId = null
            }
        }
    }

    private suspend fun saveDevice(userId: String, installationId: String, token: String) {
        firestore.collection(USERS)
            .document(userId)
            .collection(NOTIFICATION_DEVICES)
            .document(installationId)
            .set(
                NotificationDeviceDto(
                    token = token,
                    platform = platformName,
                    updatedAt = Clock.System.now().toString(),
                ),
            )
    }

    private companion object {
        const val FIRESTORE_DATABASE_ID = "default"
        const val USERS = "users"
        const val NOTIFICATION_DEVICES = "notificationDevices"
    }
}

@Serializable
private data class NotificationDeviceDto(
    val token: String,
    val platform: String,
    val updatedAt: String,
)
