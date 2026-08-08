package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import java.io.File
import java.text.Normalizer
import java.util.UUID

private const val VEHICLE_SHARE_VERSION = 1
private const val VEHICLE_SHARE_MIME = "application/vnd.zellu.vehicle"

private data class VehicleTransferPayload(
    val version: Int = VEHICLE_SHARE_VERSION,
    val exportedBy: String = "Zellu",
    val vehicle: CarroInfo,
    val reminders: List<Lembrete>,
    val fuelRecords: List<Abastecimento>
)

data class ImportedVehicleTransfer(
    val vehicle: CarroInfo,
    val reminders: List<Lembrete>,
    val fuelRecords: List<Abastecimento>
)

fun exportVehicleToOtherDevice(
    context: Context,
    vehicle: CarroInfo,
    reminders: List<Lembrete>,
    fuelRecords: List<Abastecimento>
): Boolean = runCatching {
    val payload = VehicleTransferPayload(
        vehicle = vehicle.copy(fotoNome = null),
        reminders = reminders.map { it.copy(fotoPath = null, fotoAvisoNome = null) },
        fuelRecords = fuelRecords
    )
    val file = File(context.cacheDir, "veiculo_${safeVehicleTransferFileName(vehicle.nome)}.zellu")
    file.writeText(Gson().toJson(payload), Charsets.UTF_8)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = VEHICLE_SHARE_MIME
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Veiculo Zellu - ${vehicle.nome}")
        putExtra(
            Intent.EXTRA_TEXT,
            "Arquivo de veiculo do Zellu: ${vehicle.nome}. Abra com o Zellu para importar avisos e registros."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar veiculo para outro aparelho"))
}.isSuccess

fun importVehicleTransferFromUri(context: Context, uri: Uri): ImportedVehicleTransfer? {
    val raw = runCatching {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }.getOrNull().orEmpty()
    if (raw.isBlank()) return null

    val payload = runCatching {
        Gson().fromJson(raw, VehicleTransferPayload::class.java)
    }.getOrNull() ?: return null
    if (payload.version > VEHICLE_SHARE_VERSION || payload.vehicle.id.isBlank()) return null

    val newVehicleId = UUID.randomUUID().toString()
    val newVehicle = payload.vehicle.copy(id = newVehicleId, fotoNome = null)
    val newReminders = payload.reminders.map { reminder ->
        reminder.copy(
            id = UUID.randomUUID().toString(),
            carroId = newVehicleId,
            contatoId = null,
            fotoPath = null,
            fotoAvisoNome = null
        )
    }
    val newFuelRecords = payload.fuelRecords.map { fuel ->
        fuel.copy(id = UUID.randomUUID().toString(), carroId = newVehicleId)
    }

    return ImportedVehicleTransfer(newVehicle, newReminders, newFuelRecords)
}

private fun safeVehicleTransferFileName(name: String): String {
    val normalized = Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
    return normalized.ifBlank { "veiculo" }
}
