package com.example.fable.data.local

import android.content.Context
import java.util.UUID

class DeviceIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences("fable_device", Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreateDeviceId(): String {
        preferences.getString(KEY_DEVICE_ID, null)
            ?.let { stored -> runCatching { UUID.fromString(stored).toString() }.getOrNull() }
            ?.let { return it }

        val deviceId = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_DEVICE_ID, deviceId).commit()) {
            "Could not persist the Fable device identity."
        }
        return deviceId
    }

    private companion object {
        const val KEY_DEVICE_ID = "device_id"
    }
}
