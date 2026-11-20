package fr.husi.ui

import fr.husi.bg.AppUpdateAutoChecker
import fr.husi.bg.BackendState
import fr.husi.bg.ServiceState
import fr.husi.database.DataStore
import fr.husi.test.FakeAppUpdateFetcher
import fr.husi.test.FakeCoreClient
import fr.husi.test.HusiKoinMainDispatcherTest
import fr.husi.test.appUpdateInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : HusiKoinMainDispatcherTest() {

    override suspend fun postStartKoin() {
        DataStore.configurationStore.reset()
    }

    @AfterTest
    fun resetBackendState() {
        BackendState.reset()
        DataStore.serviceState = ServiceState.Idle
    }

    /**
     * https://codeberg.org/xchacha20-poly1305/husi/issues/50
     */
    @Test
    fun `confirm should complete false when dialog is dismissed`() = runTest(dispatcher.scheduler) {
        val viewModel = newViewModel()

        val eventDeferred = backgroundScope.async {
            viewModel.dialogEvent.first()
        }
        val resultDeferred = backgroundScope.async {
            viewModel.confirm("confirm")
        }

        advanceUntilIdle()

        val event = assertIs<MainAlertDialogEvent>(eventDeferred.await())
        event.onDismiss!!.invoke()

        advanceUntilIdle()

        assertFalse(resultDeferred.await())
    }


    @Test
    fun `app update check waits for the service to connect`() = runTest(dispatcher.scheduler) {
        DataStore.appUpdateAutoCheck.set(true)
        DataStore.appUpdateOnlyWhenConnected.set(true)
        DataStore.appUpdateLastCheckEpochDay.set(0L)
        DataStore.appUpdateSkippedVersion.set("")
        val info = appUpdateInfo()
        val fetcher = FakeAppUpdateFetcher(nextResult = info)
        val viewModel = MainViewModel(
            coreClient = FakeCoreClient(),
            appUpdateChecker = AppUpdateAutoChecker(
                fetcher = fetcher,
                isSupported = true,
                today = { 100L },
                ioDispatcher = dispatcher,
            ),
            ioDispatcher = dispatcher,
        )

        advanceUntilIdle()
        assertEquals(0, fetcher.checkCount)
        assertNull(viewModel.appUpdate.value)

        BackendState.updateState(ServiceState.Connected)
        advanceUntilIdle()

        assertEquals(1, fetcher.checkCount)
        assertEquals(info.version, viewModel.appUpdate.value?.version)

        viewModel.close()
    }

    @Test
    fun `connection test uses the active service and configured URL`() = runTest(dispatcher.scheduler) {
        val client = FakeCoreClient().apply { nextUrlTestDelay = 123 }
        DataStore.connectionTestURL.set("https://test.example/")
        DataStore.connectionTestTimeout.set(4_000)
        DataStore.connectionTestUnifiedDelay.set(true)
        DataStore.connectionTestIgnoreHandshakeTime.set(true)
        DataStore.serviceState = ServiceState.Connected
        val viewModel = newViewModel(client)

        viewModel.urlTest()
        advanceUntilIdle()

        assertEquals(1, client.urlTestCalls)
        assertEquals("", client.lastUrlTestTag)
        assertEquals("https://test.example/", client.lastUrlTestLink)
        assertEquals(4_000, client.lastUrlTestTimeoutMs)
        assertEquals(123, assertIs<URLTestStatus.Success>(viewModel.urlTestStatus.value).delay)
        viewModel.close()
    }

    @Test
    fun `connection test does not contact the core while disconnected`() = runTest(dispatcher.scheduler) {
        val client = FakeCoreClient()
        DataStore.serviceState = ServiceState.Idle
        val viewModel = newViewModel(client)

        viewModel.urlTest()
        advanceUntilIdle()

        assertEquals(0, client.urlTestCalls)
        assertIs<URLTestStatus.Exception>(viewModel.urlTestStatus.value)
        viewModel.close()
    }

    private fun newViewModel(coreClient: FakeCoreClient = FakeCoreClient()) = MainViewModel(
        coreClient = coreClient,
        ioDispatcher = dispatcher,
    )
}
