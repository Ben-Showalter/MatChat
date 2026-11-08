package org.matchat.feature.onboarding

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.matchat.core.model.ErrorText
import org.matchat.core.policy.Policy
import org.matchat.core.testing.FakeMatrixAuth
import org.matchat.core.testing.FakeMatrixSession
import org.matchat.core.testing.FakePolicyProvider

/** S3/S32: a setup code is shown first (secrets hidden) and used only on
 *  "Sign in"; its recovery key unlocks history right after sign-in. */
class SignInSetupCodeTest {

    private val auth = FakeMatrixAuth()
    private val session = FakeMatrixSession()
    private val full = "matchat:add?v=1&server=matrix.example.org&user=sam&pass=hunter2&key=EsTj%203ySTy93F"

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun subject(policy: Policy = Policy.UNMANAGED, pending: PendingSetupCode = PendingSetupCode()) =
        SignInViewModel(auth, session, FakePolicyProvider(policy), pending)

    @Test
    fun `a setup code is shown, not used, with the password hidden`() {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full))
        val found = vm.state.value.found!!
        assertEquals("matrix.example.org", found.server)
        assertEquals("sam", found.user)
        assertTrue(found.hasPassword)
        assertTrue(found.hasRecoveryKey)
        assertTrue(found.canSignIn)
        assertNull(auth.lastUser) // nothing used yet
        assertFalse(found.toString().contains("hunter2"))
    }

    @Test
    fun `Sign in uses the code, then applies its recovery key`() = runTest {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full))
        vm.navEvents.test {
            vm.onAction(SignInAction.ConfirmSetupCode)
            assertEquals(SignInNav.Success, awaitItem())
        }
        assertEquals("sam", auth.lastUser)
        assertEquals("matrix.example.org", auth.lastHomeserver)
        assertEquals("EsTj 3ySTy93F", session.lastRecoveryKey)
        assertNull(vm.state.value.found)
    }

    @Test
    fun `a recovery key that doesn't work still signs in, then goes to recovery-key entry`() = runTest {
        session.recoverResult = Result.failure(IllegalStateException("bad key"))
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full))
        vm.navEvents.test {
            vm.onAction(SignInAction.ConfirmSetupCode)
            assertEquals(SignInNav.RecoveryKeyFailed, awaitItem())
        }
    }

    @Test
    fun `a code for another server than the pinned one is explained and not used`() {
        val vm = subject(Policy.UNMANAGED.copy(pinnedHomeserver = "corp.example"))
        vm.onAction(SignInAction.SetupCodeReceived(full))
        val found = vm.state.value.found!!
        assertEquals("matrix.example.org", found.blockedServer)
        assertEquals("corp.example", found.server)
        assertFalse(found.canSignIn)
        vm.onAction(SignInAction.ConfirmSetupCode)
        assertNull(auth.lastUser)
    }

    @Test
    fun `the pinned server written differently is the same server`() {
        val vm = subject(Policy.UNMANAGED.copy(pinnedHomeserver = "matrix.example.org"))
        vm.onAction(SignInAction.SetupCodeReceived(full.replace("server=matrix", "server=https://Matrix")))
        assertNull(vm.state.value.found!!.blockedServer)
    }

    @Test
    fun `an incomplete code fills the form and keeps its recovery key for after sign-in`() = runTest {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived("matchat:add?v=1&user=sam&key=EsTj%203ySTy93F"))
        assertFalse(vm.state.value.found!!.canSignIn)
        vm.onAction(SignInAction.FillFromSetupCode)
        assertEquals("sam", vm.state.value.fill?.user)
        assertTrue(vm.state.value.recoveryKeyPending)
        vm.navEvents.test {
            vm.onAction(SignInAction.Submit("sam", "typed-password", "srv.example"))
            assertEquals(SignInNav.Success, awaitItem())
        }
        assertEquals("EsTj 3ySTy93F", session.lastRecoveryKey)
    }

    @Test
    fun `cancel forgets the code`() {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full))
        vm.onAction(SignInAction.CancelSetupCode)
        assertNull(vm.state.value.found)
        assertFalse(vm.state.value.recoveryKeyPending)
    }

    @Test
    fun `other text, newer codes and an empty clipboard are explained`() {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived("https://example.org"))
        assertEquals(SetupProblem.NOT_A_SETUP_CODE, vm.state.value.setupProblem)
        vm.onAction(SignInAction.SetupCodeReceived("matchat:add?v=9&user=sam"))
        assertEquals(SetupProblem.TOO_NEW, vm.state.value.setupProblem)
        vm.onAction(SignInAction.SetupCodeReceived(null))
        assertEquals(SetupProblem.NOTHING_TO_PASTE, vm.state.value.setupProblem)
    }

    @Test
    fun `a code chosen on S4 is waiting when sign-in opens`() {
        val pending = PendingSetupCode().apply { put(full) }
        val vm = subject(pending = pending)
        assertEquals("sam", vm.state.value.found?.user)
        assertNull(pending.take()) // taken once
    }

    @Test
    fun `a failed sign-in stays on Setup code found, and Sign in retries`() = runTest {
        val refused = "Failed to build the client: Permission denied (os error 13)"
        auth.signInResult = Result.failure(IllegalStateException(refused))
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full))
        vm.onAction(SignInAction.ConfirmSetupCode)
        assertEquals("sam", vm.state.value.found?.user)
        assertEquals(ErrorText.Key.STORAGE_UNAVAILABLE, vm.state.value.error?.key)

        auth.signInResult = Result.success(Unit)
        vm.navEvents.test {
            vm.onAction(SignInAction.ConfirmSetupCode)
            assertEquals(SignInNav.Success, awaitItem())
        }
        assertEquals("EsTj 3ySTy93F", session.lastRecoveryKey) // the key survived the retry
        assertNull(vm.state.value.found)
    }

    @Test
    fun `the code's password shows one dot per character, and Show password reveals it`() {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived(full)) // pass=hunter2, 7 characters
        assertEquals("•••••••", vm.state.value.found?.password)
        vm.onAction(SignInAction.ToggleShowPassword)
        assertEquals("hunter2", vm.state.value.found?.password)
        assertFalse(vm.state.value.found.toString().contains("hunter2"))
        vm.onAction(SignInAction.ToggleShowPassword)
        assertEquals("•••••••", vm.state.value.found?.password)
    }

    @Test
    fun `a stray space at the end of the password is pointed out`() {
        val vm = subject()
        vm.onAction(SignInAction.SetupCodeReceived("matchat:add?v=1&user=sam&pass=hunter2%20"))
        assertTrue(vm.state.value.found!!.passwordHasEdgeSpace)
        assertEquals(8, vm.state.value.found!!.password!!.length)
    }
}
