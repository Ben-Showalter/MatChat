package org.matchat.feature.timeline

/**
 * Whether the timeline keeps following the newest message after a scroll
 * (UX-SPEC S9): it follows while the list is at the bottom, stops when the
 * user scrolls up, and follows again once they scroll back down to the end.
 * A zero-distance callback ([dy] == 0) is RecyclerView reporting a layout
 * pass — new rows, or a row growing when its image loads — not the user
 * moving, so it keeps the current choice. Pure, so it's unit-tested.
 */
internal fun followAfterScroll(following: Boolean, dy: Int, canScrollDown: Boolean): Boolean =
    if (dy == 0) following else !canScrollDown
