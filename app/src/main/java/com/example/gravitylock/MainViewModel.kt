package com.example.gravitylock

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("gravity_lock_prefs", Context.MODE_PRIVATE)

    private val _isMasterEnabled = MutableStateFlow(prefs.getBoolean("is_master_enabled", false))
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _gravityZ = MutableStateFlow(0f)
    val gravityZ: StateFlow<Float> = _gravityZ.asStateFlow()

    private val _proximity = MutableStateFlow(0f)
    val proximity: StateFlow<Float> = _proximity.asStateFlow()

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _isBatteryOptimizationIgnored = MutableStateFlow(false)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            "sensor_gravity_z" -> _gravityZ.value = sharedPreferences.getFloat(key, 0f)
            "sensor_proximity" -> _proximity.value = sharedPreferences.getFloat(key, 0f)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        startPermissionPolling()
    }

    private fun startPermissionPolling() {
        viewModelScope.launch {
            while (true) {
                _isAccessibilityEnabled.value = PermissionHelper.isAccessibilityServiceEnabled(
                    getApplication(), GravityLockService::class.java
                )
                _isBatteryOptimizationIgnored.value = PermissionHelper.isBatteryOptimizationIgnored(getApplication())
                delay(1000) // Poll every second for UI updates
            }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_master_enabled", enabled).apply()
        _isMasterEnabled.value = enabled
    }

    override fun onCleared() {
        super.onCleared()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}
