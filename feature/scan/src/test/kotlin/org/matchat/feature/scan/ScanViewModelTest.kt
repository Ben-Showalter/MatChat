package org.matchat.feature.scan

import androidx.lifecycle.SavedStateHandle
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
import org.matchat.core.model.setup.ScanKind

/** S31: a code of the asked-for kind closes the screen; anything else is
 *  explained and scanning goes on. */
class ScanViewModelTest {

    private val key = "EsTj 3yST y93F SLpB jJsz eAXc 2XzA ygD3 w69H fGaN TKBj jXEd"

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun subject(kind: ScanKind) = ScanViewModel(SavedStateHandle(mapOf("kind" to kind.name)))

    @Test
    fun `a setup code is handed back as scanned`() = runTest {
        val vm = subject(ScanKind.SETUP_CODE)
        vm.navEvents.test {
            vm.onAction(ScanAction.Decoded("matchat:add?v=1&user=sam"))
            assertEquals(ScanNav.Accepted("matchat:add?v=1&user=sam"), awaitItem())
        }
    }

    @Test
    fun `a recovery key or an address is handed back as the value itself`() = runTest {
        val keyVm = subject(ScanKind.RECOVERY_KEY)
        keyVm.navEvents.test {
            keyVm.onAction(ScanAction.Decoded(key))
            assertEquals(ScanNav.Accepted(key), awaitItem())
        }
        val addressVm = subject(ScanKind.ADDRESS)
        addressVm.navEvents.test {
            addressVm.onAction(ScanAction.Decoded("https://matrix.to/#/@sam:example.org"))
            assertEquals(ScanNav.Accepted("@sam:example.org"), awaitItem())
        }
    }

    @Test
    fun `the wrong kind of code is explained and scanning goes on`() {
        val vm = subject(ScanKind.RECOVERY_KEY)
        vm.onAction(ScanAction.CameraStarted)
        vm.onAction(ScanAction.Decoded("https://example.org"))
        assertEquals(ScanStatus.NOT_A_RECOVERY_KEY, vm.state.value.status)

        val setup = subject(ScanKind.SETUP_CODE)
        setup.onAction(ScanAction.Decoded("busycall:add?v=1&user=1"))
        assertEquals(ScanStatus.NOT_A_SETUP_CODE, setup.state.value.status)

        val address = subject(ScanKind.ADDRESS)
        address.onAction(ScanAction.Decoded("hello"))
        assertEquals(ScanStatus.NOT_AN_ADDRESS, address.state.value.status)
    }

    @Test
    fun `a code from a newer MatChat asks for an update`() {
        val vm = subject(ScanKind.SETUP_CODE)
        vm.onAction(ScanAction.Decoded("matchat:add?v=2&user=sam"))
        assertEquals(ScanStatus.TOO_NEW, vm.state.value.status)
    }

    @Test
    fun `the same wrong code doesn't re-announce until Scan again`() {
        val vm = subject(ScanKind.SETUP_CODE)
        vm.onAction(ScanAction.Decoded("hello"))
        vm.onAction(ScanAction.ScanAgain)
        assertEquals(ScanStatus.POINT, vm.state.value.status)
        vm.onAction(ScanAction.Decoded("hello"))
        assertEquals(ScanStatus.NOT_A_SETUP_CODE, vm.state.value.status)
    }

    @Test
    fun `no camera or no permission offers Try again, which restarts the camera`() = runTest {
        val vm = subject(ScanKind.SETUP_CODE)
        vm.onAction(ScanAction.PermissionDenied)
        assertEquals(true, vm.state.value.canRetry)
        vm.navEvents.test {
            vm.onAction(ScanAction.ScanAgain)
            assertEquals(ScanNav.RestartCamera, awaitItem())
        }
        assertEquals(ScanStatus.STARTING, vm.state.value.status)
        vm.onAction(ScanAction.CameraFailed)
        assertEquals(ScanStatus.NO_CAMERA, vm.state.value.status)
    }
}
