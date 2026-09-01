package com.garam.whenwheremeet.platform

import com.tweener.alarmee.PushNotificationService
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Clock

internal class AlarmeeMeetingNotificationPlatform(
    private val pushService: PushNotificationService,
    private val platformName: String,
    private val requestSystemPermission: ((Boolean) -> Unit) -> Unit,
    private val allowDeferredTokenRegistration: Boolean = false,
    private val deviceStore: NotificationDeviceStore = FirestoreNotificationDeviceStore(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val installationIdRetryDelaysMillis: List<Long> = DEFAULT_INSTALLATION_ID_RETRY_DELAYS_MILLIS,
    private val deferredTokenRetryDelaysMillis: List<Long> = DEFAULT_TOKEN_RETRY_DELAYS_MILLIS,
    private val deviceSaveRetryDelaysMillis: List<Long> = DEFAULT_SAVE_RETRY_DELAYS_MILLIS,
) : MeetingNotificationPlatform {
    override val isSupported: Boolean = true

    private var currentUserId: String? = null
    private var currentInstallationId: String? = null
    private var currentLoginAt: String? = null
    private var isTokenCallbackRegistered = false
    private var deferredRegistrationJob: Job? = null

    override fun requestPermission(onResult: (Boolean) -> Unit) {
        requestSystemPermission(onResult)
    }

    override suspend fun registerDevice(userId: String): Result<Unit> {
        return try {
            val installationId = getInstallationIdWithRetry()
            val loginAt = Clock.System.now().toString()
            currentUserId = userId
            currentInstallationId = installationId
            currentLoginAt = loginAt

            if (!isTokenCallbackRegistered) {
                pushService.onNewToken { refreshedToken ->
                    val registeredUserId = currentUserId ?: return@onNewToken
                    val registeredInstallationId = currentInstallationId ?: return@onNewToken
                    val registeredLoginAt = currentLoginAt ?: return@onNewToken
                    if (refreshedToken.isBlank()) return@onNewToken
                    deferredRegistrationJob?.cancel()
                    scope.launch {
                        try {
                            saveDeviceWithRetry(
                                registeredUserId,
                                registeredInstallationId,
                                refreshedToken,
                                registeredLoginAt,
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                            println("Notification device save failed after receiving a refreshed token.")
                        }
                    }
                }
                isTokenCallbackRegistered = true
            }

            val tokenResult = pushService.getToken()
            tokenResult.fold(
                onSuccess = { token ->
                    require(token.isNotBlank()) { "Firebase push token is empty." }
                    deferredRegistrationJob?.cancel()
                    saveDeviceWithRetry(userId, installationId, token, loginAt)
                },
                onFailure = { throwable ->
                    if (!allowDeferredTokenRegistration) throw throwable
                    scheduleDeferredTokenRegistration(userId, installationId, loginAt)
                },
            )
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    override suspend fun unregisterDevice(userId: String) {
        deferredRegistrationJob?.cancel()
        deferredRegistrationJob = null
        val installationId = currentInstallationId
            ?: pushService.getInstallationId().getOrNull()
        try {
            if (installationId != null) {
                deviceStore.delete(userId, installationId)
            }
        } finally {
            pushService.unregister()
            if (currentUserId == userId) {
                currentUserId = null
                currentInstallationId = null
                currentLoginAt = null
            }
        }
    }

    private fun scheduleDeferredTokenRegistration(
        userId: String,
        installationId: String,
        loginAt: String,
    ) {
        deferredRegistrationJob?.cancel()
        deferredRegistrationJob = scope.launch {
            for (retryDelayMillis in deferredTokenRetryDelaysMillis) {
                delay(retryDelayMillis)
                if (currentUserId != userId || currentInstallationId != installationId) return@launch
                val token = pushService.getToken().getOrNull()?.takeIf(String::isNotBlank) ?: continue
                val saved = try {
                    saveDeviceWithRetry(userId, installationId, token, loginAt)
                    true
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    false
                }
                if (saved) return@launch
            }
            println("Notification device registration is still waiting for a Firebase push token.")
        }
    }

    private suspend fun getInstallationIdWithRetry(): String {
        var lastFailure: Throwable? = null
        repeat(installationIdRetryDelaysMillis.size + 1) { attempt ->
            val result = pushService.getInstallationId()
            result.getOrNull()?.takeIf(String::isNotBlank)?.let { return it }
            val failure = result.exceptionOrNull()
                ?: IllegalStateException("Firebase Installation ID is empty.")
            if (failure is CancellationException) throw failure
            lastFailure = failure
            val retryDelayMillis = installationIdRetryDelaysMillis.getOrNull(attempt) ?: return@repeat
            delay(retryDelayMillis)
        }
        throw checkNotNull(lastFailure)
    }

    private suspend fun saveDeviceWithRetry(
        userId: String,
        installationId: String,
        token: String,
        lastLoginAt: String,
    ) {
        var lastFailure: Throwable? = null
        repeat(deviceSaveRetryDelaysMillis.size + 1) { attempt ->
            try {
                deviceStore.save(
                    userId = userId,
                    installationId = installationId,
                    device = NotificationDevice(
                        token = token,
                        platform = platformName,
                        lastLoginAt = lastLoginAt,
                        updatedAt = Clock.System.now().toString(),
                    ),
                )
                return
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                lastFailure = throwable
                val retryDelayMillis = deviceSaveRetryDelaysMillis.getOrNull(attempt) ?: return@repeat
                delay(retryDelayMillis)
            }
        }
        throw checkNotNull(lastFailure)
    }

    private companion object {
        val DEFAULT_INSTALLATION_ID_RETRY_DELAYS_MILLIS = listOf(500L, 1_500L)
        val DEFAULT_TOKEN_RETRY_DELAYS_MILLIS = listOf(2_000L, 5_000L, 10_000L)
        val DEFAULT_SAVE_RETRY_DELAYS_MILLIS = listOf(500L, 1_500L)
    }
}

internal interface NotificationDeviceStore {
    suspend fun save(userId: String, installationId: String, device: NotificationDevice)

    suspend fun delete(userId: String, installationId: String)
}

private class FirestoreNotificationDeviceStore(
    private val firestore: FirebaseFirestore = Firebase.firestore(Firebase.app, databaseId = FIRESTORE_DATABASE_ID),
) : NotificationDeviceStore {
    override suspend fun save(userId: String, installationId: String, device: NotificationDevice) {
        firestore.collection(USERS)
            .document(userId)
            .collection(NOTIFICATION_DEVICES)
            .document(installationId)
            .set(device.toDto())
    }

    override suspend fun delete(userId: String, installationId: String) {
        firestore.collection(USERS)
            .document(userId)
            .collection(NOTIFICATION_DEVICES)
            .document(installationId)
            .delete()
    }

    private companion object {
        const val FIRESTORE_DATABASE_ID = "default"
        const val USERS = "users"
        const val NOTIFICATION_DEVICES = "notificationDevices"
    }
}

internal data class NotificationDevice(
    val token: String,
    val platform: String,
    val lastLoginAt: String,
    val updatedAt: String,
)

@Serializable
private data class NotificationDeviceDto(
    val token: String,
    val platform: String,
    val lastLoginAt: String,
    val updatedAt: String,
)

private fun NotificationDevice.toDto() = NotificationDeviceDto(
    token = token,
    platform = platform,
    lastLoginAt = lastLoginAt,
    updatedAt = updatedAt,
)
