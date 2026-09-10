package org.matchat.feature.settings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.matchat.core.testing.FakeUserPreferences

class AdvancedViewModelTest {

    private val prefs = FakeUserPreferences()

    private fun subject() = AdvancedViewModel(prefs)

    @BeforeEach fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `default state is not swapped`() = runTest {
        subject().state.test {
            assertEquals(false, expectMostRecentItem().softkeysSwapped)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggling on updates the fake and the state`() = runTest {
        val vm = subject()
        vm.onAction(AdvancedAction.ToggleSoftkeysSwapped)
        vm.state.test {
            assertEquals(true, expectMostRecentItem().softkeysSwapped)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(true, prefs.softkeysSwapped.value)
    }

    @Test
    fun `toggling twice returns to not swapped`() = runTest {
        val vm = subject()
        vm.onAction(AdvancedAction.ToggleSoftkeysSwapped)
        vm.onAction(AdvancedAction.ToggleSoftkeysSwapped)
        vm.state.test {
            assertEquals(false, expectMostRecentItem().softkeysSwapped)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
