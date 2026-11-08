package org.matchat.feature.onboarding

sealed interface WelcomeAction {
    data object SignInWithSetupCode : WelcomeAction
    data object SignInWithPassword : WelcomeAction
    data object Help : WelcomeAction
}

/** One-shot navigation out of Welcome. */
sealed interface WelcomeNav {
    data object Password : WelcomeNav
    data object SetupCode : WelcomeNav
    data object Help : WelcomeNav
}
