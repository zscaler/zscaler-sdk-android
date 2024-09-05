package com.zscaler.sdk.demoapp.repository

import android.content.Context

/**
 * Created by Anurag Goel on 04/12/24.
 *
 */
class SharedPrefsUserRepository(private val context: Context) : UserRepository {

    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val UDID_KEY = "udid"

    override fun saveUdid(udid: String) {
        sharedPreferences.edit().putString(UDID_KEY, udid).apply()
    }

    override fun getUdid(): String {
        return sharedPreferences.getString(UDID_KEY, "") ?: ""
    }
}