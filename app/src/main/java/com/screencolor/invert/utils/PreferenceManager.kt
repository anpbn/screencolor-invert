package com.screencolor.invert.utils

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.screencolor.invert.data.AppSettings
import com.screencolor.invert.data.ColorPair

/**
 * Manager for app preferences and settings
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "screen_color_invert_prefs"
        private const val KEY_COLOR_PAIRS = "color_pairs"
        private const val KEY_SETTINGS = "app_settings"
        private const val KEY_FIRST_RUN = "first_run"
    }

    /**
     * Save color pairs list
     */
    fun saveColorPairs(colorPairs: List<ColorPair>) {
        val json = gson.toJson(colorPairs)
        prefs.edit().putString(KEY_COLOR_PAIRS, json).apply()
    }

    /**
     * Load color pairs list
     */
    fun loadColorPairs(): MutableList<ColorPair> {
        val json = prefs.getString(KEY_COLOR_PAIRS, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<ColorPair>>() {}.type
                gson.fromJson<MutableList<ColorPair>>(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }

    /**
     * Save app settings
     */
    fun saveSettings(settings: AppSettings) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }

    /**
     * Load app settings
     */
    fun loadSettings(): AppSettings {
        val json = prefs.getString(KEY_SETTINGS, null)
        return if (json != null) {
            try {
                gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
    }

    /**
     * Check if this is the first run
     */
    fun isFirstRun(): Boolean {
        return prefs.getBoolean(KEY_FIRST_RUN, true)
    }

    /**
     * Set first run flag
     */
    fun setFirstRun(isFirstRun: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_RUN, isFirstRun).apply()
    }

    /**
     * Add a new color pair
     */
    fun addColorPair(colorPair: ColorPair): Boolean {
        val colorPairs = loadColorPairs()
        if (colorPairs.size >= ColorPair.MAX_COLOR_PAIRS) {
            return false
        }
        colorPairs.add(colorPair)
        saveColorPairs(colorPairs)
        return true
    }

    /**
     * Update an existing color pair
     */
    fun updateColorPair(colorPair: ColorPair) {
        val colorPairs = loadColorPairs()
        val index = colorPairs.indexOfFirst { it.id == colorPair.id }
        if (index != -1) {
            colorPairs[index] = colorPair
            saveColorPairs(colorPairs)
        }
    }

    /**
     * Delete a color pair
     */
    fun deleteColorPair(id: Long) {
        val colorPairs = loadColorPairs()
        colorPairs.removeAll { it.id == id }
        saveColorPairs(colorPairs)
    }

    /**
     * Get enabled color pairs only
     */
    fun getEnabledColorPairs(): List<ColorPair> {
        return loadColorPairs().filter { it.isEnabled }
    }

    /**
     * Initialize default color pairs for first run
     */
    fun initializeDefaults() {
        if (isFirstRun()) {
            val defaults = mutableListOf(
                ColorPair(
                    targetColor = Color.parseColor("#FF0000"),
                    replacementColor = Color.parseColor("#00FF00"),
                    tolerance = 0.3f,
                    priority = 0
                )
            )
            saveColorPairs(defaults)
            setFirstRun(false)
        }
    }

    /**
     * Clear all preferences
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
