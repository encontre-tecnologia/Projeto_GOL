package br.com.gui.carlembrete

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.gui.carlembrete.NotificacaoReceiver.Companion.CHANNEL_ID
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import br.com.gui.carlembrete.MainActivity

data class NotificacaoDisparada(
    val id: String,
    val titulo: String,
    val descricao: String,
    val carroId: String?,
    val timestamp: Long
)

class NotificacaoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PARKING_NOTIFICATION_DISMISSED) {
            restoreParkingOngoingNotificationIfNeeded(context, intent)
            return
        }

        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val descricao = intent.getStringExtra(EXTRA_DESCRICAO) ?: ""
        val lembreteId = intent.getStringExtra(EXTRA_ID) ?: titulo
        if (!NotificacaoHelper.deveDispararAgora(context, lembreteId)) {
            Log.w(NotificacaoHelper.TAG_NOTIF, "notificacao suprimida por duplicidade id=$lembreteId")
            return
        }
        val carroId = intent.getStringExtra(EXTRA_CARRO_ID)
        val nomeVeiculo = carroId?.let { id ->
            BancoDeDados.carregarCarros(context).orEmpty()
                .firstOrNull { it.id == id }
                ?.nome
                ?.ifBlank { null }
        }
        val descricaoComContexto = if (!nomeVeiculo.isNullOrBlank()) {
            "$descricao\n${trNow("Veículo", "Vehicle")}: $nomeVeiculo"
        } else {
            descricao
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_LEMBRETE_ID, lembreteId)
            putExtra(EXTRA_OPEN_LEMBRETE_CARRO_ID, carroId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            lembreteId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.e("NotificacaoHelper", "notificacao bloqueada pelo sistema (areNotificationsEnabled=false)")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("NotificacaoHelper", "notificacao bloqueada: POST_NOTIFICATIONS nao concedida")
            return
        }

        val tituloComContexto = if (!nomeVeiculo.isNullOrBlank()) {
            "$titulo • $nomeVeiculo"
        } else {
            titulo
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setColor(NotificacaoHelper.notificationAccentColor(context))
            .setContentTitle(tituloComContexto)
            .setContentText(descricaoComContexto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(descricaoComContexto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(lembreteId.hashCode(), notification)
        NotificacaoHelper.registrarNotificacaoDisparada(
            context = context,
            id = lembreteId,
            titulo = tituloComContexto,
            descricao = descricaoComContexto,
            carroId = carroId
        )

        val isRollingStep = intent.getBooleanExtra(EXTRA_IS_ROLLING_STEP, false)
        Log.i(
            TAG_REPEAT,
            "onReceive id=$lembreteId rollingStep=$isRollingStep titulo='${titulo.take(40)}'"
        )
        if (isRollingStep) {
            val baseReminderId = intent.getStringExtra(EXTRA_BASE_REMINDER_ID).orEmpty()
            val dueDateText = intent.getStringExtra(EXTRA_DUE_DATE).orEmpty()
            val hora = intent.getStringExtra(EXTRA_HORA).orEmpty().ifBlank { "09:00" }
            val tituloBase = intent.getStringExtra(EXTRA_TITULO_BASE).orEmpty().ifBlank { titulo }
            val tipoLabel = intent.getStringExtra(EXTRA_TIPO_LABEL).orEmpty()
            if (baseReminderId.isNotBlank() && dueDateText.isNotBlank()) {
                val lembreteAtivo = BancoDeDados.carregarLembretes(context)
                    .firstOrNull { it.id == baseReminderId && !isLembreteRealizado(it) }
                if (lembreteAtivo != null) {
                    val dataVencimento = runCatching {
                        LocalDate.parse(dueDateText, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }.getOrNull()
                    if (dataVencimento != null) {
                        NotificacaoHelper.agendarProximaNotificacaoEtapas(
                            context = context,
                            baseReminderId = baseReminderId,
                            tituloBase = tituloBase,
                            tipoLabel = tipoLabel.ifBlank { lembreteAtivo.tipo.label },
                            dataVencimento = dataVencimento,
                            hora = hora,
                            carroId = carroId,
                            referencia = LocalDate.now().plusDays(1)
                        )
                    }
                }
            }
        } else {
            processarRecorrenciaSeNecessario(context, lembreteId)
        }
    }

    private fun restoreParkingOngoingNotificationIfNeeded(context: Context, intent: Intent) {
        if (!hasNotificationPermission(context)) return
        if (AppPreferences.isParkingFinalized(context)) return

        val location = AppPreferences.getParkedLocation(context) ?: return
        val isBike = if (intent.hasExtra(EXTRA_PARKING_IS_BIKE)) {
            intent.getBooleanExtra(EXTRA_PARKING_IS_BIKE, false)
        } else {
            inferBikeContextFromLastVehicle(context)
        }

        createParkingNotificationChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_AONDE_PAREI, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            90422,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            90423,
            Intent(context, NotificacaoReceiver::class.java).apply {
                action = ACTION_PARKING_NOTIFICATION_DISMISSED
                putExtra(EXTRA_PARKING_IS_BIKE, isBike)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sinceText = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("pt", "BR"))
            .format(java.util.Date(location.timeMillis))
        val title = if (isBike) trNow("Parada em andamento", "Stop in progress") else trNow("Estacionamento em andamento", "Parking in progress")
        val tapText = if (isBike) trNow("Toque quando encontrar a bike.", "Tap when you find the bike.") else trNow("Toque quando encontrar o carro.", "Tap when you find the car.")
        val finishText = if (isBike) trNow("\"Encontrei minha bike\"", "\"I found my bike\"") else trNow("\"Encontrei meu carro\"", "\"I found my car\"")

        val notification = NotificationCompat.Builder(context, PARKING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_notification)
            .setColor(NotificacaoHelper.notificationAccentColor(context))
            .setContentTitle(title)
            .setContentText("$tapText ${trNow("Desde", "Since")}: $sinceText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    trNow("Local marcado com sucesso. Esta notificacao fica ativa ate voce tocar em $finishText no app.", "Location saved successfully. This notification stays active until you tap $finishText in the app.")
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .build()
            .apply { flags = flags or Notification.FLAG_NO_CLEAR }

        NotificationManagerCompat.from(context).notify(PARKING_NOTIFICATION_ID, notification)
    }

    private fun createParkingNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            PARKING_NOTIFICATION_CHANNEL_ID,
            trNow("Estacionamento", "Parking"),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = trNow("Lembrete de estacionamento em andamento", "Parking in-progress reminder")
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun inferBikeContextFromLastVehicle(context: Context): Boolean {
        val lastVehicleId = AppPreferences.getLastSelectedCarId(context) ?: return false
        val vehicle = BancoDeDados.carregarCarros(context).orEmpty().firstOrNull { it.id == lastVehicleId } ?: return false
        return vehicle.tipoVeiculo == TipoVeiculo.BICICLETA || vehicle.tipoVeiculo == TipoVeiculo.BIKE_ELETRICA
    }

    private fun processarRecorrenciaSeNecessario(context: Context, lembreteId: String) {
        if (lembreteId.startsWith("INSTANT_")) {
            Log.d(TAG_REPEAT, "recorrencia ignorada: id instantaneo=$lembreteId")
            return
        }
        val config = NotificacaoHelper.obterRecorrencia(context, lembreteId)
        if (config == null) {
            Log.d(TAG_REPEAT, "recorrencia ausente para id=$lembreteId")
            return
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val lembretes = BancoDeDados.carregarLembretes(context).toMutableList()
        val idx = lembretes.indexOfFirst { it.id == lembreteId }
        if (idx < 0) {
            Log.w(TAG_REPEAT, "lembrete nao encontrado para recorrencia id=$lembreteId; limpando config")
            NotificacaoHelper.removerRecorrencia(context, lembreteId)
            return
        }
        val lembreteAtual = lembretes[idx]
        if (isLembreteRealizado(lembreteAtual)) {
            Log.i(TAG_REPEAT, "lembrete realizado; removendo recorrencia id=$lembreteId")
            NotificacaoHelper.removerRecorrencia(context, lembreteId)
            return
        }
        val dataBase = runCatching {
            LocalDate.parse(lembreteAtual.dataLimite, formatter)
        }.getOrNull() ?: LocalDate.now()
        val proximaData = when (config.unit) {
            NotificacaoHelper.REC_UNIT_DAY -> dataBase.plusDays(config.interval.toLong())
            NotificacaoHelper.REC_UNIT_WEEK -> dataBase.plusWeeks(config.interval.toLong())
            NotificacaoHelper.REC_UNIT_MONTH -> dataBase.plusMonths(config.interval.toLong())
            NotificacaoHelper.REC_UNIT_YEAR -> dataBase.plusYears(config.interval.toLong())
            else -> null
        } ?: return
        if (!proximaData.isAfter(dataBase)) {
            Log.w(
                TAG_REPEAT,
                "proxima data invalida id=$lembreteId dataBase=$dataBase proxima=$proximaData unit=${config.unit} intervalo=${config.interval}"
            )
            return
        }
        Log.i(
            TAG_REPEAT,
            "recorrencia processada id=$lembreteId unit=${config.unit} intervalo=${config.interval} de=${dataBase.format(formatter)} para=${proximaData.format(formatter)}"
        )
        val atualizado = lembreteAtual.copy(dataLimite = proximaData.format(formatter))
        lembretes[idx] = atualizado
        BancoDeDados.salvarLembretes(context, lembretes)
        NotificacaoHelper.agendarNotificacao(context, atualizado, atualizado.horaAviso)
        Log.i(
            TAG_REPEAT,
            "recorrencia reagendada id=$lembreteId novaData=${atualizado.dataLimite} hora=${atualizado.horaAviso}"
        )
    }

    companion object {
        const val TAG_REPEAT = "ReminderRepeat"
        const val CHANNEL_ID = "lembretes_channel"
        const val ACTION_PARKING_NOTIFICATION_DISMISSED = "br.com.gui.carlembrete.action.PARKING_NOTIFICATION_DISMISSED"
        const val EXTRA_PARKING_IS_BIKE = "extra_parking_is_bike"
        const val PARKING_NOTIFICATION_CHANNEL_ID = "parking_ongoing_channel"
        const val PARKING_NOTIFICATION_ID = 90421
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRICAO = "extra_descricao"
        const val EXTRA_CARRO_ID = "extra_carro_id"
        const val EXTRA_IS_ROLLING_STEP = "extra_is_rolling_step"
        const val EXTRA_BASE_REMINDER_ID = "extra_base_reminder_id"
        const val EXTRA_DUE_DATE = "extra_due_date"
        const val EXTRA_HORA = "extra_hora"
        const val EXTRA_TITULO_BASE = "extra_titulo_base"
        const val EXTRA_TIPO_LABEL = "extra_tipo_label"
    }
}

object NotificacaoHelper {
    const val TAG_NOTIF = "NotificacaoHelper"
    private const val PREFS_NAME = "notificacoes_prefs"
    private const val PREFS_LAST_DISPATCH_PREFIX = "last_dispatch_"
    private const val DISPATCH_DEBOUNCE_MS = 45_000L
    data class RecorrenciaConfig(val unit: String, val interval: Int)
    const val REC_UNIT_DAY = "DAY"
    const val REC_UNIT_WEEK = "WEEK"
    const val REC_UNIT_MONTH = "MONTH"
    const val REC_UNIT_YEAR = "YEAR"
    private const val REC_PREFIX_UNIT = "rec_unit_"
    private const val REC_PREFIX_INTERVAL = "rec_interval_"
    private const val PREFS_HISTORY_KEY = "historico_disparadas_v1"
    private const val HISTORY_LIMIT = 100
    private const val PAST_TRIGGER_GRACE_MS = 60_000L
    private const val IMMEDIATE_TRIGGER_DELAY_MS = 5_000L
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val tiposAvisoEtapas = setOf(
        TipoManutencao.SEGURO,
        TipoManutencao.LICENCIAMENTO,
        TipoManutencao.IPVA
    )
    private val etapasAntesDoVencimento = listOf(5, 0)
    private const val MAX_ALERTAS_POS_VENCIMENTO = 365

    fun notificationAccentColor(context: Context): Int {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (nightMode == Configuration.UI_MODE_NIGHT_YES) 0xFFFFFFFF.toInt() else 0xFF2563EB.toInt()
    }

    private fun podeUsarAlarmeExato(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun nomeVeiculoParaNotificacao(context: Context, carroId: String?): String? {
        if (carroId.isNullOrBlank()) return null
        val carro = BancoDeDados.carregarCarros(context).orEmpty().firstOrNull { it.id == carroId } ?: return null
        return carro.nome
            .ifBlank { "${carro.marca} ${carro.modelo}".trim() }
            .ifBlank { null }
    }

    private fun tituloComVeiculo(context: Context, lembrete: Lembrete): String {
        val nomeVeiculo = nomeVeiculoParaNotificacao(context, lembrete.carroId)
        return if (nomeVeiculo.isNullOrBlank()) {
            lembrete.titulo
        } else {
            "$nomeVeiculo: ${lembrete.titulo}"
        }
    }

    private fun agendarAlarmManager(
        context: Context,
        alarmManager: AlarmManager,
        triggerAt: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (podeUsarAlarmeExato(alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                Toast.makeText(
                    context,
                    "Alarme exato nao esta liberado. O aviso foi salvo e a notificacao pode variar alguns minutos.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (ex: SecurityException) {
            Toast.makeText(
                context,
                "Nao foi possivel agendar a notificacao. Verifique as permissoes do app.",
                Toast.LENGTH_LONG
            ).show()
        } catch (ex: IllegalStateException) {
            android.util.Log.e(
                "NotificacaoHelper",
                "Limite de alarmes do Android atingido. Alarme ignorado para evitar crash.",
                ex
            )
            Toast.makeText(
                context,
                "Muitos lembretes ativos no momento. Alguns alertas podem ser reagendados depois.",
                Toast.LENGTH_LONG
            ).show()
        } catch (ex: Exception) {
            android.util.Log.e(
                "NotificacaoHelper",
                "Falha ao agendar alarme. Operacao ignorada para manter app estavel.",
                ex
            )
        }
    }

    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de manutenÃ§Ã£o",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes para serviÃ§os e avisos do carro"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun agendarNotificacao(context: Context, lembrete: Lembrete, hora: String) {
        cancelarNotificacoesEmEtapas(context, lembrete.id)
        val tituloCompleto = tituloComVeiculo(context, lembrete)
        Log.i(
            NotificacaoReceiver.TAG_REPEAT,
            "agendarNotificacao id=${lembrete.id} tipo=${lembrete.tipo.name} data=${lembrete.dataLimite} hora=$hora"
        )

        if (lembrete.tipo in tiposAvisoEtapas) {
            val dataVencimento = runCatching {
                LocalDate.parse(lembrete.dataLimite, dateFormatter)
            }.getOrNull()
            if (dataVencimento != null) {
                agendarProximaNotificacaoEtapas(
                    context = context,
                    baseReminderId = lembrete.id,
                    tituloBase = tituloCompleto,
                    tipoLabel = lembrete.tipo.label,
                    dataVencimento = dataVencimento,
                    hora = hora,
                    carroId = lembrete.carroId,
                    referencia = LocalDate.now()
                )
            }
            return
        }

        agendarNotificacaoUnica(
            context = context,
            id = lembrete.id,
            titulo = tituloCompleto,
            descricao = "Lembrete agendado para ${lembrete.dataLimite} as $hora. Abra o app para revisar e concluir.",
            data = lembrete.dataLimite,
            hora = hora,
            carroId = lembrete.carroId
        )
    }

    fun cancelarNotificacao(context: Context, lembreteId: String) {
        cancelarNotificacaoPorId(context, lembreteId)
        cancelarNotificacoesEmEtapas(context, lembreteId)
        removerRecorrencia(context, lembreteId)
    }

    fun salvarRecorrencia(context: Context, lembreteId: String, unit: String, interval: Int) {
        if (lembreteId.isBlank() || interval <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$REC_PREFIX_UNIT$lembreteId", unit)
            .putInt("$REC_PREFIX_INTERVAL$lembreteId", interval)
            .apply()
        Log.i(
            NotificacaoReceiver.TAG_REPEAT,
            "recorrencia salva id=$lembreteId unit=$unit interval=$interval"
        )
    }

    fun obterRecorrencia(context: Context, lembreteId: String): RecorrenciaConfig? {
        if (lembreteId.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val unit = prefs.getString("$REC_PREFIX_UNIT$lembreteId", null)?.trim().orEmpty()
        val interval = prefs.getInt("$REC_PREFIX_INTERVAL$lembreteId", 0)
        if (unit.isBlank() || interval <= 0) return null
        Log.d(
            NotificacaoReceiver.TAG_REPEAT,
            "recorrencia carregada id=$lembreteId unit=$unit interval=$interval"
        )
        return RecorrenciaConfig(unit = unit, interval = interval)
    }

    fun removerRecorrencia(context: Context, lembreteId: String) {
        if (lembreteId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$REC_PREFIX_UNIT$lembreteId")
            .remove("$REC_PREFIX_INTERVAL$lembreteId")
            .apply()
        Log.i(NotificacaoReceiver.TAG_REPEAT, "recorrencia removida id=$lembreteId")
    }

    fun reagendarExistentes(context: Context, lembretes: List<Lembrete>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        lembretes.forEach { lembrete ->
            val hora = prefs.getString(lembrete.id, null) ?: return@forEach
            agendarNotificacao(context, lembrete, hora)
        }
    }

    fun dispararNotificacaoInstantanea(context: Context, titulo: String, descricao: String) {
        val intent = Intent(context, NotificacaoReceiver::class.java).apply {
            putExtra(NotificacaoReceiver.EXTRA_ID, "INSTANT_${System.currentTimeMillis()}")
            putExtra(NotificacaoReceiver.EXTRA_TITULO, titulo)
            putExtra(NotificacaoReceiver.EXTRA_DESCRICAO, descricao)
        }
        context.sendBroadcast(intent)
    }

    fun agendarNotificacaoPorData(
        context: Context,
        id: String,
        titulo: String,
        descricao: String,
        data: LocalDate,
        hora: String = "09:00",
        carroId: String? = null
    ) {
        val dataFormatada = data.format(dateFormatter)
        agendarNotificacaoUnica(
            context = context,
            id = id,
            titulo = titulo,
            descricao = descricao,
            data = dataFormatada,
            hora = hora,
            carroId = carroId
        )
    }

    fun agendarProximaNotificacaoEtapas(
        context: Context,
        baseReminderId: String,
        tituloBase: String,
        tipoLabel: String,
        dataVencimento: LocalDate,
        hora: String,
        carroId: String?,
        referencia: LocalDate = LocalDate.now()
    ) {
        var referenciaAtual = referencia
        var dataAlerta = calcularProximaDataEtapa(dataVencimento, referenciaAtual) ?: return
        val now = System.currentTimeMillis()
        var tentativas = 0
        while (tentativas <= MAX_ALERTAS_POS_VENCIMENTO + etapasAntesDoVencimento.size) {
            val trigger = calcularMillis(dataAlerta.format(dateFormatter), hora)
            if (trigger == null || trigger > now) break
            referenciaAtual = dataAlerta.plusDays(1)
            dataAlerta = calcularProximaDataEtapa(dataVencimento, referenciaAtual) ?: return
            tentativas++
        }
        val triggerDaEtapa = calcularMillis(dataAlerta.format(dateFormatter), hora)
        if (triggerDaEtapa != null && triggerDaEtapa <= now) {
            Log.w(
                TAG_NOTIF,
                "etapa ignorada por estar no passado baseId=$baseReminderId vencimento=${dataVencimento.format(dateFormatter)} dataAlerta=${dataAlerta.format(dateFormatter)} hora=$hora"
            )
            return
        }
        val idEtapa = idEtapaPorData(baseReminderId, dataVencimento, dataAlerta) ?: return
        val diasAntes = java.time.temporal.ChronoUnit.DAYS.between(dataAlerta, dataVencimento).toInt()
        val diasAtraso = java.time.temporal.ChronoUnit.DAYS.between(dataVencimento, dataAlerta).toInt()
        val tituloEtapa = when {
            diasAntes > 0 -> "$tituloBase vence em $diasAntes dias"
            diasAntes == 0 -> "$tituloBase vence hoje"
            else -> "$tituloBase vencido"
        }
        val descricaoEtapa = when {
            diasAntes > 0 -> "Faltam $diasAntes dias. Vencimento em ${dataVencimento.format(dateFormatter)} as $hora."
            diasAntes == 0 -> "Vence hoje (${dataVencimento.format(dateFormatter)}) as $hora. Regularize para evitar pendencias."
            else -> "Vencido ha $diasAtraso dia(s). Regularize assim que possivel no app."
        }
        agendarNotificacaoUnica(
            context = context,
            id = idEtapa,
            titulo = tituloEtapa,
            descricao = descricaoEtapa,
            data = dataAlerta.format(dateFormatter),
            hora = hora,
            carroId = carroId,
            isRollingStep = true,
            baseReminderId = baseReminderId,
            dueDate = dataVencimento.format(dateFormatter),
            tituloBase = tituloBase,
            tipoLabel = tipoLabel
        )
    }

    private fun agendarNotificacaoUnica(
        context: Context,
        id: String,
        titulo: String,
        descricao: String,
        data: String,
        hora: String,
        carroId: String?,
        isRollingStep: Boolean = false,
        baseReminderId: String? = null,
        dueDate: String? = null,
        tituloBase: String? = null,
        tipoLabel: String? = null
    ) {
        val now = System.currentTimeMillis()
        val triggerAtFinal = calcularTriggerFuturo(context, id, data, hora, isRollingStep, now) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        salvarHora(context, id, hora)
        val intent = Intent(context, NotificacaoReceiver::class.java).apply {
            putExtra(NotificacaoReceiver.EXTRA_ID, id)
            putExtra(NotificacaoReceiver.EXTRA_TITULO, titulo)
            putExtra(NotificacaoReceiver.EXTRA_DESCRICAO, descricao)
            putExtra(NotificacaoReceiver.EXTRA_CARRO_ID, carroId)
            putExtra(NotificacaoReceiver.EXTRA_IS_ROLLING_STEP, isRollingStep)
            if (isRollingStep) {
                putExtra(NotificacaoReceiver.EXTRA_BASE_REMINDER_ID, baseReminderId)
                putExtra(NotificacaoReceiver.EXTRA_DUE_DATE, dueDate)
                putExtra(NotificacaoReceiver.EXTRA_HORA, hora)
                putExtra(NotificacaoReceiver.EXTRA_TITULO_BASE, tituloBase)
                putExtra(NotificacaoReceiver.EXTRA_TIPO_LABEL, tipoLabel)
            }
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d(TAG_NOTIF, "agendar id=$id data=$data hora=$hora triggerAt=$triggerAtFinal now=$now")
        agendarAlarmManager(context, alarmManager, triggerAtFinal, pendingIntent)
    }

    private fun calcularTriggerFuturo(
        context: Context,
        id: String,
        data: String,
        hora: String,
        isRollingStep: Boolean,
        now: Long
    ): Long? {
        val triggerAt = calcularMillis(data, hora) ?: return null
        if (triggerAt > now) return triggerAt

        val atrasoMs = now - triggerAt
        if (!isRollingStep && atrasoMs in 0..PAST_TRIGGER_GRACE_MS) {
            Log.w(
                TAG_NOTIF,
                "trigger ajustado para disparo imediato id=$id data=$data hora=$hora atrasoMs=$atrasoMs"
            )
            return now + IMMEDIATE_TRIGGER_DELAY_MS
        }

        val recorrencia = if (isRollingStep) null else obterRecorrencia(context, id)
        val proximoTrigger = recorrencia?.let { calcularProximoTriggerRecorrente(data, hora, it, now) }
        if (proximoTrigger != null) {
            Log.w(
                TAG_NOTIF,
                "trigger no passado; recorrencia agendada para proxima ocorrencia id=$id data=$data hora=$hora atrasoMs=$atrasoMs"
            )
            return proximoTrigger
        }

        Log.w(
            TAG_NOTIF,
            "trigger ignorado por estar no passado id=$id data=$data hora=$hora atrasoMs=$atrasoMs"
        )
        return null
    }

    private fun calcularProximoTriggerRecorrente(
        data: String,
        hora: String,
        recorrencia: RecorrenciaConfig,
        now: Long
    ): Long? {
        var proximaData = runCatching { LocalDate.parse(data, dateFormatter) }.getOrNull() ?: return null
        repeat(500) {
            proximaData = when (recorrencia.unit) {
                REC_UNIT_DAY -> proximaData.plusDays(recorrencia.interval.toLong())
                REC_UNIT_WEEK -> proximaData.plusWeeks(recorrencia.interval.toLong())
                REC_UNIT_MONTH -> proximaData.plusMonths(recorrencia.interval.toLong())
                REC_UNIT_YEAR -> proximaData.plusYears(recorrencia.interval.toLong())
                else -> return null
            }
            val trigger = calcularMillis(proximaData.format(dateFormatter), hora) ?: return null
            if (trigger > now) return trigger
        }
        return null
    }

    private fun cancelarNotificacoesEmEtapas(context: Context, lembreteId: String) {
        etapasAntesDoVencimento.forEach { diasAntes ->
            cancelarNotificacaoPorId(context, "${lembreteId}_stage_$diasAntes")
        }
        (1..MAX_ALERTAS_POS_VENCIMENTO).forEach { diaAtraso ->
            cancelarNotificacaoPorId(context, "${lembreteId}_overdue_$diaAtraso")
        }
    }

    private fun calcularProximaDataEtapa(dataVencimento: LocalDate, referencia: LocalDate): LocalDate? {
        val etapaCincoDias = dataVencimento.minusDays(5)
        return when {
            referencia.isBefore(etapaCincoDias) || referencia.isEqual(etapaCincoDias) -> etapaCincoDias
            referencia.isBefore(dataVencimento) || referencia.isEqual(dataVencimento) -> dataVencimento
            referencia.isAfter(dataVencimento.plusDays(MAX_ALERTAS_POS_VENCIMENTO.toLong())) -> null
            else -> referencia
        }
    }

    private fun idEtapaPorData(baseReminderId: String, dataVencimento: LocalDate, dataAlerta: LocalDate): String? {
        val diasAntes = java.time.temporal.ChronoUnit.DAYS.between(dataAlerta, dataVencimento).toInt()
        return when {
            diasAntes == 5 -> "${baseReminderId}_stage_5"
            diasAntes == 0 -> "${baseReminderId}_stage_0"
            diasAntes < 0 -> "${baseReminderId}_overdue_${kotlin.math.abs(diasAntes)}"
            else -> null
        }
    }

    private fun cancelarNotificacaoPorId(context: Context, id: String) {
        removerHora(context, id)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificacaoReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun registrarNotificacaoDisparada(
        context: Context,
        id: String,
        titulo: String,
        descricao: String,
        carroId: String?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val atual = prefs.getString(PREFS_HISTORY_KEY, null)
        val array = runCatching { JSONArray(atual) }.getOrElse { JSONArray() }
        val item = JSONObject().apply {
            put("id", id)
            put("titulo", titulo)
            put("descricao", descricao)
            put("carroId", carroId ?: "")
            put("timestamp", System.currentTimeMillis())
        }
        array.put(item)
        val enxuto = JSONArray()
        val inicio = (array.length() - HISTORY_LIMIT).coerceAtLeast(0)
        for (i in inicio until array.length()) {
            enxuto.put(array.getJSONObject(i))
        }
        prefs.edit().putString(PREFS_HISTORY_KEY, enxuto.toString()).apply()
    }

    fun registrarNotificacaoDisparadaUnica(
        context: Context,
        id: String,
        titulo: String,
        descricao: String,
        carroId: String?
    ) {
        removerNotificacoesPorId(context, id)
        registrarNotificacaoDisparada(
            context = context,
            id = id,
            titulo = titulo,
            descricao = descricao,
            carroId = carroId
        )
    }

    fun carregarNotificacoesDisparadas(context: Context, carroId: String? = null): List<NotificacaoDisparada> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(PREFS_HISTORY_KEY, null)) }.getOrElse { JSONArray() }
        val lista = mutableListOf<NotificacaoDisparada>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val itemCarroId = obj.optString("carroId").ifBlank { null }
            if (carroId != null && itemCarroId != carroId) continue
            lista += NotificacaoDisparada(
                id = obj.optString("id"),
                titulo = obj.optString("titulo"),
                descricao = obj.optString("descricao"),
                carroId = itemCarroId,
                timestamp = obj.optLong("timestamp", 0L)
            )
        }
        return lista.sortedByDescending { it.timestamp }
    }

    fun limparNotificacoesDisparadas(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(PREFS_HISTORY_KEY)
            .apply()
    }

    fun removerNotificacaoDisparada(context: Context, id: String, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(PREFS_HISTORY_KEY, null)) }.getOrElse { JSONArray() }
        val atualizado = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val objId = obj.optString("id")
            val objTimestamp = obj.optLong("timestamp", 0L)
            if (objId == id && objTimestamp == timestamp) continue
            atualizado.put(obj)
        }
        prefs.edit().putString(PREFS_HISTORY_KEY, atualizado.toString()).apply()
    }

    fun removerNotificacoesPorId(context: Context, id: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = runCatching { JSONArray(prefs.getString(PREFS_HISTORY_KEY, null)) }.getOrElse { JSONArray() }
        val atualizado = JSONArray()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val objId = obj.optString("id")
            if (objId == id) continue
            atualizado.put(obj)
        }
        prefs.edit()
            .putString(PREFS_HISTORY_KEY, atualizado.toString())
            .remove("$PREFS_LAST_DISPATCH_PREFIX$id")
            .apply()
    }

    fun deveDispararAgora(context: Context, id: String): Boolean {
        if (id.isBlank()) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val key = "$PREFS_LAST_DISPATCH_PREFIX$id"
        val last = prefs.getLong(key, 0L)
        if (last > 0L && now - last < DISPATCH_DEBOUNCE_MS) {
            return false
        }
        prefs.edit().putLong(key, now).apply()
        return true
    }

    fun registrarListenerHistorico(
        context: Context,
        onChanged: () -> Unit
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREFS_HISTORY_KEY) onChanged()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun removerListenerHistorico(
        context: Context,
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun salvarHora(context: Context, lembreteId: String, hora: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(lembreteId, hora)
            .apply()
    }

    private fun removerHora(context: Context, lembreteId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(lembreteId)
            .apply()
    }

    private fun calcularMillis(data: String, hora: String): Long? {
        return try {
            val date = LocalDate.parse(data, dateFormatter)
            val partes = hora.split(":")
            val time = LocalTime.of(
                partes.getOrNull(0)?.toIntOrNull() ?: 9,
                partes.getOrNull(1)?.toIntOrNull() ?: 0
            )
            date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}


