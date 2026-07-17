package com.willykez.gitsync.data.repository

import android.content.Context

/**
 * Which signing key (if any) is "active" — same plain-SharedPreferences pattern as
 * com.willykez.gitsync.sync.SyncPrefs, for the same reason: two settings, not worth
 * DataStore's overhead.
 */
object SigningPrefs {
    private const val PREFS_NAME = "gitsync_signing_prefs"
    private const val KEY_ENABLED = "commit_signing_enabled"
    private const val KEY_ACTIVE_KEY_ID = "active_signing_key_id"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** 0 = no key selected. */
    fun activeKeyId(context: Context): Long = prefs(context).getLong(KEY_ACTIVE_KEY_ID, 0L)

    fun setActiveKeyId(context: Context, id: Long) {
        prefs(context).edit().putLong(KEY_ACTIVE_KEY_ID, id).apply()
    }
}
