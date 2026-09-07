package org.matchat.core.rtc

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.matchat.core.rtc.internal.MatrixRtcCallController
import org.matchat.core.rtc.internal.StubAudioTransport
import org.matchat.core.rtc.internal.StubTokenService
import javax.inject.Singleton

/**
 * Wires the call stack. The audio transport + token service are the stub
 * implementations until LiveKit + lk-jwt-service are deployed (docs/VOICE.md §7);
 * swapping them for real impls is a one-line change here. [RtcConfig] is provided
 * by :app (blank by default), so this module stays free of app policy.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RtcModule {

    @Binds
    @Singleton
    abstract fun bindCallController(impl: MatrixRtcCallController): CallController

    @Binds
    @Singleton
    abstract fun bindAudioTransport(impl: StubAudioTransport): AudioTransport

    @Binds
    @Singleton
    abstract fun bindTokenService(impl: StubTokenService): TokenService
}
