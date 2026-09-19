package com.fastiptv.ui.settings

import com.fastiptv.data.api.XtreamApi
import com.fastiptv.data.model.AuthResponseDto
import com.fastiptv.data.model.UserInfoDto
import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.ServerConfig
import com.fastiptv.domain.repository.IptvRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var api: XtreamApi
    private lateinit var repository: IptvRepository
    private lateinit var syncManager: com.fastiptv.data.sync.SyncManager
    private lateinit var otaUpdateManager: com.fastiptv.ota.OtaUpdateManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionManager = mockk(relaxed = true)
        api = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        otaUpdateManager = mockk(relaxed = true)

        every { sessionManager.serverConfigFlow } returns flowOf(
            ServerConfig("iptv.provider.com", 80, "demo", "demo")
        )

        viewModel = SettingsViewModel(
            sessionManager = sessionManager,
            api = api,
            repository = repository,
            syncManager = syncManager,
            otaUpdateManager = otaUpdateManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSaveConfigSavesSessionAndTriggersAuthAndSync() = runTest(testDispatcher) {
        val authSuccess = AuthResponseDto(
            userInfo = UserInfoDto(
                username = "demo",
                password = "demo",
                auth = 1,
                status = "Active"
            )
        )
        coEvery { api.authenticate() } returns authSuccess

        viewModel.saveConfig("http://iptv.provider.com/", "8080", "demo", "demo123")
        advanceUntilIdle()

        coVerify { sessionManager.saveServerConfig(match { it.host == "iptv.provider.com" && it.port == 8080 }) }
        coVerify { api.authenticate() }
        verify { syncManager.startFullSync(force = true) }
        assertTrue(viewModel.statusMessage.value?.contains("Connected") == true)
    }

    @Test
    fun testSaveConfigRejectsBlankFields() = runTest(testDispatcher) {
        viewModel.saveConfig("", "80", "", "")
        advanceUntilIdle()

        assertTrue(viewModel.statusMessage.value?.contains("Error") == true)
        coVerify(exactly = 0) { sessionManager.saveServerConfig(any()) }
    }

    @Test
    fun testClearSessionDelegatesToSessionManager() = runTest(testDispatcher) {
        viewModel.clearSession()
        advanceUntilIdle()

        coVerify { sessionManager.clearSession() }
        assertNotNull(viewModel.statusMessage.value)
    }

    @Test
    fun testSetPreferredStreamFormatSavesPreference() = runTest(testDispatcher) {
        viewModel.setPreferredStreamFormat("m3u8")
        advanceUntilIdle()

        coVerify { sessionManager.savePreferredStreamFormat("m3u8") }
        assertTrue(viewModel.statusMessage.value?.contains("M3U8") == true)
    }
}
