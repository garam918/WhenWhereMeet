package com.garam.whenwheremeet.platform

import com.tweener.alarmee.PushNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmeeMeetingNotificationPlatformTest {
    @Test
    fun deferredRegistrationWaitsForTokenCallback() = runTest {
        val pushService = FakePushNotificationService(
            tokenResults = listOf(Result.failure(IllegalStateException("token not ready"))),
        )
        val deviceStore = FakeNotificationDeviceStore()
        val platform = createPlatform(
            pushService = pushService,
            deviceStore = deviceStore,
            scope = this,
            deferredTokenRetryDelaysMillis = emptyList(),
        )

        val result = platform.registerDevice(USER_ID)
        pushService.emitNewToken("callback-token")
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(listOf("callback-token"), deviceStore.savedDevices.map { it.token })
    }

    @Test
    fun deferredRegistrationRetriesUntilTokenIsAvailable() = runTest {
        val pushService = FakePushNotificationService(
            tokenResults = listOf(
                Result.failure(IllegalStateException("initial token not ready")),
                Result.failure(IllegalStateException("retry token not ready")),
                Result.success("retried-token"),
            ),
        )
        val deviceStore = FakeNotificationDeviceStore()
        val platform = createPlatform(
            pushService = pushService,
            deviceStore = deviceStore,
            scope = this,
            deferredTokenRetryDelaysMillis = listOf(1L, 1L),
        )

        val result = platform.registerDevice(USER_ID)
        advanceUntilIdle()

        assertTrue(result.isSuccess)
        assertEquals(3, pushService.getTokenCallCount)
        assertEquals(listOf("retried-token"), deviceStore.savedDevices.map { it.token })
    }

    @Test
    fun deviceSaveRetriesTransientFailures() = runTest {
        val pushService = FakePushNotificationService(
            tokenResults = listOf(Result.success("token")),
        )
        val deviceStore = FakeNotificationDeviceStore(failuresBeforeSuccess = 2)
        val platform = createPlatform(
            pushService = pushService,
            deviceStore = deviceStore,
            scope = this,
            deviceSaveRetryDelaysMillis = listOf(1L, 1L),
        )

        val result = platform.registerDevice(USER_ID)

        assertTrue(result.isSuccess)
        assertEquals(3, deviceStore.saveCallCount)
        assertEquals(listOf("token"), deviceStore.savedDevices.map { it.token })
    }

    @Test
    fun installationIdRetriesTransientFailures() = runTest {
        val pushService = FakePushNotificationService(
            installationIdResults = listOf(
                Result.failure(IllegalStateException("installation unavailable")),
                Result.failure(IllegalStateException("installation still unavailable")),
                Result.success("installation-id"),
            ),
            tokenResults = listOf(Result.success("token")),
        )
        val platform = createPlatform(
            pushService = pushService,
            deviceStore = FakeNotificationDeviceStore(),
            scope = this,
            installationIdRetryDelaysMillis = listOf(1L, 1L),
        )

        val result = platform.registerDevice(USER_ID)

        assertTrue(result.isSuccess)
        assertEquals(3, pushService.getInstallationIdCallCount)
    }

    @Test
    fun immediateTokenFailureStillFailsWhenDeferredRegistrationIsDisabled() = runTest {
        val pushService = FakePushNotificationService(
            tokenResults = listOf(Result.failure(IllegalStateException("token unavailable"))),
        )
        val platform = AlarmeeMeetingNotificationPlatform(
            pushService = pushService,
            platformName = "android",
            requestSystemPermission = { it(true) },
            allowDeferredTokenRegistration = false,
            deviceStore = FakeNotificationDeviceStore(),
            scope = this,
            installationIdRetryDelaysMillis = emptyList(),
            deferredTokenRetryDelaysMillis = emptyList(),
            deviceSaveRetryDelaysMillis = emptyList(),
        )

        assertTrue(platform.registerDevice(USER_ID).isFailure)
    }

    private fun createPlatform(
        pushService: PushNotificationService,
        deviceStore: NotificationDeviceStore,
        scope: CoroutineScope,
        installationIdRetryDelaysMillis: List<Long> = emptyList(),
        deferredTokenRetryDelaysMillis: List<Long> = emptyList(),
        deviceSaveRetryDelaysMillis: List<Long> = emptyList(),
    ) = AlarmeeMeetingNotificationPlatform(
        pushService = pushService,
        platformName = "ios",
        requestSystemPermission = { it(true) },
        allowDeferredTokenRegistration = true,
        deviceStore = deviceStore,
        scope = scope,
        installationIdRetryDelaysMillis = installationIdRetryDelaysMillis,
        deferredTokenRetryDelaysMillis = deferredTokenRetryDelaysMillis,
        deviceSaveRetryDelaysMillis = deviceSaveRetryDelaysMillis,
    )

    private companion object {
        const val USER_ID = "test-user"
    }
}

private class FakePushNotificationService(
    installationIdResults: List<Result<String>> = listOf(Result.success("installation-id")),
    tokenResults: List<Result<String>>,
) : PushNotificationService {
    private val pendingInstallationIdResults = ArrayDeque(installationIdResults)
    private val pendingTokenResults = ArrayDeque(tokenResults)
    private var tokenCallback: ((String) -> Unit)? = null

    var getTokenCallCount: Int = 0
        private set
    var getInstallationIdCallCount: Int = 0
        private set

    override fun unregister() = Unit

    override fun handleIncomingMessage(data: Map<String, String>) = Unit

    override suspend fun getInstallationId(): Result<String> {
        getInstallationIdCallCount += 1
        return pendingInstallationIdResults.removeFirstOrNull()
            ?: Result.failure(IllegalStateException("No fake installation ID result configured"))
    }

    override suspend fun getToken(): Result<String> {
        getTokenCallCount += 1
        return pendingTokenResults.removeFirstOrNull()
            ?: Result.failure(IllegalStateException("No fake token result configured"))
    }

    override suspend fun onNewToken(callback: (String) -> Unit) {
        tokenCallback = callback
    }

    override suspend fun forceTokenRefresh(): Result<String> = getToken()

    override suspend fun onPushMessageReceived(callback: (Map<String, String>) -> Unit) = Unit

    fun emitNewToken(token: String) {
        tokenCallback?.invoke(token)
    }
}

private class FakeNotificationDeviceStore(
    private var failuresBeforeSuccess: Int = 0,
) : NotificationDeviceStore {
    val savedDevices = mutableListOf<NotificationDevice>()
    var saveCallCount: Int = 0
        private set

    override suspend fun save(userId: String, installationId: String, device: NotificationDevice) {
        saveCallCount += 1
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess -= 1
            throw IllegalStateException("temporary save failure")
        }
        savedDevices += device
    }

    override suspend fun delete(userId: String, installationId: String) = Unit
}
