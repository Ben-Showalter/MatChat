package org.matchat.feature.onboarding

/**
 * S2 Welcome. [qrEnabled] shows "Sign in with setup code" (S4, a QR code or
 * link holding the sign-in details — docs/adr/0010). Matrix's own
 * device-linking QR login (MSC4108) is still a later option (PLAN.md §7).
 */
data class WelcomeState(
    val qrEnabled: Boolean = false,
)
