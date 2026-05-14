package com.suraksha.setu

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

// ─── BACKGROUND SHAKE SERVICE ─────────────────────────────────────────────────
class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 12f
    private val CHANNEL_ID = "suraksha_channel"

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        startForegroundNotification()
    }

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Suraksha-Setu Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitoring for shake gesture" }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ Suraksha-Setu Active")
            .setContentText("Shake detection running in background")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

            if (acceleration > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 3000) {
                    lastShakeTime = currentTime
                    val intent = Intent("SHAKE_DETECTED").setPackage(packageName)
                    sendBroadcast(intent)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

// ─── MAIN ACTIVITY ────────────────────────────────────────────────────────────
class MainActivity : AppCompatActivity(), SensorEventListener {

    // UI Elements
    private lateinit var btnSOS: Button
    private lateinit var btnAddContact: Button
    private lateinit var btnVolunteer: Button
    private lateinit var etContact: EditText
    private lateinit var tvStatus: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvContacts: TextView

    // Sensors & Location
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var locationManager: LocationManager

    // Audio Recording
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingFilePath = ""

    // Data
    private val safeCircle = mutableListOf<String>()
    private var currentLat = 0.0
    private var currentLon = 0.0
    private var isVolunteer = false

    // Shake Detection
    private var lastShakeTime = 0L
    private val SHAKE_THRESHOLD = 12f

    // Broadcast receiver for background shake
    private val shakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "SHAKE_DETECTED") {
                triggerSOS("Background Shake")
            }
        }
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Link UI
        btnSOS = findViewById(R.id.btnSOS)
        btnAddContact = findViewById(R.id.btnAddContact)
        btnVolunteer = findViewById(R.id.btnVolunteer)
        etContact = findViewById(R.id.etContact)
        tvStatus = findViewById(R.id.tvStatus)
        tvLocation = findViewById(R.id.tvLocation)
        tvContacts = findViewById(R.id.tvContacts)

        // Setup sensors
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Setup location
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Register shake broadcast receiver
        val filter = IntentFilter("SHAKE_DETECTED")
        registerReceiver(shakeReceiver, filter, RECEIVER_NOT_EXPORTED)

        // Load saved contacts
        loadContacts()

        // Ask for permissions
        requestAllPermissions()

        // Start background shake service
        startShakeService()

        // Get FCM Token
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                android.util.Log.d("FCM_TOKEN", token)
                tvStatus.text = "FCM Ready ✅"
            }
            .addOnFailureListener {
                android.util.Log.d("FCM_TOKEN", "Failed: ${it.message}")
                tvStatus.text = "Status: Ready ✅"
            }

        // SOS Button
        btnSOS.setOnClickListener {
            triggerSOS("Button Press")
        }

        // Add Contact
        btnAddContact.setOnClickListener {
            val number = etContact.text.toString().trim()
            if (number.length >= 10) {
                if (safeCircle.size < 5) {
                    safeCircle.add(number)
                    etContact.text.clear()
                    updateContactDisplay()
                    saveContacts()
                    Toast.makeText(this, "✅ Added to Safe-Circle!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Maximum 5 contacts allowed!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Volunteer Mode
        btnVolunteer.setOnClickListener {
            isVolunteer = !isVolunteer
            if (isVolunteer) {
                btnVolunteer.text = "✅ You are a Verified Volunteer!"
                btnVolunteer.backgroundTintList = getColorStateList(android.R.color.holo_green_dark)
                Toast.makeText(this, "You are now a Verified Responder!", Toast.LENGTH_SHORT).show()
            } else {
                btnVolunteer.text = "🤝 Register as Verified Volunteer"
                btnVolunteer.backgroundTintList = getColorStateList(android.R.color.holo_blue_dark)
            }
        }
    }

    // ─── SAVE & LOAD CONTACTS ─────────────────────────────────
    private fun saveContacts() {
        val prefs = getSharedPreferences("SurakshaPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("safeCircle", safeCircle.toSet()).apply()
    }

    private fun loadContacts() {
        val prefs = getSharedPreferences("SurakshaPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("safeCircle", emptySet())
        if (saved != null) {
            safeCircle.clear()
            safeCircle.addAll(saved)
            updateContactDisplay()
        }
    }

    // ─── START BACKGROUND SERVICE ──────────────────────────────
    private fun startShakeService() {
        val serviceIntent = Intent(this, ShakeService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    // ─── AUDIO RECORDING ──────────────────────────────────────
    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) return

        try {
            recordingFilePath = "${externalCacheDir?.absolutePath}/sos_audio.3gp"

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(recordingFilePath)
                setMaxDuration(30000)
                prepare()
                start()
            }

            isRecording = true
            tvStatus.text = "🎙️ Recording audio for 30 seconds..."

            Handler(Looper.getMainLooper()).postDelayed({
                stopAudioRecording()
            }, 30000)

        } catch (e: Exception) {
            Toast.makeText(this, "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() {
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                tvStatus.text = "🎙️ Audio recorded successfully!"
            }
        } catch (e: Exception) {
            mediaRecorder = null
            isRecording = false
        }
    }

    // ─── SHAKE DETECTION (Foreground) ─────────────────────────
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat() - SensorManager.GRAVITY_EARTH

            if (acceleration > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > 3000) {
                    lastShakeTime = currentTime
                    triggerSOS("Shake Gesture")
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ─── MAIN SOS FUNCTION ────────────────────────────────────
    private fun triggerSOS(trigger: String) {
        tvStatus.text = "🚨 SOS TRIGGERED via $trigger!"

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
        }

        startAudioRecording()

        if (safeCircle.isEmpty()) {
            Toast.makeText(this, "⚠️ No contacts in Safe-Circle!", Toast.LENGTH_LONG).show()
            return
        }

        val message = buildSOSMessage()
        for (contact in safeCircle) {
            sendSMS(contact, message)
        }

        Toast.makeText(this, "🚨 SOS sent to ${safeCircle.size} contacts!", Toast.LENGTH_LONG).show()
    }

    // ─── BUILD SOS MESSAGE ────────────────────────────────────
    private fun buildSOSMessage(): String {
        return if (currentLat != 0.0 && currentLon != 0.0) {
            "🚨 EMERGENCY! I need help!\n" +
                    "📍 My Location:\n" +
                    "https://maps.google.com/?q=$currentLat,$currentLon\n" +
                    "Sent via Suraksha-Setu"
        } else {
            "🚨 EMERGENCY! I need help!\n" +
                    "📍 Location not available yet.\n" +
                    "Please call me immediately!\n" +
                    "Sent via Suraksha-Setu"
        }
    }

    // ─── SEND SMS ─────────────────────────────────────────────
    private fun sendSMS(number: String, message: String) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(number, null, parts, null, null)
                Toast.makeText(this, "📨 SMS sent to $number", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "SMS Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ─── LOCATION ─────────────────────────────────────────────
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            if (lastKnown != null) {
                currentLat = lastKnown.latitude
                currentLon = lastKnown.longitude
                tvLocation.text = "📍 Location: $currentLat, $currentLon"
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 3000, 1f,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            currentLat = location.latitude
                            currentLon = location.longitude
                            tvLocation.text = "📍 Location: $currentLat, $currentLon"
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                    }
                )
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 3000, 1f,
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            if (currentLat == 0.0) {
                                currentLat = location.latitude
                                currentLon = location.longitude
                                tvLocation.text = "📍 Location: $currentLat, $currentLon"
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                    }
                )
            }
        }
    }

    // ─── UPDATE CONTACT DISPLAY ───────────────────────────────
    private fun updateContactDisplay() {
        if (safeCircle.isEmpty()) {
            tvContacts.text = "Safe-Circle: (empty)"
        } else {
            tvContacts.text = "Safe-Circle (${safeCircle.size}/5):\n" +
                    safeCircle.joinToString("\n") { "📞 $it" }
        }
    }

    // ─── PERMISSIONS ──────────────────────────────────────────
    private fun requestAllPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE
        )
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startLocationUpdates()
            Toast.makeText(this, "Permissions granted! App is ready.", Toast.LENGTH_SHORT).show()
        }
    }

    // ─── LIFECYCLE ────────────────────────────────────────────
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(shakeReceiver)
        stopAudioRecording()
    }
}