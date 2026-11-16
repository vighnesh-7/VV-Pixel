package com.example

import android.content.Context

object AccessibilityMasterState {
    private const val PREFS = "com.example.vvpixel.SETTINGS"
    private const val KEY = "accessibility_master_enabled"
    private const val KEY_AUTO_DISABLE_ENABLED = "auto_disable_on_apps_enabled"
    private const val KEY_AUTO_DISABLE_PACKAGES = "auto_disable_packages"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY, enabled).apply()
    }

    fun isAutoDisableOnAppsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_DISABLE_ENABLED, true)

    fun setAutoDisableOnAppsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_DISABLE_ENABLED, enabled).apply()
    }

    fun getAutoDisablePackages(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_AUTO_DISABLE_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun setAutoDisablePackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY_AUTO_DISABLE_PACKAGES, packages).apply()
    }
}

