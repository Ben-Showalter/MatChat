package org.matchat.feature.onboarding

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** S4 "Do you have a QR setup code ready?". */
class SetupCodeViewModelTest {

    private val code = "matchat:add?v=1&user=sam&pass=x"

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the three rows go to the scanner, sign-in and help`() = runTest {
        val pending = PendingSetupCode()
        val vm = SetupCodeViewModel(pending)
        vm.navEvents.test {
            vm.onAction(SetupCodeAction.Scan)
            assertEquals(SetupCodeNav.OpenScanner, awaitItem())
            vm.onAction(SetupCodeAction.Help)
            assertEquals(SetupCodeNav.OpenHelp, awaitItem())
            vm.onAction(SetupCodeAction.Scanned(code))
            assertEquals(SetupCodeNav.OpenSignIn, awaitItem())
        }
        assertEquals(code, pending.take())
    }

    @Test
    fun `a pasted setup link goes on to sign-in`() = runTest {
        val pending = PendingSetupCode()
        val vm = SetupCodeViewModel(pending)
        vm.navEvents.test {
            vm.onAction(SetupCodeAction.Paste(code))
            assertEquals(SetupCodeNav.OpenSignIn, awaitItem())
        }
        assertNull(vm.state.value.problem)
    }

    @Test
    fun `a bad paste is explained and nothing is passed on`() {
        val pending = PendingSetupCode()
        val vm = SetupCodeViewModel(pending)
        vm.onAction(SetupCodeAction.Paste(null))
        assertEquals(SetupProblem.NOTHING_TO_PASTE, vm.state.value.problem)
        vm.onAction(SetupCodeAction.Paste("hello"))
        assertEquals(SetupProblem.NOT_A_SETUP_CODE, vm.state.value.problem)
        vm.onAction(SetupCodeAction.Paste("matchat:add?v=2&user=sam"))
        assertEquals(SetupProblem.TOO_NEW, vm.state.value.problem)
        assertNull(pending.take())
    }
}
