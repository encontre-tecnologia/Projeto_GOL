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
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.roundToInt

class TravelAlarmService : Service() {
    companion object {
        const val ACTION_START = "br.com.gui.carlembrete.travel.START"
        const val ACTION_STOP = "br.com.gui.carlembrete.travel.STOP"
        const val ACTION_SILENCE = "br.com.gui.carlembrete.travel.SILENCE"
        const val ACTION_SIMULATE_ROUTE = "br.com.gui.carlembrete.travel.SIMULATE_ROUTE"
        private const val TRACKING_CHANNEL = "travel_alarm_tracking"
        private const val ALERT_CHANNEL = "travel_alarm_alert"
        private const val VIBRATION_ALERT_CHANNEL = "travel_alarm_alert_vibration"
        private const val TRACKING_NOTIFICATION = 7301
        private const val ALERT_NOTIFICATION = 7302

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TravelAlarmService::class.java).setAction(ACTION_START)
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TravelAlarmService::class.java).setAction(ACTION_STOP))
        }

        fun simulateRoute(context: Context) {
            if (!BuildConfig.DEBUG) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, TravelAlarmService::class.java).setAction(ACTION_SIMULATE_ROUTE)
            )
        }
    }

    private lateinit var locationClient: FusedLocationProviderClient
    private var currentIntervalMillis = 0L
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val simulationHandler = Handler(Looper.getMainLooper())
    private var simulationMode = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::handleLocation)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        createChannels()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                finishTracking(arrived = false)
                return START_NOT_STICKY
            }
            ACTION_SILENCE -> {
                stopAlarmEffects()
                TravelAlarmStore.setActive(this, active = false, arrived = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val state = TravelAlarmStore.load(this)
        if (!state.active && intent?.action != ACTION_START) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (state.destinationName.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        TravelAlarmStore.setActive(this, active = true)
        startForeground(TRACKING_NOTIFICATION, trackingNotification(state, state.lastDistanceMeters))
        if (intent?.action == ACTION_SIMULATE_ROUTE && BuildConfig.DEBUG) {
            startRouteSimulation(state)
            return START_NOT_STICKY
        }
        requestUpdates(intervalFor(state.lastDistanceMeters))
        loadInitialLocation()
        return START_STICKY
    }

    private fun loadInitialLocation() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        locationClient.lastLocation.addOnSuccessListener { location ->
            if (location == null) return@addOnSuccessListener
            val ageMillis = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
            if (ageMillis <= 30 * 60_000L) {
                handleLocation(location, allowArrival = ageMillis <= 2 * 60_000L)
            }
        }

        val tokenSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(30_000L)
            .setDurationMillis(15_000L)
            .build()
        locationClient.getCurrentLocation(request, tokenSource.token)
            .addOnSuccessListener { location -> location?.let { handleLocation(it, allowArrival = true) } }
    }

    private fun requestUpdates(intervalMillis: Long) {
        if (currentIntervalMillis == intervalMillis) return
        currentIntervalMillis = intervalMillis
        locationClient.removeLocationUpdates(locationCallback)
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            finishTracking(arrived = false)
            return
        }
        val priority = if (intervalMillis <= 60_000L) Priority.PRIORITY_HIGH_ACCURACY
        else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val request = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis((intervalMillis / 2).coerceAtLeast(10_000L))
            .setMaxUpdateDelayMillis(intervalMillis * 2)
            .setMinUpdateDistanceMeters(if (intervalMillis >= 120_000L) 500f else 80f)
            .build()
        locationClient.requestLocationUpdates(request, locationCallback, mainLooper)
    }

    private fun handleLocation(location: Location, allowArrival: Boolean = true) {
        val state = TravelAlarmStore.load(this)
        if (!state.active) return
        val output = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, state.latitude, state.longitude, output)
        val distance = output[0]
        TravelAlarmStore.updateDistance(this, distance)
        if (distance <= state.radiusMeters && allowArrival) {
            triggerArrival(state)
            return
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(TRACKING_NOTIFICATION, trackingNotification(state, distance))
        if (!simulationMode) requestUpdates(intervalFor(distance))
    }

    private fun startRouteSimulation(state: TravelAlarmState) {
        simulationMode = true
        locationClient.removeLocationUpdates(locationCallback)
        simulationHandler.removeCallbacksAndMessages(null)
        val radius = state.radiusMeters.toFloat()
        val testDistances = listOf(radius + 20_000f, radius + 10_000f, radius + 3_000f, radius + 800f, (radius - 200f).coerceAtLeast(50f))
        testDistances.forEachIndexed { index, distanceMeters ->
            simulationHandler.postDelayed({
                if (!TravelAlarmStore.load(this).active) return@postDelayed
                val latitudeOffset = distanceMeters / 111_320.0
                handleLocation(Location("zellu_route_simulator").apply {
                    latitude = state.latitude + latitudeOffset
                    longitude = state.longitude
                    accuracy = 8f
                    time = System.currentTimeMillis()
                })
            }, index * 3_000L)
        }
    }

    private fun intervalFor(distanceMeters: Float?): Long = when {
        distanceMeters == null -> 60_000L
        distanceMeters > 100_000f -> 5 * 60_000L
        distanceMeters > 30_000f -> 2 * 60_000L
        distanceMeters > 10_000f -> 60_000L
        else -> 20_000L
    }

    private fun triggerArrival(state: TravelAlarmState) {
        locationClient.removeLocationUpdates(locationCallback)
        TravelAlarmStore.setActive(this, active = false, arrived = true)
        stopForeground(STOP_FOREGROUND_REMOVE)

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Zellu:TravelArrival")
            .apply { acquire(60_000L) }

        if (TravelAlarmStore.isSoundEnabled(this)) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                play()
            }
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 350, 700), 0))
        startForeground(ALERT_NOTIFICATION, arrivalNotification(state))
    }

    private fun trackingNotification(state: TravelAlarmState, distance: Float?) =
        NotificationCompat.Builder(this, TRACKING_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle("Despertador de viagem ativo")
            .setContentText(distance?.let { "${formatDistance(it)} até ${state.destinationName}" } ?: "Monitorando ${state.destinationName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("O Zellu está acompanhando sua aproximação de ${state.destinationName} com consumo adaptativo."))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Cancelar", serviceIntent(ACTION_STOP, 1))
            .build()

    private fun arrivalNotification(state: TravelAlarmState) =
        NotificationCompat.Builder(
            this,
            if (TravelAlarmStore.isSoundEnabled(this)) ALERT_CHANNEL else VIBRATION_ALERT_CHANNEL
        )
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setContentTitle("Bora acordar! Você está chegando")
            .setContentText("Você entrou no raio de ${state.destinationName}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Hora de acordar: você está chegando em ${state.destinationName}."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openAppIntent())
            .addAction(0, "Já acordei", serviceIntent(ACTION_SILENCE, 2))
            .build()

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun serviceIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, TravelAlarmService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun finishTracking(arrived: Boolean) {
        locationClient.removeLocationUpdates(locationCallback)
        TravelAlarmStore.setActive(this, active = false, arrived = arrived)
        stopAlarmEffects()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAlarmEffects() {
        ringtone?.stop()
        ringtone = null
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        getSystemService(NotificationManager::class.java).cancel(ALERT_NOTIFICATION)
    }

    override fun onDestroy() {
        simulationHandler.removeCallbacksAndMessages(null)
        locationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(TRACKING_CHANNEL, "Despertador de viagem", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Mostra o acompanhamento de uma viagem ativa"
                setSound(null, null)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, "Chegada ao destino", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerta quando você está chegando ao destino"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 350, 700)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(VIBRATION_ALERT_CHANNEL, "Chegada ao destino (somente vibração)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Vibra sem emitir som quando você está chegando ao destino"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 350, 700)
            }
        )
    }

    private fun formatDistance(meters: Float): String = if (meters < 1_000f) {
        "${meters.roundToInt()} m"
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f km", meters / 1_000f)
    }
}
