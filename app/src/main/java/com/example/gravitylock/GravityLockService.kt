package com.example.gravitylock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlin.math.abs

class GravityLockService : AccessibilityService(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var gravitySensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private var isFaceDown = false
    private var isProximityNear = false
    
    private lateinit var prefs: SharedPreferences
    private var isMasterEnabled = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "is_master_enabled") {
            isMasterEnabled = sharedPreferences.getBoolean(key, false)
            updateSensorRegistration()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GravityLock", "Accessibility Service Connected")
        
        prefs = getSharedPreferences("gravity_lock_prefs", Context.MODE_PRIVATE)
        isMasterEnabled = prefs.getBoolean("is_master_enabled", false)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        updateSensorRegistration()
    }

    private fun updateSensorRegistration() {
        if (isMasterEnabled) {
            gravitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            proximitySensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } else {
            sensorManager.unregisterListener(this)
            isFaceDown = false
            isProximityNear = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We do not need to process accessibility events for this app
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMasterEnabled) return

        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_GRAVITY -> {
                    val z = it.values[2]
                    // Face down: Z-axis approaches -9.8 m/s^2. We allow some tolerance.
                    isFaceDown = z < -8.5f
                    
                    // Broadcast values for UI
                    prefs.edit().putFloat("sensor_gravity_z", z).apply()
                }
                Sensor.TYPE_PROXIMITY -> {
                    val distance = it.values[0]
                    // Typically, if distance < maximum range, it means "near". 
                    // Some sensors return 0 for near, others return < 5.
                    isProximityNear = distance < it.sensor.maximumRange && distance < 3.0f
                    
                    // Broadcast values for UI
                    prefs.edit().putFloat("sensor_proximity", distance).apply()
                }
            }

            checkAndLockScreen()
        }
    }

    private fun checkAndLockScreen() {
        if (isFaceDown && isProximityNear) {
            Log.d("GravityLock", "Conditions met, locking screen!")
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            
            // To prevent repeated lock calls immediately, we could add a debounce, 
            // but locking the screen usually puts sensors to sleep anyway.
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        sensorManager.unregisterListener(this)
    }
}
