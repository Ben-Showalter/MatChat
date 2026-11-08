package org.matchat.feature.scan

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** S33: every way of saving the setup page ends in a line on screen. */
class SetupHelpViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private class FakeStore(
        override val needsStoragePermission: Boolean = false,
        val downloads: String? = "Download › matchat-setup.html",
        val sd: SdCardSave = SdCardSave.NoCard,
    ) : SetupPageStore {
        override fun saveToDownloads(): String? = downloads
        override fun saveToSdCard(): SdCardSave = sd
    }

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun subject(store: FakeStore) = SetupHelpViewModel(store, dispatcher)

    @Test
    fun `saving to the phone says where it went`() {
        val vm = subject(FakeStore())
        vm.onAction(SetupHelpAction.SaveToPhone)
        assertEquals(SetupHelpMessage.Saved("Download › matchat-setup.html"), vm.state.value.message)
    }

    @Test
    fun `a failed save says so`() {
        val vm = subject(FakeStore(downloads = null))
        vm.onAction(SetupHelpAction.SaveToPhone)
        assertEquals(SetupHelpMessage.SaveFailed, vm.state.value.message)
    }

    @Test
    fun `older phones ask for storage first, and a refusal is explained`() = runTest {
        val vm = subject(FakeStore(needsStoragePermission = true))
        vm.navEvents.test {
            vm.onAction(SetupHelpAction.SaveToPhone)
            assertEquals(SetupHelpNav.RequestStoragePermission, awaitItem())
        }
        vm.onAction(SetupHelpAction.StoragePermissionResult(granted = false))
        assertEquals(SetupHelpMessage.NeedsStorage, vm.state.value.message)
        vm.onAction(SetupHelpAction.StoragePermissionResult(granted = true))
        assertEquals(SetupHelpMessage.Saved("Download › matchat-setup.html"), vm.state.value.message)
    }

    @Test
    fun `SD card and Bluetooth outcomes are shown`() {
        val noCard = subject(FakeStore(sd = SdCardSave.NoCard))
        noCard.onAction(SetupHelpAction.SaveToSdCard)
        assertEquals(SetupHelpMessage.NoSdCard, noCard.state.value.message)

        val card = subject(FakeStore(sd = SdCardSave.Saved("Android/data/x/files/matchat-setup.html")))
        card.onAction(SetupHelpAction.SaveToSdCard)
        assertEquals(SetupHelpMessage.Saved("Android/data/x/files/matchat-setup.html"), card.state.value.message)

        card.onAction(SetupHelpAction.ShareFailed)
        assertEquals(SetupHelpMessage.NoShare, card.state.value.message)
    }
}
