package com.screencolor.invert

import android.app.Application
import android.content.Context
import com.screencolor.invert.utils.PreferenceManager

/**
 * Application class for ScreenColor Invert System
 */
class ScreenColorApp : Application() {

    companion object {
        lateinit var instance: ScreenColorApp
            private set
        
        fun getContext(): Context = instance.applicationContext
    }

    lateinit var preferenceManager: PreferenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferenceManager = PreferenceManager(this)
    }
}
