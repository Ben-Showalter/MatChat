package org.matchat.feature.onboarding

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands a setup code from S4 to S3 (UX-SPEC S4/S32) in memory only — never a
 * nav argument, which Android may write to disk with the saved state. S3 takes
 * it once and it's gone. Never logged (it can hold a password).
 */
@Singleton
class PendingSetupCode @Inject constructor() {
    private var text: String? = null

    fun put(code: String) {
        text = code
    }

    fun take(): String? = text.also { text = null }
}
