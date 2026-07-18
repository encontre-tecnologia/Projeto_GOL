package br.com.gui.carlembrete

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
internal fun DriverManagerScreen(
    drivers: List<OperationalDriver>,
    routeRecords: List<OperationalRecord>,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    screenBg: Color,
    onDismiss: () -> Unit,
    onSaveDriver: (
        editingDriverId: String?,
        name: String,
        code: String,
        phone: String,
        salary: String,
        taxCost: String,
        defaultCost: String
    ) -> Boolean,
    onDeleteDriver: (OperationalDriver) -> Unit
) {
    var editingDriver by remember { mutableStateOf<OperationalDriver?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<OperationalDriver?>(null) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var taxCost by remember { mutableStateOf("") }
    var defaultCost by remember { mutableStateOf("") }

    fun openEditor(driver: OperationalDriver?) {
        editingDriver = driver
        name = driver?.name.orEmpty()
        code = driver?.code.orEmpty()
        phone = driver?.phone.orEmpty()
        salary = driver?.salary?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        taxCost = driver?.taxCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        defaultCost = driver?.defaultCost?.takeIf { it > 0.0 }?.let { formatPlainDecimal(it) }.orEmpty()
        showEditor = true
    }

    val sortedDrivers = remember(drivers) {
        drivers.sortedWith(compareBy<OperationalDriver> { it.name.lowercase(Locale.getDefault()) }.thenBy { it.code })
    }

    if (showEditor) {
        DriverEditorScreen(
            title = if (editingDriver == null) tr("Cadastrar motorista", "Add driver") else tr("Editar motorista", "Edit driver"),
            subtitle = tr(
                "Os vinculos com linhas serao mantidos pelo identificador interno.",
                "Route links are kept by the internal identifier."
            ),
            name = name,
            onNameChange = { name = it },
            code = code,
            onCodeChange = { code = it },
            phone = phone,
            onPhoneChange = { phone = formatDriverPhoneInput(it) },
            salary = salary,
            onSalaryChange = { salary = keepDecimalInput(it) },
            taxCost = taxCost,
            onTaxCostChange = { taxCost = keepDecimalInput(it) },
            cost = defaultCost,
            onCostChange = { defaultCost = keepDecimalInput(it) },
            cardBg = cardBg,
            cardBorder = cardBorder,
            titleColor = titleColor,
            subColor = subColor,
            screenBg = screenBg,
            onSave = {
                val saved = onSaveDriver(editingDriver?.id, name, code, phone, salary, taxCost, defaultCost)
                if (saved) showEditor = false
            },
            onDismiss = { showEditor = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
            }
            IconButton(
                onClick = { openEditor(null) },
                modifier = Modifier.align(Alignment.CenterEnd).size(40.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = tr("Cadastrar motorista", "Add driver"), tint = titleColor)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(29.dp))
            }
            Text(
                text = tr("Gerenciar Motoristas", "Manage Drivers"),
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = tr(
                    "Edite os motoristas usados nas linhas sem criar cadastro duplicado.",
                    "Edit the drivers used on routes without creating duplicate records."
                ),
                color = subColor,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = { openEditor(null) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF357AE8), contentColor = Color.White)
        ) {
            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tr("Novo motorista", "New driver"), fontWeight = FontWeight.Bold)
        }

        if (sortedDrivers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr("Nenhum motorista cadastrado", "No drivers registered"),
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = tr("Cadastre aqui e use no seletor das linhas.", "Add one here and use it in route selectors."),
                    color = subColor,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            sortedDrivers.forEach { driver ->
                DriverManagerCard(
                    driver = driver,
                    linkedRoutes = routeRecords.filter { it.driverId == driver.id },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    titleColor = titleColor,
                    subColor = subColor,
                    onEdit = { openEditor(driver) },
                    onDelete = { deleteCandidate = driver }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
    }

    deleteCandidate?.let { driver ->
        val linkedRoutes = routeRecords.filter { it.driverId == driver.id }
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = {
                Text(
                    if (linkedRoutes.isEmpty()) tr("Excluir motorista?", "Delete driver?")
                    else tr("Motorista vinculado", "Driver linked")
                )
            },
            text = {
                Text(
                    if (linkedRoutes.isEmpty()) {
                        tr("Esse motorista sera removido do cadastro.", "This driver will be removed.")
                    } else {
                        tr(
                            "Esse motorista esta vinculado a ${linkedRoutes.size} linha(s). Ao excluir, essas linhas ficarao sem motorista para evitar referencia quebrada.",
                            "This driver is linked to ${linkedRoutes.size} route(s). Deleting will leave those routes without a driver to avoid broken references."
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDriver(driver)
                        deleteCandidate = null
                    }
                ) {
                    Text(tr("Excluir", "Delete"), color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text(tr("Cancelar", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun DriverManagerCard(
    driver: OperationalDriver,
    linkedRoutes: List<OperationalRecord>,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = driver.name,
                    color = titleColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOf(
                        driver.code.takeIf { it.isNotBlank() }?.let { "Cod: $it" },
                        driver.phone.takeIf { it.isNotBlank() }
                    ).filterNotNull().joinToString(" • ").ifBlank { tr("Sem codigo ou telefone", "No code or phone") },
                    color = subColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Edit, contentDescription = tr("Editar", "Edit"), tint = Color(0xFF60A5FA))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Delete, contentDescription = tr("Excluir", "Delete"), tint = Color(0xFFEF4444))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DriverMetricChip(
                label = tr("Salario", "Salary"),
                value = formatMoney(driver.salary),
                titleColor = titleColor,
                subColor = subColor,
                modifier = Modifier.weight(1f)
            )
            DriverMetricChip(
                label = tr("Custos/impostos", "Taxes/costs"),
                value = formatMoney(driver.taxCost),
                titleColor = titleColor,
                subColor = subColor,
                modifier = Modifier.weight(1f)
            )
        }
        DriverMetricChip(
            label = tr("Custo padrao por linha", "Default cost per route"),
            value = formatMoney(driver.defaultCost),
            titleColor = titleColor,
            subColor = subColor,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF2563EB).copy(alpha = 0.08f))
                .border(BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.18f)), RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tr("Linhas vinculadas", "Linked routes"),
                color = subColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (linkedRoutes.isEmpty()) {
                    tr("Nenhuma linha vinculada", "No linked routes")
                } else {
                    linkedRoutes
                        .take(4)
                        .joinToString(" • ") { splitOperationalTitle(it.name).title.ifBlank { it.positionOrRoute } }
                        .let { names ->
                            if (linkedRoutes.size > 4) "$names +${linkedRoutes.size - 4}" else names
                        }
                },
                color = titleColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun DriverMetricChip(
    label: String,
    value: String,
    titleColor: Color,
    subColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = subColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun DriverEditorScreen(
    title: String,
    subtitle: String,
    name: String,
    onNameChange: (String) -> Unit,
    code: String,
    onCodeChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    salary: String,
    onSalaryChange: (String) -> Unit,
    taxCost: String,
    onTaxCostChange: (String) -> Unit,
    cost: String,
    onCostChange: (String) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    subColor: Color,
    screenBg: Color,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = tr("Voltar", "Back"), tint = titleColor)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                color = subColor,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(cardBg)
                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                OperationalTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = tr("Nome do motorista", "Driver name")
                )
                OperationalTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = tr("Codigo/identificador", "Code/identifier")
                )
                OperationalTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = tr("Telefone", "Phone"),
                    keyboardType = KeyboardType.Phone
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OperationalTextField(
                        value = salary,
                        onValueChange = onSalaryChange,
                        label = tr("Salario", "Salary"),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    OperationalTextField(
                        value = taxCost,
                        onValueChange = onTaxCostChange,
                        label = tr("Custos/impostos", "Taxes/costs"),
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                OperationalTextField(
                    value = cost,
                    onValueChange = onCostChange,
                    label = tr("Custo padrao por linha", "Default cost per route"),
                    keyboardType = KeyboardType.Decimal
                )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(tr("Cancelar", "Cancel"))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(tr("Salvar", "Save"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
