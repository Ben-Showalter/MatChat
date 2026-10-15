package org.matchat.core.matrix.internal

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LatestOnlyRunnerTest {

    @Test
    fun `the last result always reflects the latest input`() = runTest {
        var input = 0
        var published = -1
        val runner = LatestOnlyRunner(backgroundScope) {
            val snapshot = input
            delay(if (snapshot == 1) 1_000L else 10L) // the older run is the slow one
            published = snapshot
        }
        input = 1
        runner.request()
        runCurrent() // the slow run starts and is still in progress
        input = 2
        runner.request()
        advanceUntilIdle()
        assertEquals(2, published)
    }

    @Test
    fun `requests during a run collapse into one more run`() = runTest {
        var runs = 0
        val runner = LatestOnlyRunner(backgroundScope) {
            runs++
            delay(100L)
        }
        runner.request()
        runCurrent() // first run in progress
        runner.request()
        runner.request()
        advanceUntilIdle()
        assertEquals(2, runs)
    }
}
