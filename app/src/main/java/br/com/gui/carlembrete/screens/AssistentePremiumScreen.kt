package br.com.gui.carlembrete

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private data class TravelExpense(
    val id: String,
    val label: String,
    val amount: Double,
    val category: String,
    val notePhotoUri: Uri? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistentePremiumScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val bg = if (isDark) scheme.background else Color.White
    val textPrimary = if (isDark) Color.White else Color.Black
    val textDim = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.12f)
    val accentBlue = Color(0xFF3B82F6)
    val categories = listOf("Combustivel", "Pedagio", "Alimentacao", "Hospedagem", "Compras", "Outros")

    val expenses = remember { mutableStateListOf<TravelExpense>() }
    var expenseLabel by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Combustivel") }
    var editingExpenseId by remember { mutableStateOf<String?>(null) }
    var editExpenseLabel by remember { mutableStateOf("") }
    var editExpenseAmount by remember { mutableStateOf("") }
    var editExpenseCategory by remember { mutableStateOf("Combustivel") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val photoUris = remember { mutableStateListOf<Uri>() }
    var showCamera by remember { mutableStateOf(false) }
    var isQrLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = { Text("Viagem", color = textPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue)
                        Text("1. Adicionar gasto", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    var showCategoryDialog by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showCategoryDialog = true }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textPrimary,
                                disabledBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                                disabledContainerColor = bg,
                                disabledLabelColor = textDim,
                                disabledTrailingIconColor = textDim
                            )
                        )
                    }
                    if (showCategoryDialog) {
                        Dialog(onDismissRequest = { showCategoryDialog = false }) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Categoria", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    categories.forEach { item ->
                                        val icon = when (item) {
                                            "Combustivel" -> Icons.Default.LocalGasStation
                                            "Pedagio" -> Icons.Default.Toll
                                            else -> Icons.Default.ReceiptLong
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                category = item
                                                showCategoryDialog = false
                                            },
                                            border = BorderStroke(1.dp, cardBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(icon, contentDescription = null, tint = accentBlue, modifier = Modifier.size(18.dp))
                                                Text(item)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = expenseLabel,
                        onValueChange = { expenseLabel = it },
                        label = { Text("Produtos") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = textPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
                            unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                            focusedLabelColor = textDim,
                            unfocusedLabelColor = textDim
                        )
                    )
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Valor Total") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = textPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
                            unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                            focusedLabelColor = textDim,
                            unfocusedLabelColor = textDim
                        )
                    )

                    Button(
                        onClick = {
                            val amount = parseValorMonetario(expenseAmount)
                            if (amount == null || amount <= 0.0) {
                                Toast.makeText(context, "Informe um valor valido.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            expenses.add(
                                TravelExpense(
                                    id = UUID.randomUUID().toString(),
                                    label = expenseLabel.ifBlank { category },
                                    amount = amount,
                                    category = category,
                                    notePhotoUri = photoUris.lastOrNull()
                                )
                            )
                            expenseLabel = ""
                            expenseAmount = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Adicionar Gasto", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showCamera = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Adicionar Foto da Nota", fontWeight = FontWeight.SemiBold)
                    }

                    if (photoUris.isNotEmpty()) {
                        Text("Fotos adicionadas: ${photoUris.size}", color = textDim, fontSize = 12.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Wallet, contentDescription = null, tint = accentBlue)
                        Text("Gastos adicionados", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        if (expenses.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Wallet,
                                    contentDescription = null,
                                    tint = textDim,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Nenhum gasto adicionado.", color = textDim, fontSize = 12.sp)
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                expenses.forEach { expense ->
                                    val expenseCardBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = expenseCardBg),
                                        border = BorderStroke(1.dp, cardBorder),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    expense.category,
                                                    color = accentBlue,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier
                                                        .background(
                                                            color = accentBlue.copy(alpha = if (isDark) 0.22f else 0.14f),
                                                            shape = RoundedCornerShape(999.dp)
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                                Text(currency.format(expense.amount), color = textPrimary, fontWeight = FontWeight.Bold)
                                            }

                                            Text(expense.label, color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        if (expense.notePhotoUri == null) {
                                                            Toast.makeText(context, "Este gasto nao possui foto da nota.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            selectedPhotoUri = expense.notePhotoUri
                                                        }
                                                    },
                                                    border = BorderStroke(1.dp, cardBorder),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Image,
                                                        contentDescription = "Ver foto da nota",
                                                        tint = textDim,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Ver foto da nota", fontSize = 12.sp)
                                                }
                                                IconButton(
                                                    onClick = {
                                                        editingExpenseId = expense.id
                                                        editExpenseLabel = expense.label
                                                        editExpenseAmount = "R$ ${formatarValorBr(expense.amount)}"
                                                        editExpenseCategory = expense.category
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Editar gasto", tint = accentBlue)
                                                }
                                                IconButton(
                                                    onClick = { expenses.removeAll { it.id == expense.id } }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Apagar gasto", tint = Color(0xFFDC2626))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    if (showCamera) {
        CameraCapturaDialog(
            onDismiss = { showCamera = false },
            onFotoCapturada = { resultado ->
                showCamera = false
                photoUris.add(Uri.fromFile(resultado.arquivoFoto))
                val qrUrl = resultado.qrCodeUrl?.trim()
                if (qrUrl.isNullOrBlank()) {
                    Toast.makeText(context, "QR Code da nota nao detectado.", Toast.LENGTH_LONG).show()
                    return@CameraCapturaDialog
                }

                scope.launch {
                    isQrLoading = true
                    val notaInfo = consultarNotaPorQrCode(qrUrl)
                    isQrLoading = false
                    if (notaInfo == null) {
                        Toast.makeText(context, "QR lido, mas nao foi possivel carregar a nota.", Toast.LENGTH_LONG).show()
                    } else {
                        notaInfo.valorTotal?.let { total ->
                            expenseAmount = "R$ ${formatarValorBr(total)}"
                        }
                        val descricaoItens = notaInfo.descricaoItens?.trim().orEmpty()
                        if (descricaoItens.isNotBlank()) {
                            expenseLabel = formatarProdutosParaDescricao(descricaoItens)
                        }
                        Toast.makeText(context, "Nota lida. Campos preenchidos.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (isQrLoading) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text("Lendo nota", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = accentBlue
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Consultando dados da nota...", color = textDim)
                }
            },
            confirmButton = {}
        )
    }

    if (selectedPhotoUri != null) {
        Dialog(onDismissRequest = { selectedPhotoUri = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                val bitmap = remember(selectedPhotoUri) {
                    selectedPhotoUri?.let { uri ->
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                BitmapFactory.decodeStream(input)
                            }
                        }.getOrNull()
                    }
                }
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Foto da nota", color = textPrimary, fontWeight = FontWeight.SemiBold)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto da nota",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        )
                    } else {
                        Text("Nao foi possivel carregar a foto.", color = textDim, fontSize = 12.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { selectedPhotoUri = null }) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }
    }

    if (editingExpenseId != null) {
        Dialog(onDismissRequest = { editingExpenseId = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Editar gasto", color = textPrimary, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = editExpenseLabel,
                        onValueChange = { editExpenseLabel = it },
                        label = { Text("Produtos") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = textPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
                            unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                            focusedLabelColor = textDim,
                            unfocusedLabelColor = textDim
                        )
                    )

                    OutlinedTextField(
                        value = editExpenseAmount,
                        onValueChange = { editExpenseAmount = it },
                        label = { Text("Valor Total") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = textPrimary,
                            focusedBorderColor = if (isDark) Color(0xFF334155) else Color.Black,
                            unfocusedBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                            focusedContainerColor = bg,
                            unfocusedContainerColor = bg,
                            focusedLabelColor = textDim,
                            unfocusedLabelColor = textDim
                        )
                    )

                    var showEditCategoryDialog by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showEditCategoryDialog = true }
                    ) {
                        OutlinedTextField(
                            value = editExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Categoria") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textDim)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = textPrimary,
                                disabledBorderColor = if (isDark) Color(0xFF1F2A44) else Color(0xFFCBD5E1),
                                disabledContainerColor = bg,
                                disabledLabelColor = textDim,
                                disabledTrailingIconColor = textDim
                            )
                        )
                    }
                    if (showEditCategoryDialog) {
                        Dialog(onDismissRequest = { showEditCategoryDialog = false }) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = bg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Categoria", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    categories.forEach { item ->
                                        OutlinedButton(
                                            onClick = {
                                                editExpenseCategory = item
                                                showEditCategoryDialog = false
                                            },
                                            border = BorderStroke(1.dp, cardBorder),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(item)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { editingExpenseId = null }) {
                            Text("Cancelar")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = parseValorMonetario(editExpenseAmount)
                                if (amount == null || amount <= 0.0) {
                                    Toast.makeText(context, "Informe um valor valido.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val idx = expenses.indexOfFirst { it.id == editingExpenseId }
                                if (idx >= 0) {
                                    expenses[idx] = expenses[idx].copy(
                                        label = editExpenseLabel.ifBlank { editExpenseCategory },
                                        amount = amount,
                                        category = editExpenseCategory
                                    )
                                }
                                editingExpenseId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

private fun formatarProdutosParaDescricao(descricaoItens: String): String {
    val itemComValorRegex = Regex("^(.*?)\\s*\\((\\d+[\\.,]\\d{2})\\)\\s*$")
    return descricaoItens
        .split("+")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { item ->
            val match = itemComValorRegex.find(item)
            if (match != null) {
                val nome = match.groupValues[1].trim()
                val valor = match.groupValues[2].trim().replace(".", ",")
                if (nome.isNotBlank()) "$nome - R$ $valor" else item
            } else {
                item
            }
        }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

private fun formatarValorBr(valor: Double): String =
    String.format(Locale("pt", "BR"), "%.2f", valor)

private fun parseValorMonetario(valorInput: String): Double? {
    val limpo = valorInput
        .replace("R$", "", ignoreCase = true)
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
        .trim()
    return limpo.toDoubleOrNull()
}
