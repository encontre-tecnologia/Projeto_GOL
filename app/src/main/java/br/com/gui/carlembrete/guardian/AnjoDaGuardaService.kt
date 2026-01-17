package br.com.gui.carlembrete

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.ParcelUuid
import android.os.SystemClock
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.nio.charset.Charset
import java.util.UUID
import kotlin.math.sin

class AnjoDaGuardaService : Service(), SensorEventListener {
    companion object {
        const val ACTION_START = "br.com.gui.carlembrete.guardian.START"
        const val ACTION_STOP = "br.com.gui.carlembrete.guardian.STOP"
        const val EXTRA_IS_CAR = "extra_is_car"
        const val EXTRA_NOTIFY_REMOTE = "extra_notify_remote"
        const val EXTRA_ALARM_LOCAL = "extra_alarm_local"

        private const val CHANNEL_ID = "guardian_channel"
        private const val NOTIF_ID = 4401

        private val SERVICE_UUID = UUID.fromString("8f0efc9a-3d8d-4b2a-8f6a-5e75b6d3b8f1")
        private val CHAR_ALERT_UUID = UUID.fromString("a2c0e0a2-2b49-4c91-9b4c-3f0e0a8b0b0f")
        private val DESC_CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val TAG = "AnjoDaGuarda"
    }

    private var isCarMode = true
    private var notifyRemote = true
    private var alarmLocal = true

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var lastMagnitude = 0f
    private var lastAlertAt = 0L

