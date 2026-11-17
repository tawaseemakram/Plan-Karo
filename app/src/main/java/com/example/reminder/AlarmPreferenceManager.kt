package com.example.reminder

import android.content.Context

object AlarmPreferenceManager {
    private const val PREFS_NAME = "plan_karo_alarm_prefs"
    private const val KEY_ALARM_ENABLED = "key_task_alarm_enabled"

    /**
     * When true: At task notification time, phone rings loudly like an alarm (sound + vibration + screen alert).
     * When false: Reminders only arrive quietly/normally in the notification bar ("alarm sirf notification bar par ayega").
     * Defaults to true so users don't miss tasks unless they choose to turn it off.
     */
    fun isAlarmEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ALARM_ENABLED, true)
    }

    fun setAlarmEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, enabled).commit()
    }
}
