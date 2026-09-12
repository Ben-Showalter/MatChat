package org.matchat.core.model

import kotlinx.coroutines.flow.Flow

/**
 * A live source of [SyncState] (Online indicator round). This lives in
 * :core:model — not :core:matrix, where the real implementation is — purely
 * so :core:ui's SoftkeyFragment can observe it without depending on
 * :core:matrix (AGENTS.md §2: :core:ui only depends on :core:model).
 * [org.matchat.core.matrix.MatrixSession] extends this; MatrixModule binds
 * the same concrete session both ways.
 */
interface SyncStateSource {
    val syncState: Flow<SyncState>
}