    private var bluetoothManager: BluetoothManager? = null
    private var adapter: BluetoothAdapter? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var gattClient: BluetoothGatt? = null
    private var connectedDevices = mutableSetOf<BluetoothDevice>()
    private var firestoreListener: ListenerRegistration? = null
    private var lastAlertTimestamp: Timestamp? = null
    private var lastAlertMillis: Long = 0L
    private var alarmTrack: AudioTrack? = null
    private var lastSoundAt: Long = 0L
    private var previousAlarmVolume: Int? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager?.adapter
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                isCarMode = intent.getBooleanExtra(EXTRA_IS_CAR, true)
                notifyRemote = intent.getBooleanExtra(EXTRA_NOTIFY_REMOTE, true)
                alarmLocal = intent.getBooleanExtra(EXTRA_ALARM_LOCAL, true)
            }
        }

        startForeground(NOTIF_ID, buildStatusNotification("Vigia ativo"))

        if (isCarMode) {
            startMotionMonitor()
            startBleAdvertiser()
            startGattServer()
        } else {
            startBleScanner()
            startFirestoreListener()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopMotionMonitor()
        stopBleAdvertiser()
        stopBleScanner()
        stopGattServer()
        closeGattClient()
        stopFirestoreListener()
        stopOwnerAlarm()
        super.onDestroy()
    }

    private fun startMotionMonitor() {
        accelSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    private fun stopMotionMonitor() {
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val values = event.values
        val magnitude = kotlin.math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2])
        val delta = kotlin.math.abs(magnitude - lastMagnitude)
        lastMagnitude = magnitude

        val now = SystemClock.elapsedRealtime()
        if (delta > 1.8f && now - lastAlertAt > 2000) {
            lastAlertAt = now
            handleMotionDetected()
        }
    }

    private fun handleMotionDetected() {
        showAlertNotification("Movimento detectado")
        if (alarmLocal) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(1200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(1200)
            }
        }
        if (notifyRemote) {
            notifyBleClients()
            sendFirestoreAlert()
        }
    }

    private fun startBleAdvertiser() {
        if (adapter?.isEnabled != true) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        advertiser = adapter?.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopBleAdvertiser() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiser = null
    }

    private val advertiseCallback = object : AdvertiseCallback() {}

    private fun startGattServer() {
        if (adapter?.isEnabled != true) return
        gattServer = bluetoothManager?.openGattServer(this, gattServerCallback)
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            CHAR_ALERT_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val descriptor = BluetoothGattDescriptor(
            DESC_CCCD_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        characteristic.addDescriptor(descriptor)
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }

    private fun stopGattServer() {
        gattServer?.close()
        gattServer = null
        connectedDevices.clear()
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            if (device == null) return
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                connectedDevices.add(device)
            } else {
                connectedDevices.remove(device)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (descriptor?.uuid == DESC_CCCD_UUID && device != null) {
                if (value?.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == true ||
                    value?.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE) == true
                ) {
                    connectedDevices.add(device)
                } else {
                    connectedDevices.remove(device)
                }
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value)
            }
        }
    }

    private fun notifyBleClients() {
        val server = gattServer ?: return
        val service = server.getService(SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(CHAR_ALERT_UUID) ?: return
        characteristic.value = "ALERT".toByteArray(Charset.defaultCharset())
        connectedDevices.forEach { device ->
            server.notifyCharacteristicChanged(device, characteristic, false)
        }
    }

    private fun startBleScanner() {
        if (adapter?.isEnabled != true) return
        scanner = adapter?.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private fun stopBleScanner() {
        scanner?.stopScan(scanCallback)
        scanner = null
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            if (gattClient != null) return
            gattClient = device.connectGatt(this@AnjoDaGuardaService, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt?.discoverServices()
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt?.close()
                gattClient = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            val service = gatt?.getService(SERVICE_UUID) ?: return
            val characteristic = service.getCharacteristic(CHAR_ALERT_UUID) ?: return
            gatt.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(DESC_CCCD_UUID)
            val value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                run {
                    descriptor.value = value
                    gatt.writeDescriptor(descriptor)
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic?.uuid == CHAR_ALERT_UUID) {
                showAlertNotification("Movimento no carro detectado")
                playOwnerAlarm()
            }
        }
    }

    private fun closeGattClient() {
        gattClient?.close()
        gattClient = null
    }

    private fun startFirestoreListener() {
        if (!notifyRemote) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Sem login para escutar alertas remotos")
            showAlertNotification("Login necessario para alertas remotos")
            return
        }
        val db = FirebaseFirestore.getInstance()
        firestoreListener = db.collection("guardian_alerts")
            .document(uid)
            .collection("events")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, err ->
                if (err != null) {
                    Log.e(TAG, "Erro ao escutar alertas remotos", err)
                    return@addSnapshotListener
                }
                val doc = snapshot?.documents?.firstOrNull() ?: return@addSnapshotListener
                val clientMillis = doc.getLong("clientMillis")
                if (clientMillis != null) {
                    if (clientMillis > lastAlertMillis) {
                        lastAlertMillis = clientMillis
                        Log.d(TAG, "Alerta remoto recebido (clientMillis)")
                        showAlertNotification("Movimento no carro detectado")
                        playOwnerAlarm()
                    }
                    return@addSnapshotListener
                }
                val ts = doc.getTimestamp("timestamp") ?: return@addSnapshotListener
                val lastTs = lastAlertTimestamp
                if (lastTs == null || ts > lastTs) {
                    lastAlertTimestamp = ts
                    Log.d(TAG, "Alerta remoto recebido (timestamp)")
                    showAlertNotification("Movimento no carro detectado")
                    playOwnerAlarm()
                }
            }
    }

    private fun stopFirestoreListener() {
        firestoreListener?.remove()
        firestoreListener = null
    }

    private fun sendFirestoreAlert() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Sem login para enviar alerta remoto")
            showAlertNotification("Login necessario para alertas remotos")
            return
        }
        val db = FirebaseFirestore.getInstance()
        val payload = hashMapOf(
            "timestamp" to FieldValue.serverTimestamp(),
            "clientMillis" to System.currentTimeMillis(),
            "device" to Build.MODEL
        )
        db.collection("guardian_alerts")
            .document(uid)
            .collection("events")
            .add(payload)
            .addOnSuccessListener { Log.d(TAG, "Alerta remoto enviado") }
            .addOnFailureListener { err ->
                Log.e(TAG, "Falha ao enviar alerta remoto", err)
                showAlertNotification("Falha ao enviar alerta remoto")
            }
    }

    private fun buildStatusNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anjo da Guarda")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }

    private fun showAlertNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anjo da Guarda")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIF_ID + 1, notification)
    }

    private fun createChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "Anjo da Guarda", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    private fun playOwnerAlarm() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSoundAt < 1500) return
        lastSoundAt = now
        Handler(Looper.getMainLooper()).post {
            stopOwnerAlarm()
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val stream = AudioManager.STREAM_ALARM
            val current = audioManager.getStreamVolume(stream)
            val max = audioManager.getStreamMaxVolume(stream)
            if (current < max) {
                previousAlarmVolume = current
                audioManager.setStreamVolume(stream, max, 0)
            }
            val sampleRate = 44100
            val durationMs = 6000
            val pcm = buildSirenPcm(sampleRate, durationMs)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            alarmTrack = track
            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                Handler(Looper.getMainLooper()).postDelayed({
                    stopOwnerAlarm()
                }, durationMs.toLong())
            } catch (err: Exception) {
                Log.e(TAG, "Falha ao tocar alarme do dono", err)
                stopOwnerAlarm()
            }
        }
    }

    private fun stopOwnerAlarm() {
        alarmTrack?.let { track ->
            try {
                track.stop()
            } catch (_: Exception) {
            }
            track.release()
        }
        alarmTrack = null
        previousAlarmVolume?.let { prev ->
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, prev, 0)
        }
        previousAlarmVolume = null
    }

    private fun buildSirenPcm(sampleRate: Int, durationMs: Int): ShortArray {
        val totalSamples = (durationMs / 1000.0 * sampleRate).toInt()
        val pcm = ShortArray(totalSamples)
        val minFreq = 700.0
        val maxFreq = 1200.0
        val cycleSec = 1.2
        var phase = 0.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val pos = (t % cycleSec) / cycleSec
            val tri = if (pos < 0.5) pos * 2.0 else (1.0 - pos) * 2.0
            val freq = minFreq + (maxFreq - minFreq) * tri
            phase += 2.0 * Math.PI * freq / sampleRate
            val sample = sin(phase) * 0.8
            pcm[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return pcm
    }
}
