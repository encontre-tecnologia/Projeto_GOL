package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Modelos internos ───────────────────────────────────────────────────────

private data class PassoTrocaPneu(
    val numero: Int,
    val titulo: String,
    val descricao: String,
    val icon: ImageVector,
    val cor: Color
)

private data class ItemIntervalManutencao(
    val servico: String,
    val km: String,
    val periodo: String,
    val urgencia: UrgenciaManutencao
)

private enum class UrgenciaManutencao { ALTA, MEDIA, NORMAL }

private data class DicaManutencao(
    val titulo: String,
    val descricao: String,
    val icon: ImageVector,
    val cor: Color
)

// ─── Overlay principal ───────────────────────────────────────────────────────

@Composable
fun GuiaManutencaoOverlay(
    onDismiss: () -> Unit,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color,
    pageBackground: Color
) {
    val abas = listOf(
        tr("Intervalos", "Intervals"),
        tr("Pneus", "Tires"),
        tr("Dicas", "Tips")
    )
    var abaSelecionada by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    LaunchedEffect(abaSelecionada) { scrollState.animateScrollTo(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .statusBarsPadding()
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = tr("Voltar", "Back"),
                    tint = textPrimary
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    tr("Guia de Manutenção", "Maintenance Guide"),
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    tr("Tabelas e dicas de referência", "Reference tables and tips"),
                    color = textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // ── Tabs ─────────────────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = abaSelecionada,
            containerColor = pageBackground,
            contentColor = accentBlue
        ) {
            abas.forEachIndexed { index, titulo ->
                Tab(
                    selected = abaSelecionada == index,
                    onClick = { abaSelecionada = index },
                    selectedContentColor = accentBlue,
                    unselectedContentColor = textSecondary,
                    text = {
                        Text(
                            titulo,
                            fontSize = 13.sp,
                            fontWeight = if (abaSelecionada == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ── Conteúdo das abas ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (abaSelecionada) {
                0 -> GuiaIntervalosContent(isDark, textPrimary, textSecondary, accentBlue)
                1 -> GuiaTrocaPneuContent(isDark, textPrimary, textSecondary, accentBlue)
                2 -> GuiaDicasContent(isDark, textPrimary, textSecondary, accentBlue)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

// ─── Aba 1: Como trocar pneu ─────────────────────────────────────────────────

@Composable
private fun GuiaTrocaPneuContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)

    // Aviso de segurança
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = accentBlue.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accentBlue.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = accentBlue,
                modifier = Modifier.size(20.dp).padding(top = 1.dp)
            )
            Text(
                tr(
                    "Sempre que possível, procure um borracheiro. Este guia é para situações de emergência em estrada.",
                    "Whenever possible, seek a tire specialist. This guide is for roadside emergencies."
                ),
                color = textPrimary,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }

    val passos = listOf(
        PassoTrocaPneu(
            1,
            tr("Posicione o veículo", "Position the vehicle"),
            tr("Estacione em local plano, firme e seguro, longe do trânsito. Aplique o freio de mão e engate a 1ª marcha (ou \"P\" no automático).", "Park on a flat, firm, safe location away from traffic. Apply the handbrake and engage 1st gear (or 'P' on automatics)."),
            Icons.Default.Warning, Color(0xFFF59E0B)
        ),
        PassoTrocaPneu(
            2,
            tr("Sinalização de segurança", "Safety signaling"),
            tr("Ligue os pisca-alertas imediatamente. Coloque o triângulo de segurança a ~30 m atrás do veículo.", "Turn on the hazard lights immediately. Place the safety triangle ~30 m behind the vehicle."),
            Icons.Default.Warning, Color(0xFFEF4444)
        ),
        PassoTrocaPneu(
            3,
            tr("Afrouxe os parafusos", "Loosen the bolts"),
            tr("Com o veículo NO CHÃO, afrouxe levemente cada parafuso com a chave de roda no sentido anti-horário. NÃO retire ainda.", "With the vehicle ON THE GROUND, slightly loosen each bolt counterclockwise with the wheel wrench. Do NOT remove yet."),
            Icons.Rounded.Build, accentBlue
        ),
        PassoTrocaPneu(
            4,
            tr("Posicione o macaco", "Position the jack"),
            tr("Localize o ponto de apoio indicado no manual do veículo — geralmente uma nervura metálica reforçada próxima à roda com o pneu furado.", "Locate the jack support point indicated in the vehicle manual — usually a reinforced metal rib near the flat wheel."),
            Icons.Rounded.DirectionsCar, Color(0xFF7C3AED)
        ),
        PassoTrocaPneu(
            5,
            tr("Levante o veículo", "Lift the vehicle"),
            tr("Acione o macaco até a roda ficar ~10 cm do chão. NUNCA coloque qualquer parte do corpo sob o veículo enquanto ele estiver levantado.", "Operate the jack until the wheel is ~10 cm off the ground. NEVER place any body part under the vehicle while it is raised."),
            Icons.Rounded.Speed, Color(0xFF0EA5E9)
        ),
        PassoTrocaPneu(
            6,
            tr("Remova o pneu furado", "Remove the flat tire"),
            tr("Retire os parafusos completamente e guarde-os em local seguro. Puxe o pneu furado para fora e posicione-o sob a carroceria como trava de segurança.", "Remove the bolts completely and store them safely. Pull the flat tire out and place it under the chassis as a safety lock."),
            Icons.Default.Warning, Color(0xFFDC2626)
        ),
        PassoTrocaPneu(
            7,
            tr("Monte o estepe", "Mount the spare tire"),
            tr("Encaixe o estepe nos pinos. Aperte os parafusos COM A MÃO em forma de estrela (intercalado, nunca em sequência circular).", "Fit the spare onto the pins. Tighten the bolts BY HAND in a star pattern (alternating, never in circular sequence)."),
            Icons.Default.CheckCircle, Color(0xFF16A34A)
        ),
        PassoTrocaPneu(
            8,
            tr("Abaixe e aperte os parafusos", "Lower and tighten bolts"),
            tr("Retire o pneu furado de baixo da carroceria. Abaixe o veículo. Com a roda no chão, aperte todos os parafusos com força total, em forma de estrela.", "Remove the flat from under the chassis. Lower the vehicle. With the wheel on the ground, fully tighten all bolts in a star pattern."),
            Icons.Rounded.Build, accentBlue
        ),
        PassoTrocaPneu(
            9,
            tr("Finalize e dirija com cautela", "Finalize and drive carefully"),
            tr("Guarde o pneu furado, o macaco e as ferramentas. Estepes sobressalentes têm velocidade máx. de 80 km/h. Calibre na primeira oportunidade.", "Stow the flat tire, jack and tools. Spare tires have a max. speed of 80 km/h. Calibrate at the first opportunity."),
            Icons.Rounded.Speed, Color(0xFF059669)
        )
    )

    passos.forEach { passo ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Círculo numerado
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(passo.cor.copy(alpha = 0.14f), CircleShape)
                        .border(1.5.dp, passo.cor.copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        passo.numero.toString(),
                        color = passo.cor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        passo.titulo,
                        color = textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        passo.descricao,
                        color = textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

// ─── Aba 2: Tabela de intervalos ──────────────────────────────────────────────

@Composable
private fun GuiaIntervalosContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color
) {
    val rowEven = if (isDark) Color(0xFF1E293B) else Color.White
    val rowOdd = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val colorAlta = Color(0xFFEF4444)
    val colorMedia = Color(0xFFF59E0B)
    val colorNormal = Color(0xFF16A34A)

    val intervalos = listOf(
        ItemIntervalManutencao(tr("Troca de óleo", "Oil change"), "5.000 – 10.000 km", tr("6 – 12 meses", "6 – 12 months"), UrgenciaManutencao.MEDIA),
        ItemIntervalManutencao(tr("Filtro de óleo", "Oil filter"), "5.000 – 10.000 km", tr("A cada troca de óleo", "Every oil change"), UrgenciaManutencao.MEDIA),
        ItemIntervalManutencao(tr("Alinhamento", "Alignment"), "10.000 km", tr("12 meses", "12 months"), UrgenciaManutencao.NORMAL),
        ItemIntervalManutencao(tr("Balanceamento", "Balancing"), "10.000 km", tr("12 meses", "12 months"), UrgenciaManutencao.NORMAL),
        ItemIntervalManutencao(tr("Filtro de ar", "Air filter"), "15.000 – 20.000 km", tr("12 meses", "12 months"), UrgenciaManutencao.NORMAL),
        ItemIntervalManutencao(tr("Filtro de combustível", "Fuel filter"), "20.000 – 40.000 km", tr("24 meses", "24 months"), UrgenciaManutencao.NORMAL),
        ItemIntervalManutencao(tr("Pastilhas de freio", "Brake pads"), "30.000 – 40.000 km", tr("Inspecionar anualmente", "Inspect annually"), UrgenciaManutencao.ALTA),
        ItemIntervalManutencao(tr("Fluido de freio", "Brake fluid"), "20.000 – 30.000 km", tr("12 – 24 meses", "12 – 24 months"), UrgenciaManutencao.ALTA),
        ItemIntervalManutencao(tr("Correia dentada", "Timing belt"), "60.000 – 100.000 km", tr("4 – 5 anos", "4 – 5 years"), UrgenciaManutencao.ALTA),
        ItemIntervalManutencao(tr("Velas de ignição", "Spark plugs"), "20.000 – 30.000 km", tr("24 meses", "24 months"), UrgenciaManutencao.MEDIA),
        ItemIntervalManutencao(tr("Bateria", "Battery"), tr("Sem km fixo", "No fixed km"), tr("2 – 3 anos", "2 – 3 years"), UrgenciaManutencao.MEDIA),
        ItemIntervalManutencao(tr("Pneus (troca)", "Tires (replacement)"), "40.000 – 80.000 km", tr("5 – 10 anos", "5 – 10 years"), UrgenciaManutencao.ALTA),
        ItemIntervalManutencao(tr("Revisão geral", "General service"), "10.000 – 15.000 km", tr("12 meses", "12 months"), UrgenciaManutencao.MEDIA),
        ItemIntervalManutencao("IPVA", tr("–", "–"), tr("Anual", "Annual"), UrgenciaManutencao.NORMAL),
        ItemIntervalManutencao(tr("Licenciamento", "Registration"), tr("–", "–"), tr("Anual", "Annual"), UrgenciaManutencao.NORMAL)
    )

    // Legenda de prioridade
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                tr("Prioridade:", "Priority:"),
                color = textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            listOf(
                tr("Alta", "High") to colorAlta,
                tr("Média", "Medium") to colorMedia,
                tr("Normal", "Normal") to colorNormal
            ).forEach { (label, cor) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(cor, CircleShape)
                    )
                    Text(label, color = textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    // Tabela
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cabeçalho azul
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        accentBlue,
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    tr("Serviço", "Service"),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2.4f)
                )
                Text(
                    "KM",
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.Center
                )
                Text(
                    tr("Período", "Period"),
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.8f),
                    textAlign = TextAlign.End
                )
            }
            // Linhas de dados
            intervalos.forEachIndexed { index, item ->
                val urgColor = when (item.urgencia) {
                    UrgenciaManutencao.ALTA -> colorAlta
                    UrgenciaManutencao.MEDIA -> colorMedia
                    UrgenciaManutencao.NORMAL -> colorNormal
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) rowEven else rowOdd)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(2.4f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(urgColor, CircleShape)
                        )
                        Text(
                            item.servico,
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        item.km,
                        color = textSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1.8f),
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Text(
                        item.periodo,
                        color = textSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1.8f),
                        textAlign = TextAlign.End,
                        lineHeight = 14.sp
                    )
                }
                // Separador entre linhas (exceto a última)
                if (index < intervalos.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(borderColor)
                    )
                }
            }
        }
    }

    // Nota de rodapé
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF59E0B).copy(alpha = if (isDark) 0.12f else 0.08f),
        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(16.dp).padding(top = 1.dp)
            )
            Text(
                tr(
                    "Os intervalos são estimativas gerais. Consulte sempre o manual do seu veículo para os valores exatos.",
                    "Intervals are general estimates. Always consult your vehicle manual for exact values."
                ),
                color = textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Aba 3: Dicas gerais ──────────────────────────────────────────────────────

@Composable
private fun GuiaDicasContent(
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentBlue: Color
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.08f)

    val dicas = listOf(
        DicaManutencao(
            tr("Calibragem dos pneus", "Tire pressure"),
            tr("Calibre sempre com o pneu frio (de manhã ou após mais de 2h parado). A pressão correta economiza combustível, melhora a dirigibilidade e aumenta a vida útil dos pneus. Verifique mensalmente.", "Always calibrate with cold tires (morning or after 2h stopped). Correct pressure saves fuel, improves handling, and increases tire life. Check monthly."),
            Icons.Rounded.Speed,
            Color(0xFF0EA5E9)
        ),
        DicaManutencao(
            tr("Troca de óleo em dia", "Keep up with oil changes"),
            tr("Não ultrapasse o intervalo recomendado. Óleo velho perde viscosidade e pode danificar o motor de forma irreversível. Use sempre o óleo especificado no manual do fabricante.", "Don't exceed the recommended interval. Old oil loses viscosity and can irreversibly damage the engine. Always use the oil specified in the manufacturer's manual."),
            Icons.Rounded.WaterDrop,
            Color(0xFFF59E0B)
        ),
        DicaManutencao(
            tr("Fluido e pastilhas de freio", "Brake fluid and pads"),
            tr("O fluido de freio absorve umidade com o tempo e perde eficiência. Troque no prazo, mesmo sem problemas visíveis. Barulhos ou vibrações ao frear indicam pastilhas desgastadas.", "Brake fluid absorbs moisture over time and loses efficiency. Replace on schedule, even without visible issues. Noises or vibrations when braking indicate worn pads."),
            Icons.Default.Warning,
            Color(0xFFEF4444)
        ),
        DicaManutencao(
            tr("Bateria: sinais de alerta", "Battery: warning signs"),
            tr("Luzes fracas, dificuldade para ligar e bateria com mais de 3 anos são sinais de que a troca é iminente. Faça um teste de carga preventivo antes do inverno ou períodos de chuva.", "Dim lights, difficulty starting, and a battery over 3 years old are signs that replacement is imminent. Run a preventive load test before winter or rainy seasons."),
            Icons.Rounded.BatteryAlert,
            Color(0xFF8B5CF6)
        ),
        DicaManutencao(
            tr("Correia dentada: não arrisque", "Timing belt: don't risk it"),
            tr("É um dos componentes mais críticos do motor. A ruptura pode causar danos graves e irreparáveis (motor 'chapado'). Siga rigorosamente o intervalo do fabricante — nunca deixe para depois.", "It's one of the most critical engine components. Breakage can cause severe and irreparable damage ('bent valves'). Strictly follow the manufacturer's interval — never delay it."),
            Icons.Rounded.Build,
            Color(0xFFDC2626)
        ),
        DicaManutencao(
            tr("Alinhamento e balanceamento", "Alignment and balancing"),
            tr("Direção puxando para um lado, vibração no volante e desgaste irregular dos pneus indicam desalinhamento. Corrija sempre após bater em buracos ou meio-fios. Economiza pneu e combustível.", "Steering pulling to one side, steering wheel vibration, and uneven tire wear indicate misalignment. Always correct after hitting potholes or curbs. Saves tires and fuel."),
            Icons.Rounded.DirectionsCar,
            Color(0xFF16A34A)
        ),
        DicaManutencao(
            tr("Verificações semanais rápidas", "Quick weekly checks"),
            tr("Crie o hábito: nível de óleo (dipstick), água do radiador, fluido do freio e aspecto dos pneus. 5 minutos de atenção evitam horas de dor de cabeça e milhares em consertos.", "Develop the habit: oil level (dipstick), coolant, brake fluid and tire appearance. 5 minutes of attention prevents hours of headaches and thousands in repairs."),
            Icons.Default.CheckCircle,
            Color(0xFF059669)
        ),
        DicaManutencao(
            tr("Ar-condicionado", "Air conditioning"),
            tr("Ligue o ar-condicionado pelo menos uma vez por semana, mesmo no inverno, para lubrificar o compressor e evitar vazamentos. Recarga a cada 1–2 anos.", "Turn on the air conditioning at least once a week, even in winter, to lubricate the compressor and prevent leaks. Recharge every 1–2 years."),
            Icons.Default.Info,
            Color(0xFF06B6D4)
        )
    )

    dicas.forEach { dica ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                // Strip lateral colorida
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            dica.cor,
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(dica.cor.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                dica.icon,
                                contentDescription = null,
                                tint = dica.cor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            dica.titulo,
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        dica.descricao,
                        color = textSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}
