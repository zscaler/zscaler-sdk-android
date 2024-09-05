package com.zscaler.sdk.demoapp.repository

/**
 * Created by Anurag Goel on 04/12/24.
 *
 */
interface UserRepository {
    fun saveUdid(udid: String)
    fun getUdid(): String
}