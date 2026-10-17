package com.essential.shortcuts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class CompassActivity : AppCompatActivity(), SensorEventListener, LocationListener {

    private lateinit var compassView: CompassView
    private lateinit var tvDegree: TextView
    private lateinit var tvCardinal: TextView
    private lateinit var tvPitchRoll: TextView
    private lateinit var tvMagneticField: TextView
    private lateinit var tvLocationCoords: TextView
    private lateinit var btnGetLocation: Button
    private lateinit var btnCopyCoords: Button
    private lateinit var btnCalibrateInfo: Button
    private lateinit var btnClose: ImageButton

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var lastAzimuthRounded = -1
    private var currentCoords: String? = null
    private var locationManager: LocationManager? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            fetchLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)

        // Make window background transparent for popup style
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        compassView = findViewById(R.id.compass_view)
        tvDegree = findViewById(R.id.tv_degree)
        tvCardinal = findViewById(R.id.tv_cardinal)
        tvPitchRoll = findViewById(R.id.tv_pitch_roll)
        tvMagneticField = findViewById(R.id.tv_magnetic_field)
        tvLocationCoords = findViewById(R.id.tv_location_coords)
        btnGetLocation = findViewById(R.id.btn_get_location)
        btnCopyCoords = findViewById(R.id.btn_copy_coords)
        btnCalibrateInfo = findViewById(R.id.btn_calibrate_info)
        btnClose = findViewById(R.id.btn_close)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        btnClose.setOnClickListener { finish() }

        btnGetLocation.setOnClickListener { requestLocationUpdate() }

        btnCopyCoords.setOnClickListener {
            val coords = currentCoords
            if (coords != null) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Coordinates", coords)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Coordinates copied!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Locate first to copy coordinates", Toast.LENGTH_SHORT).show()
            }
        }

        btnCalibrateInfo.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Compass Calibration")
                .setMessage("To calibrate your compass sensors:\n\n1. Hold your device in front of you.\n2. Move it slowly in a figure-8 (∞) pattern a few times.\n3. Keep away from strong metallic objects or magnetic sources.")
                .setPositiveButton("OK", null)
                .show()
        }

        // Try getting initial location if permission is already available
        if (hasLocationPermission()) {
            fetchLocation()
        }
    }

    override fun onResume() {
        super.onResume()
        if (rotationVectorSensor != null) {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magnetometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
        magnetometerSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val azimuthDeg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

                updateOrientationUI(azimuthDeg, pitchDeg, rollDeg)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                calculateFallbackOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true

                val strength = sqrt(
                    (event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]).toDouble()
                ).toFloat()
                tvMagneticField.text = "${strength.roundToInt()} µT"

                calculateFallbackOrientation()
            }
        }
    }

    private fun calculateFallbackOrientation() {
        if (rotationVectorSensor != null) return // Already handling via rotation vector
        if (!hasGravity || !hasGeomagnetic) return

        val r = FloatArray(9)
        val i = FloatArray(9)
        if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
            val orientation = FloatArray(3)
            SensorManager.getOrientation(r, orientation)

            val azimuthDeg = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
            val pitchDeg = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rollDeg = Math.toDegrees(orientation[2].toDouble()).toFloat()

            updateOrientationUI(azimuthDeg, pitchDeg, rollDeg)
        }
    }

    private fun updateOrientationUI(azimuth: Float, pitch: Float, roll: Float) {
        compassView.setAzimuth(azimuth)
        compassView.setPitchAndRoll(pitch, roll)

        val roundedAzimuth = azimuth.roundToInt() % 360
        tvDegree.text = "$roundedAzimuth°"
        tvCardinal.text = getCardinalDirection(roundedAzimuth.toFloat())
        tvPitchRoll.text = "${pitch.roundToInt()}° / ${roll.roundToInt()}°"

        // Haptic feedback when crossing exact North (0° / 360°)
        if (roundedAzimuth == 0 && lastAzimuthRounded != 0) {
            triggerHapticFeedback()
        }
        lastAzimuthRounded = roundedAzimuth
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                compassView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (_: Exception) {}
    }

    private fun getCardinalDirection(azimuth: Float): String {
        return when {
            azimuth >= 337.5 || azimuth < 22.5 -> "N • North"
            azimuth >= 22.5 && azimuth < 67.5 -> "NE • North East"
            azimuth >= 67.5 && azimuth < 112.5 -> "E • East"
            azimuth >= 112.5 && azimuth < 157.5 -> "SE • South East"
            azimuth >= 157.5 && azimuth < 202.5 -> "S • South"
            azimuth >= 202.5 && azimuth < 247.5 -> "SW • South West"
            azimuth >= 247.5 && azimuth < 292.5 -> "W • West"
            azimuth >= 292.5 && azimuth < 337.5 -> "NW • North West"
            else -> "N • North"
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationUpdate() {
        if (hasLocationPermission()) {
            fetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchLocation() {
        if (!hasLocationPermission()) return
        try {
            val lm = locationManager ?: return
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLocation = lastGps ?: lastNetwork

            if (bestLocation != null) {
                updateLocationDisplay(bestLocation)
            }

            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null)
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null)
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not fetch location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLocationDisplay(location: Location) {
        val lat = String.format("%.4f", location.latitude)
        val lon = String.format("%.4f", location.longitude)
        val alt = if (location.hasAltitude()) " • Alt: ${location.altitude.roundToInt()}m" else ""
        currentCoords = "$lat, $lon"
        tvLocationCoords.text = "Lat: $lat, Lon: $lon$alt"
    }

    override fun onLocationChanged(location: Location) {
        updateLocationDisplay(location)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
