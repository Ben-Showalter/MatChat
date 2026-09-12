package org.matchat.core.matrix

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.matchat.core.matrix.internal.RustMatrixAuth
import org.matchat.core.matrix.internal.RustMatrixSession
import org.matchat.core.matrix.internal.RustSessionVerification
import org.matchat.core.matrix.internal.SessionFileStore
import org.matchat.core.matrix.internal.SharedPreferencesDraftStore
import org.matchat.core.model.SyncStateSource

/**
 * Binds the Matrix contract to the SDK-backed implementations (M1). Only the
 * bindings changed from M0 — every caller is unaffected, because the interfaces
 * in [MatrixSession]/[MatrixAuth]/[MatrixSessionStore] are unchanged.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class MatrixModule {
    @Binds
    abstract fun bindSession(impl: RustMatrixSession): MatrixSession

    /** Online indicator round: lets :core:ui's SoftkeyFragment observe live
     *  sync state without depending on :core:matrix — same concrete session,
     *  bound to the narrower :core:model-level contract too. */
    @Binds
    abstract fun bindSyncStateSource(impl: RustMatrixSession): SyncStateSource

    @Binds
    abstract fun bindAuth(impl: RustMatrixAuth): MatrixAuth

    @Binds
    abstract fun bindStore(impl: SessionFileStore): MatrixSessionStore

    @Binds
    abstract fun bindDraftStore(impl: SharedPreferencesDraftStore): DraftStore

    @Binds
    abstract fun bindVerification(impl: RustSessionVerification): SessionVerification
}
