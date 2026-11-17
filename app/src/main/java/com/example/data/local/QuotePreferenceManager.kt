package com.example.data.local

import android.content.Context

object QuotePreferenceManager {
    private const val PREFS_NAME = "plan_karo_quote_prefs"
    private const val KEY_LAST_QUOTE_ID = "key_last_banner_quote_id"

    fun getSavedQuoteId(context: Context): Int? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getInt(KEY_LAST_QUOTE_ID, -1)
        return if (id > 0) id else null
    }

    fun saveQuoteId(context: Context, quoteId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_QUOTE_ID, quoteId).commit()
    }
}
