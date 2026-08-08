package br.com.gui.carlembrete

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

private const val TAG_VEHICLE_PHOTO = "VehiclePhotoStore"
private const val PREFIX = "veiculo_foto_"

/**
 * Guarda a foto do veiculo como arquivo em filesDir.
 *
 * Nao guardamos o content:// da galeria porque essa permissao pode ser revogada e a
 * imagem desaparece da tela sem aviso. Copiando o arquivo, a foto sobrevive a
 * reinstalacao da galeria, troca de app de fotos e restauracao de backup.
 *
 * A imagem e reduzida antes de salvar: uma foto de celular tem 3 a 8 MB e o topo da
 * home usa no maximo ~1080px de largura.
 */
object VehiclePhotoStore {

    private const val MAX_LARGURA = 1440
    private const val QUALIDADE_JPEG = 88

    fun arquivoDe(context: Context, fotoNome: String?): File? {
        if (fotoNome.isNullOrBlank()) return null
        val arquivo = File(context.filesDir, fotoNome)
        return arquivo.takeIf { it.exists() && it.length() > 0 }
    }

    /**
     * Copia e reduz a imagem escolhida. Devolve o nome do arquivo para gravar em
     * [CarroInfo.fotoNome], ou null se nao deu para ler a origem.
     */
    fun salvar(context: Context, origem: Uri, carroId: String): String? {
        return runCatching {
            // A orientacao vive no EXIF, e o BitmapFactory ignora esse metadado: sem
            // ler e aplicar, foto de celular entra deitada. Sao dois streams porque o
            // primeiro e consumido pela leitura do EXIF.
            val giro = context.contentResolver.openInputStream(origem).use { entrada ->
                entrada?.let { grausDeRotacao(ExifInterface(it)) } ?: 0f
            }
            val bitmapOriginal = context.contentResolver.openInputStream(origem).use { entrada ->
                BitmapFactory.decodeStream(entrada)
            } ?: error("Nao foi possivel decodificar a imagem")

            val bitmap = aplicarRotacao(bitmapOriginal, giro)
            val reduzido = reduzir(bitmap)
            // Sufixo de tempo evita o Coil servir a imagem antiga do cache depois de trocar.
            val nome = "$PREFIX${carroId}_${System.currentTimeMillis()}.jpg"
            FileOutputStream(File(context.filesDir, nome)).use { saida ->
                reduzido.compress(Bitmap.CompressFormat.JPEG, QUALIDADE_JPEG, saida)
            }
            if (reduzido !== bitmap) reduzido.recycle()
            if (bitmap !== bitmapOriginal) bitmap.recycle()
            bitmapOriginal.recycle()
            nome
        }.onFailure {
            Log.w(TAG_VEHICLE_PHOTO, "Falha ao salvar foto do veiculo", it)
        }.getOrNull()
    }

    /** Remove a foto anterior do disco. Trocar de foto nao pode deixar lixo acumulado. */
    fun apagar(context: Context, fotoNome: String?) {
        val arquivo = arquivoDe(context, fotoNome) ?: return
        runCatching { arquivo.delete() }
            .onFailure { Log.w(TAG_VEHICLE_PHOTO, "Falha ao apagar foto do veiculo", it) }
    }

    /** Graus que faltam para a foto ficar em pe, lidos do EXIF. */
    private fun grausDeRotacao(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

    private fun aplicarRotacao(bitmap: Bitmap, graus: Float): Bitmap {
        if (graus == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(graus) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    private fun reduzir(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= MAX_LARGURA) return bitmap
        val proporcao = MAX_LARGURA.toFloat() / bitmap.width
        val altura = (bitmap.height * proporcao).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, MAX_LARGURA, altura, true)
    }
}
