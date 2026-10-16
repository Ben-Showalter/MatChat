package org.matchat.core.matrix.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * These drive the runner deterministically: each run parks on [release] until the
 * test lets it finish, and [advanceUntilIdle] settles the scheduler between steps.
 * The earlier version raced two `delay`s and relied on `runCurrent()` landing a
 * backgroundScope coroutine at an exact point, which is timing-fragile across
 * machines (it failed on a dev box though the runner was correct).
 */
class LatestOnlyRunnerTest {

    @Test
    fun `the last result always reflects the latest input`() = runTest {
        val release = Channel<Unit>(Channel.UNLIMITED) // one token lets one run finish
        var input = 0
        var published = -1
        val runner = LatestOnlyRunner(backgroundScope) {
            val snapshot = input
            release.receive() // park mid-run until the test releases it
            published = snapshot
        }

        input = 1
        runner.request()
        advanceUntilIdle() // run 1 started with snapshot = 1, now parked

        input = 2
        runner.request() // coalesced follow-up, queued while run 1 is parked
        advanceUntilIdle()

        release.send(Unit) // finish run 1 -> publishes 1, then starts run 2 (snapshot = 2)
        advanceUntilIdle()
        release.send(Unit) // finish run 2 -> publishes 2
        advanceUntilIdle()

        assertEquals(2, published)
    }

    @Test
    fun `requests during a run collapse into one more run`() = runTest {
        val release = Channel<Unit>(Channel.UNLIMITED)
        var runs = 0
        val runner = LatestOnlyRunner(backgroundScope) {
            runs++
            release.receive()
        }

        runner.request()
        advanceUntilIdle()
        assertEquals(1, runs) // run 1 in progress

        runner.request()
        runner.request() // both coalesce while run 1 is parked
        advanceUntilIdle()
        assertEquals(1, runs) // no new run yet — still one in flight

        release.send(Unit) // finish run 1 -> the coalesced follow-up starts
        advanceUntilIdle()
        assertEquals(2, runs)

        release.send(Unit) // finish run 2 -> nothing queued
        advanceUntilIdle()
        assertEquals(2, runs) // the two requests collapsed into a single extra run
    }
}
