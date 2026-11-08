package org.matchat.feature.onboarding

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.matchat.core.model.ErrorText
import org.matchat.core.policy.Policy
import org.matchat.core.testing.FakeMatrixAuth
import org.matchat.core.testing.FakeMatrixSession
import org.matchat.core.testing.FakePolicyProvider

class SignInViewModelTest {

    @BeforeEach fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `pinned homeserver is read-only`() {
        val vm = SignInViewModel(
            FakeMatrixAuth(),
            FakeMatrixSession(),
            FakePolicyProvider(Policy.UNMANAGED.copy(pinnedHomeserver = "srv.example")),
            PendingSetupCode(),
        )
        assertEquals("srv.example", vm.state.value.homeserver)
        assertTrue(vm.state.value.homeserverPinned)
    }

    @Test
    fun `empty credentials produce an error, no network call`() {
        val auth = FakeMatrixAuth()
        val vm = SignInViewModel(auth, FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        vm.onAction(SignInAction.Submit("", "", "srv.example"))
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun `successful sign in emits Success and passes the homeserver`() = runTest {
        val auth = FakeMatrixAuth(signInResult = Result.success(Unit))
        val vm = SignInViewModel(auth, FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        vm.navEvents.test {
            vm.onAction(SignInAction.Submit("wayne", "hunter2", "srv.example"))
            assertEquals(SignInNav.Success, awaitItem())
            assertEquals("srv.example", auth.lastHomeserver)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle flips showPassword and is reversible`() {
        val vm = SignInViewModel(FakeMatrixAuth(), FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        assertEquals(false, vm.state.value.showPassword)
        vm.onAction(SignInAction.ToggleShowPassword)
        assertTrue(vm.state.value.showPassword)
        vm.onAction(SignInAction.ToggleShowPassword)
        assertEquals(false, vm.state.value.showPassword)
    }

    @Test
    fun `network failure maps to a retryable network error`() = runTest {
        val auth = FakeMatrixAuth(signInResult = Result.failure(java.io.IOException("down")))
        val vm = SignInViewModel(auth, FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        vm.onAction(SignInAction.Submit("wayne", "hunter2", "srv.example"))
        // Let the launched coroutine run.
        testScheduler.advanceUntilIdle()
        assertEquals(ErrorText.Key.NETWORK, vm.state.value.error?.key)
    }

    @Test
    fun `storage refused by the phone is explained, not shown raw`() = runTest {
        val refused = "Failed to build the client: Permission denied (os error 13)"
        val auth = FakeMatrixAuth(signInResult = Result.failure(IllegalStateException(refused)))
        val vm = SignInViewModel(auth, FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        vm.onAction(SignInAction.Submit("wayne", "hunter2", "srv.example"))
        testScheduler.advanceUntilIdle()
        assertEquals(ErrorText.Key.STORAGE_UNAVAILABLE, vm.state.value.error?.key)
    }

    @Test
    fun `a rejected password is still a bad password`() = runTest {
        val auth = FakeMatrixAuth(signInResult = Result.failure(IllegalStateException("M_FORBIDDEN: Invalid password")))
        val vm = SignInViewModel(auth, FakeMatrixSession(), FakePolicyProvider(), PendingSetupCode())
        vm.onAction(SignInAction.Submit("wayne", "wrong", "srv.example"))
        testScheduler.advanceUntilIdle()
        assertEquals(ErrorText.Key.BAD_CREDENTIALS, vm.state.value.error?.key)
    }
}
