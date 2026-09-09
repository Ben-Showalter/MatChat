package org.matchat.client.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.matchat.core.ui.prefs.UserPreferences

/**
 * Lets [org.matchat.client.MainActivity] read the current theme preference
 * before Hilt's normal `@AndroidEntryPoint` field injection runs. Field
 * injection happens inside `super.onCreate()` — too late to call
 * `setTheme()` before it, which AppCompat requires for the theme to apply
 * to the decor view.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserPreferencesEntryPoint {
    fun userPreferences(): UserPreferences
}
