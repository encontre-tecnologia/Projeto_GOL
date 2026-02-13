package br.com.gui.carlembrete

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val descricao = intent.getStringExtra(EXTRA_DESCRICAO) ?: ""
        val lembreteId = intent.getStringExtra(EXTRA_ID) ?: titulo
        val carroId = intent.getStringExtra(EXTRA_CARRO_ID)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            lembreteId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.logonotificacao)
            .setContentTitle(titulo)
            .setContentText(descricao)
            .setStyle(NotificationCompat.BigTextStyle().bigText(descricao))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(lembreteId.hashCode(), notification)
        NotificacaoHelper.registrarNotificacaoDisparada(context, lembreteId, titulo, descricao, carroId)
    }

    companion object {
        const val CHANNEL_ID = "lembretes_channel"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRICAO = "extra_descricao"
        const val EXTRA_CARRO_ID = "extra_carro_id"
    }
}

object NotificacaoHelper {
    private const val PREFS_NAME = "notificacoes_prefs"
    private const val PREFS_HISTORY_KEY = "historico_disparadas_v1"
    private const val HISTORY_LIMIT = 100
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de manutenção",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Lembretes para serviços e avisos do carro"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun agendarNotificacao(context: Context, lembrete: Lembrete, hora: String) {
        val triggerAt = calcularMillis(lembrete.dataLimite, hora) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Ative alarmes exatos para receber notificações do carro.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return
        }

        salvarHora(context, lembrete.id, hora)
        val intent = Intent(context, NotificacaoReceiver::class.java).apply {
            putExtra(NotificacaoReceiver.EXTRA_ID, lembrete.id)
            putExtra(NotificacaoReceiver.EXTRA_TITULO, lembrete.titulo)
            putExtra(NotificacaoReceiver.EXTRA_CARRO_ID, lembrete.carroId)
            putExtra(
                NotificacaoReceiver.EXTRA_DESCRICAO,
                "Atenção! O prazo de ${lembrete.titulo} vence em ${lembrete.dataLimite}."
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lembrete.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (ex: SecurityException) {
            Toast.makeText(context, "Permissão de alarme exato negada. Ajuste nas configurações.", Toast.LENGTH_LONG).show()
        }
    }

    fun cancelarNotificacao(context: Context, lembreteId: String) {
        removerHora(context, lembreteId)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificacaoReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            lembreteId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
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
        hora: String = "09:00"
    ) {
        val dataFormatada = data.format(dateFormatter)
        val triggerAt = calcularMillis(dataFormatada, hora) ?: return
        if (triggerAt <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Ative alarmes exatos para receber notificacoes do carro.", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                this.data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
            return
        }

        salvarHora(context, id, hora)
        val intent = Intent(context, NotificacaoReceiver::class.java).apply {
            putExtra(NotificacaoReceiver.EXTRA_ID, id)
            putExtra(NotificacaoReceiver.EXTRA_TITULO, titulo)
            putExtra(NotificacaoReceiver.EXTRA_DESCRICAO, descricao)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (ex: SecurityException) {
            Toast.makeText(context, "Permissao de alarme exato negada. Ajuste nas configuracoes.", Toast.LENGTH_LONG).show()
        }
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
