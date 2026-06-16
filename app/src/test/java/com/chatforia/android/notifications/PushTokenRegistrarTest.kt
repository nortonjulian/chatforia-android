package com.chatforia.android.notifications

import com.chatforia.android.crypto.DeviceIdentityStore
import com.chatforia.android.crypto.DeviceRegisterRequest
import com.chatforia.android.crypto.LinkedDeviceDto
import com.chatforia.android.crypto.LinkedDevicesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushTokenRegistrarTest {

    @Test
    fun registerCurrentFcmToken_registersDeviceAndPushToken() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            val linkedDevices = FakeLinkedDevicesDataSource()
            val fcmTokenProvider = FakeFcmTokenProvider()

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(1, identityStore.deviceIdCallCount)
            assertEquals(1, identityStore.keyPairCallCount)

            val deviceRequest = linkedDevices.registerCurrentDeviceRequests.single()

            assertEquals("device-123", deviceRequest.deviceId)
            assertEquals("Android Device", deviceRequest.name)
            assertEquals("Android", deviceRequest.platform)
            assertEquals("public-key-123", deviceRequest.publicKey)
            assertEquals("curve25519", deviceRequest.keyAlgorithm)
            assertEquals(1, deviceRequest.keyVersion)

            assertEquals(1, fcmTokenProvider.currentTokenCallCount)

            assertEquals(
                listOf("device-123" to "fcm-token-123"),
                linkedDevices.pushTokenRequests
            )
        }

    @Test
    fun registerCurrentFcmToken_usesSameDeviceIdForDeviceAndPushToken() =
        runTest {
            val identityStore =
                FakeDeviceIdentityStore(
                    deviceId = "same-device-id",
                    publicKey = "same-public-key"
                )

            val linkedDevices = FakeLinkedDevicesDataSource()
            val fcmTokenProvider =
                FakeFcmTokenProvider(
                    token = "same-fcm-token"
                )

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(
                "same-device-id",
                linkedDevices.registerCurrentDeviceRequests.single().deviceId
            )

            assertEquals(
                listOf("same-device-id" to "same-fcm-token"),
                linkedDevices.pushTokenRequests
            )
        }

    @Test
    fun registerCurrentFcmToken_doesNotCrashWhenDeviceIdFails() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            identityStore.throwOnDeviceId = true

            val linkedDevices = FakeLinkedDevicesDataSource()
            val fcmTokenProvider = FakeFcmTokenProvider()

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertTrue(linkedDevices.registerCurrentDeviceRequests.isEmpty())
            assertTrue(linkedDevices.pushTokenRequests.isEmpty())
            assertEquals(0, fcmTokenProvider.currentTokenCallCount)
        }

    @Test
    fun registerCurrentFcmToken_doesNotCrashWhenKeyPairFails() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            identityStore.throwOnKeyPair = true

            val linkedDevices = FakeLinkedDevicesDataSource()
            val fcmTokenProvider = FakeFcmTokenProvider()

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(1, identityStore.deviceIdCallCount)
            assertEquals(1, identityStore.keyPairCallCount)
            assertTrue(linkedDevices.registerCurrentDeviceRequests.isEmpty())
            assertTrue(linkedDevices.pushTokenRequests.isEmpty())
            assertEquals(0, fcmTokenProvider.currentTokenCallCount)
        }

    @Test
    fun registerCurrentFcmToken_doesNotFetchFcmTokenWhenDeviceRegistrationFails() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            val linkedDevices = FakeLinkedDevicesDataSource()
            linkedDevices.throwOnRegisterCurrentDevice = true

            val fcmTokenProvider = FakeFcmTokenProvider()

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(1, linkedDevices.registerCurrentDeviceRequests.size)
            assertTrue(linkedDevices.pushTokenRequests.isEmpty())
            assertEquals(0, fcmTokenProvider.currentTokenCallCount)
        }

    @Test
    fun registerCurrentFcmToken_doesNotRegisterPushTokenWhenFcmTokenFails() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            val linkedDevices = FakeLinkedDevicesDataSource()

            val fcmTokenProvider = FakeFcmTokenProvider()
            fcmTokenProvider.throwOnCurrentToken = true

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(1, linkedDevices.registerCurrentDeviceRequests.size)
            assertEquals(1, fcmTokenProvider.currentTokenCallCount)
            assertTrue(linkedDevices.pushTokenRequests.isEmpty())
        }

    @Test
    fun registerCurrentFcmToken_doesNotCrashWhenPushTokenRegistrationFails() =
        runTest {
            val identityStore = FakeDeviceIdentityStore()
            val linkedDevices = FakeLinkedDevicesDataSource()
            linkedDevices.throwOnRegisterPushToken = true

            val fcmTokenProvider = FakeFcmTokenProvider()

            val registrar =
                PushTokenRegistrar(
                    deviceIdentityStorage = identityStore,
                    linkedDevicesRepository = linkedDevices,
                    fcmTokenProvider = fcmTokenProvider
                )

            registrar.registerCurrentFcmToken()

            assertEquals(1, linkedDevices.registerCurrentDeviceRequests.size)
            assertEquals(1, fcmTokenProvider.currentTokenCallCount)

            assertEquals(
                listOf("device-123" to "fcm-token-123"),
                linkedDevices.pushTokenRequests
            )
        }

    private class FakeDeviceIdentityStore(
        private val deviceId: String = "device-123",
        private val publicKey: String = "public-key-123",
        private val privateKey: String = "private-key-123"
    ) : DeviceIdentityStore {
        var deviceIdCallCount = 0
        var keyPairCallCount = 0

        var throwOnDeviceId = false
        var throwOnKeyPair = false

        override fun getOrCreateDeviceId(): String {
            deviceIdCallCount++

            if (throwOnDeviceId) {
                throw Exception("Device ID boom")
            }

            return deviceId
        }

        override fun getOrCreateKeyPair(): Pair<String, String> {
            keyPairCallCount++

            if (throwOnKeyPair) {
                throw Exception("Key pair boom")
            }

            return publicKey to privateKey
        }
    }

    private class FakeFcmTokenProvider(
        private val token: String = "fcm-token-123"
    ) : FcmTokenProvider {
        var currentTokenCallCount = 0
        var throwOnCurrentToken = false

        override suspend fun currentToken(): String {
            currentTokenCallCount++

            if (throwOnCurrentToken) {
                throw Exception("FCM boom")
            }

            return token
        }
    }

    private class FakeLinkedDevicesDataSource : LinkedDevicesDataSource {
        val registerCurrentDeviceRequests = mutableListOf<DeviceRegisterRequest>()
        val pushTokenRequests = mutableListOf<Pair<String, String>>()

        var throwOnRegisterCurrentDevice = false
        var throwOnRegisterPushToken = false

        override fun fetchMine(): List<LinkedDeviceDto> {
            return emptyList()
        }

        override fun fetchPendingPairing(): List<LinkedDeviceDto> {
            return emptyList()
        }

        override fun registerCurrentDevice(
            request: DeviceRegisterRequest
        ) {
            registerCurrentDeviceRequests.add(request)

            if (throwOnRegisterCurrentDevice) {
                throw Exception("Register device boom")
            }
        }

        override fun approve(
            deviceId: String,
            wrappedAccountKey: String
        ) = Unit

        override fun reject(
            deviceId: String
        ) = Unit

        override fun revoke(
            deviceId: String
        ) = Unit

        override fun heartbeat(
            deviceId: String
        ) = Unit

        override fun requestPairing(
            request: DeviceRegisterRequest
        ) = Unit

        override fun fetchPairingStatus(
            deviceId: String
        ): LinkedDeviceDto? {
            return null
        }

        override fun registerPushToken(
            deviceId: String,
            pushToken: String
        ) {
            pushTokenRequests.add(deviceId to pushToken)

            if (throwOnRegisterPushToken) {
                throw Exception("Register push token boom")
            }
        }
    }
}