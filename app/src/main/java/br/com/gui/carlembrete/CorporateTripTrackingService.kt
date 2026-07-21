package br.com.gui.carlembrete

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.roundToInt

class CorporateTripTrackingService : Service() {
    companion object {
        private const val ACTION_START = "br.com.gui.carlembrete.fleet_trip.START"
        private const val ACTION_FINISH = "br.com.gui.carlembrete.fleet_trip.FINISH"
        private const val ACTION_STOP_LOCAL = "br.com.gui.carlembrete.fleet_trip.STOP_LOCAL"
        private const val ACTION_SIMULATE_TEST_DISTANCE = "br.com.gui.carlembrete.fleet_trip.SIMULATE_TEST_DISTANCE"
        private const val CHANNEL_ID = "corporate_trip_tracking"
        private const val NOTIFICATION_ID = 7410
        private const val PREFS = "corporate_trip_tracking"
        private const val MIN_DISTANCE_METERS = 120f
        private const val MAX_REASONABLE_SPEED_MPS = 55f
        private const val DEFAULT_SPEED_LIMIT_KMH = 100
        private const val DEFAULT_SPEED_TOLERANCE_KMH = 10
        private const val DEFAULT_SPEED_MIN_SECONDS = 15
        private const val MIN_SPEED_EVENT_INTERVAL_MILLIS = 10 * 60 * 1000L

        fun start(
            context: Context,
            companyId: String,
            reservationId: String,
            vehicleId: String,
            vehicleName: String,
            driverName: String
        ) {
            val intent = Intent(context, CorporateTripTrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra("companyId", companyId)
                .putExtra("reservationId", reservationId)
                .putExtra("vehicleId", vehicleId)
                .putExtra("vehicleName", vehicleName)
                .putExtra("driverName", driverName)
            ContextCompat.startForegroundService(context, intent)
        }

        fun finish(context: Context, companyId: String, reservationId: String, vehicleId: String) {
            context.startService(
                Intent(context, CorporateTripTrackingService::class.java)
                    .setAction(ACTION_FINISH)
                    .putExtra("companyId", companyId)
                    .putExtra("reservationId", reservationId)
                    .putExtra("vehicleId", vehicleId)
            )
        }

        fun stopLocal(context: Context) {
            context.startService(Intent(context, CorporateTripTrackingService::class.java).setAction(ACTION_STOP_LOCAL))
        }

        fun simulateTestDistance(context: Context, meters: Float = 1000f) {
            if (!BuildConfig.DEBUG) return
            context.startService(
                Intent(context, CorporateTripTrackingService::class.java)
                    .setAction(ACTION_SIMULATE_TEST_DISTANCE)
                    .putExtra("meters", meters)
            )
        }
    }

    private lateinit var locationClient: FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::handleLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking(intent)
            ACTION_FINISH -> finishTracking(intent)
            ACTION_STOP_LOCAL -> stopTracking()
            ACTION_SIMULATE_TEST_DISTANCE -> simulateDistance(intent)
        }
        return START_STICKY
    }

    private fun startTracking(intent: Intent) {
        val companyId = intent.getStringExtra("companyId").orEmpty()
        val reservationId = intent.getStringExtra("reservationId").orEmpty()
        val vehicleId = intent.getStringExtra("vehicleId").orEmpty()
        if (companyId.isBlank() || reservationId.isBlank() || vehicleId.isBlank()) {
            stopSelf()
            return
        }
        if (!hasLocationPermission()) {
            stopSelf()
            return
        }

        prefs().edit()
            .putBoolean("active", true)
            .putString("companyId", companyId)
            .putString("reservationId", reservationId)
            .putString("vehicleId", vehicleId)
            .putString("vehicleName", intent.getStringExtra("vehicleName").orEmpty())
            .putString("driverName", intent.getStringExtra("driverName").orEmpty())
            .putLong("startedAt", System.currentTimeMillis())
            .putFloat("distanceMeters", 0f)
            .remove("lastLatitude")
            .remove("lastLongitude")
            .remove("lastTime")
            .remove("speedAboveSince")
            .remove("lastSpeedEventAt")
            .putInt("speedLimitKmh", DEFAULT_SPEED_LIMIT_KMH)
            .putInt("speedToleranceKmh", DEFAULT_SPEED_TOLERANCE_KMH)
            .putInt("speedMinimumSeconds", DEFAULT_SPEED_MIN_SECONDS)
            .apply()

        loadSpeedSettings(companyId)
        startForeground(NOTIFICATION_ID, notification("Monitorando viagem corporativa", "Estimando KM com baixo consumo."))
        requestEconomyUpdates()
    }

    private fun requestEconomyUpdates() {
        if (!hasLocationPermission()) {
            stopTracking()
            return
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L)
            .setMinUpdateIntervalMillis(30_000L)
            .setMaxUpdateDelayMillis(5 * 60_000L)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()
        locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun handleLocation(location: Location) {
        if (!prefs().getBoolean("active", false)) return
        if (location.accuracy > 100f) return

        val prefs = prefs()
        val lastLat = prefs.getString("lastLatitude", null)?.toDoubleOrNull()
        val lastLng = prefs.getString("lastLongitude", null)?.toDoubleOrNull()
        val lastTime = prefs.getLong("lastTime", 0L)
        var distanceMeters = prefs.getFloat("distanceMeters", 0f)

        if (lastLat != null && lastLng != null && lastTime > 0L) {
            val output = FloatArray(1)
            Location.distanceBetween(lastLat, lastLng, location.latitude, location.longitude, output)
            val deltaMeters = output[0]
            val elapsedSeconds = ((location.time - lastTime).coerceAtLeast(1L) / 1000f)
            val speedMps = deltaMeters / elapsedSeconds
            if (deltaMeters >= 25f && speedMps <= MAX_REASONABLE_SPEED_MPS) {
                distanceMeters += deltaMeters
            }
            val measuredSpeedKmh = if (location.hasSpeed()) {
                (location.speed * 3.6f)
            } else {
                (speedMps * 3.6f)
            }
            evaluateSpeedEvent(location, measuredSpeedKmh, elapsedSeconds)
        } else if (location.hasSpeed()) {
            evaluateSpeedEvent(location, location.speed * 3.6f, 0f)
        }

        prefs.edit()
            .putString("lastLatitude", location.latitude.toString())
            .putString("lastLongitude", location.longitude.toString())
            .putLong("lastTime", location.time)
            .putFloat("distanceMeters", distanceMeters)
            .apply()

        val kmText = "%.1f km".format(distanceMeters / 1000.0).replace(',', '.')
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification("Viagem corporativa em andamento", "Estimativa atual: $kmText"))
        syncLiveDistance(distanceMeters)
    }

    private fun finishTracking(intent: Intent) {
        val prefs = prefs()
        val companyId = intent.getStringExtra("companyId").orEmpty().ifBlank { prefs.getString("companyId", "").orEmpty() }
        val reservationId = intent.getStringExtra("reservationId").orEmpty().ifBlank { prefs.getString("reservationId", "").orEmpty() }
        val vehicleId = intent.getStringExtra("vehicleId").orEmpty().ifBlank { prefs.getString("vehicleId", "").orEmpty() }
        val distanceMeters = prefs.getFloat("distanceMeters", 0f)
        val distanceKm = distanceMeters / 1000.0
        val odometerIncrement = distanceKm.roundToInt().coerceAtLeast(0)

        locationClient.removeLocationUpdates(locationCallback)
        if (companyId.isNotBlank() && reservationId.isNotBlank() && vehicleId.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            val tripRef = db.collection("companies").document(companyId).collection("trips").document(reservationId)
            val vehicleRef = db.collection("companies").document(companyId).collection("vehicles").document(vehicleId)
            db.runTransaction { transaction ->
                val vehicle = transaction.get(vehicleRef)
                val trip = transaction.get(tripRef)
                val currentOdometer = vehicle.getLong("odometerKm") ?: vehicle.getLong("kmAtual") ?: 0L
                val startOdometer = trip.getLong("odometerStartKm") ?: currentOdometer
                val endOdometer = currentOdometer + odometerIncrement
                transaction.set(
                    tripRef,
                    mapOf(
                        "gpsDistanceKm" to distanceKm,
                        "estimatedDistanceKm" to distanceKm,
                        "odometerIncrementKm" to odometerIncrement,
                        "odometerStartKm" to startOdometer,
                        "odometerEndKm" to endOdometer,
                        "trackingMode" to "economy_gps",
                        "trackingFinishedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
                transaction.set(
                    vehicleRef,
                    mapOf(
                        "odometerKm" to endOdometer,
                        "kmAtual" to endOdometer,
                        "lastTripDistanceKm" to distanceKm,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
                .addOnCompleteListener { stopTracking() }
            return
        }
        stopTracking()
    }

    private fun simulateDistance(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val prefs = prefs()
        if (!prefs.getBoolean("active", false)) return
        val meters = intent.getFloatExtra("meters", 1000f).coerceIn(100f, 10_000f)
        val distanceMeters = prefs.getFloat("distanceMeters", 0f) + meters
        prefs.edit()
            .putFloat("distanceMeters", distanceMeters)
            .putLong("lastTime", System.currentTimeMillis())
            .apply()
        val kmText = "%.1f km".format(distanceMeters / 1000.0).replace(',', '.')
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification("Viagem corporativa em teste", "Distancia simulada: $kmText"))
        syncLiveDistance(distanceMeters, simulated = true)
    }

    private fun syncLiveDistance(distanceMeters: Float, simulated: Boolean = false) {
        val prefs = prefs()
        val companyId = prefs.getString("companyId", "").orEmpty()
        val reservationId = prefs.getString("reservationId", "").orEmpty()
        if (companyId.isBlank() || reservationId.isBlank()) return
        val distanceKm = distanceMeters / 1000.0
        FirebaseFirestore.getInstance()
            .collection("companies")
            .document(companyId)
            .collection("trips")
            .document(reservationId)
            .set(
                mapOf(
                    "gpsDistanceKm" to distanceKm,
                    "estimatedDistanceKm" to distanceKm,
                    "trackingMode" to if (simulated) "debug_simulated" else "economy_gps",
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }

    private fun loadSpeedSettings(companyId: String) {
        FirebaseFirestore.getInstance()
            .collection("companies")
            .document(companyId)
            .get()
            .addOnSuccessListener { snap ->
                val limit = (snap.getLong("speedLimitKmh") ?: DEFAULT_SPEED_LIMIT_KMH.toLong()).toInt().coerceIn(40, 160)
                val tolerance = (snap.getLong("speedToleranceKmh") ?: DEFAULT_SPEED_TOLERANCE_KMH.toLong()).toInt().coerceIn(0, 40)
                val minimumSeconds = (snap.getLong("speedMinimumSeconds") ?: DEFAULT_SPEED_MIN_SECONDS.toLong()).toInt().coerceIn(5, 120)
                prefs().edit()
                    .putInt("speedLimitKmh", limit)
                    .putInt("speedToleranceKmh", tolerance)
                    .putInt("speedMinimumSeconds", minimumSeconds)
                    .apply()
            }
    }

    private fun evaluateSpeedEvent(location: Location, speedKmh: Float, elapsedSeconds: Float) {
        if (speedKmh <= 0f || speedKmh > MAX_REASONABLE_SPEED_MPS * 3.6f) return
        if (location.accuracy > 80f) return

        val prefs = prefs()
        val limit = prefs.getInt("speedLimitKmh", DEFAULT_SPEED_LIMIT_KMH)
        val tolerance = prefs.getInt("speedToleranceKmh", DEFAULT_SPEED_TOLERANCE_KMH)
        val minimumSeconds = prefs.getInt("speedMinimumSeconds", DEFAULT_SPEED_MIN_SECONDS)
        val threshold = limit + tolerance
        val now = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()

        if (speedKmh < threshold) {
            prefs.edit().remove("speedAboveSince").apply()
            return
        }

        val aboveSince = prefs.getLong("speedAboveSince", 0L).takeIf { it > 0L } ?: now
        val durationSeconds = ((now - aboveSince).coerceAtLeast(0L) / 1000L).toInt()
        prefs.edit().putLong("speedAboveSince", aboveSince).apply()

        if (durationSeconds < minimumSeconds) return
        val lastEventAt = prefs.getLong("lastSpeedEventAt", 0L)
        if (now - lastEventAt < MIN_SPEED_EVENT_INTERVAL_MILLIS) return

        prefs.edit().putLong("lastSpeedEventAt", now).putLong("speedAboveSince", now).apply()
        saveSpeedEvent(
            location = location,
            speedKmh = speedKmh,
            limit = limit,
            tolerance = tolerance,
            durationSeconds = durationSeconds.coerceAtLeast(elapsedSeconds.roundToInt())
        )
    }

    private fun saveSpeedEvent(location: Location, speedKmh: Float, limit: Int, tolerance: Int, durationSeconds: Int) {
        val prefs = prefs()
        val companyId = prefs.getString("companyId", "").orEmpty()
        val reservationId = prefs.getString("reservationId", "").orEmpty()
        val vehicleId = prefs.getString("vehicleId", "").orEmpty()
        if (companyId.isBlank() || reservationId.isBlank() || vehicleId.isBlank()) return

        val db = FirebaseFirestore.getInstance()
        val roundedSpeed = speedKmh.roundToInt()
        val event = mapOf(
            "tripId" to reservationId,
            "reservationId" to reservationId,
            "vehicleId" to vehicleId,
            "vehicleName" to prefs.getString("vehicleName", "").orEmpty(),
            "driverName" to prefs.getString("driverName", "").orEmpty(),
            "speedKmh" to roundedSpeed,
            "speedLimitKmh" to limit,
            "toleranceKmh" to tolerance,
            "durationSeconds" to durationSeconds,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracyMeters" to location.accuracy,
            "occurredAt" to FieldValue.serverTimestamp(),
            "source" to "android_economy_gps"
        )
        db.collection("companies")
            .document(companyId)
            .collection("speedEvents")
            .add(event)

        db.collection("companies")
            .document(companyId)
            .collection("trips")
            .document(reservationId)
            .set(
                mapOf(
                    "speedEventsCount" to FieldValue.increment(1),
                    "maxSpeedKmh" to roundedSpeed,
                    "lastSpeedEventAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
    }

    private fun stopTracking() {
        locationClient.removeLocationUpdates(locationCallback)
        prefs().edit().clear().apply()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun prefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hasLocationPermission(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return hasFine || hasCoarse
    }

    private fun notification(title: String, text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Viagem corporativa",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitora KM estimado de viagens corporativas com baixo consumo."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
