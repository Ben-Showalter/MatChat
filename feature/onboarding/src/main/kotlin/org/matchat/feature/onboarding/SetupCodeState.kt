package org.matchat.feature.onboarding

/** S4 "Sign in with setup code" (UX-SPEC §S4): [problem] is the line under the
 *  rows after a paste that wasn't a setup code. */
data class SetupCodeState(
    val problem: SetupProblem? = null,
)
