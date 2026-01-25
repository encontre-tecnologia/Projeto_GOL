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
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import br.com.gui.carlembrete.MainActivity

class NotificacaoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(EXTRA_TITULO) ?: return
        val descricao = intent.getStringExtra(EXTRA_DESCRICAO) ?: ""
        val lembreteId = intent.getStringExtra(EXTRA_ID) ?: titulo

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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(descricao)
            .setStyle(NotificationCompat.BigTextStyle().bigText(descricao))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(lembreteId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "lembretes_channel"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITULO = "extra_titulo"
        const val EXTRA_DESCRICAO = "extra_descricao"
    }
}

object NotificacaoHelper {
    private const val PREFS_NAME = "notificacoes_prefs"
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
