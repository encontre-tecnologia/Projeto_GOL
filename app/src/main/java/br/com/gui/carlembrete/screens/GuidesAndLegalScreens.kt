package br.com.gui.carlembrete

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VehicleBasicsGuideScreen(onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val pageBg = if (isDark) scheme.background else scheme.background
    val cardBg = if (isDark) Color(0xFF111827) else scheme.surface
    val border = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val accent = scheme.primary

    val context = LocalContext.current
    val dicas = remember {
        listOf(
            GuideVideoItem(
                icon = Icons.Rounded.TireRepair,
                title = "Como trocar pneu",
                description = "Aprenda o passo a passo para fazer a troca com segurança.",
                videoUrl = "https://autoesporte.globo.com/video/como-trocar-o-pneu-do-carro-9501074.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Speed,
                title = "Como calibrar pneu",
                description = "Veja como calibrar corretamente e por que o pneu deve estar frio.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/07/video-como-a-escolha-de-pneus-influencia-na-seguranca-e-no-desempenho-do-seu-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Build,
                title = "Troca de óleo",
                description = "Entenda quando trocar o óleo e o filtro do motor.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/04/video-como-ver-o-nivel-de-oleo-do-motor-do-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.WaterDrop,
                title = "Conferir água/arrefecimento",
                description = "Como verificar o nível do reservatório com o motor frio.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.WarningAmber,
                title = "Luzes do painel",
                description = "Entenda quais luzes exigem parada imediata e quais permitem seguir com cautela.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.BatteryAlert,
                title = "Bateria fraca (chupeta)",
                description = "Passo a passo para partida auxiliar sem danificar o sistema elétrico.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.DeviceThermostat,
                title = "Superaquecimento",
                description = "O que fazer quando o carro esquenta e o que nunca fazer com motor quente.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2020/02/enchentes-veja-quando-vale-atravessar-e-o-que-fazer-se-teve-prejuizo-com-o-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.ElectricalServices,
                title = "Fusíveis do carro",
                description = "Como identificar fusível queimado e fazer a troca correta.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.CarRepair,
                title = "Itens de emergência",
                description = "Onde ficam triângulo, macaco e chave de roda no veículo.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.OilBarrel,
                title = "Medir nível do óleo",
                description = "Como usar a vareta corretamente para conferir o nível do óleo do motor.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/04/video-como-ver-o-nivel-de-oleo-do-motor-do-carro.ghtml"
            ),
            GuideVideoItem(
                icon = null,
                badgeText = "ABS",
                title = "Sinais de problema no freio",
                description = "Ruído, vibração e pedal baixo: quando procurar oficina imediatamente.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2021/07/video-como-a-escolha-de-pneus-influencia-na-seguranca-e-no-desempenho-do-seu-carro.ghtml"
            ),
            GuideVideoItem(
                icon = Icons.Rounded.Description,
                title = "Documentos e emergência",
                description = "Checklist essencial de documentos e contatos para manter no carro.",
                videoUrl = "https://autoesporte.globo.com/videos/noticia/2019/12/vai-viajar-de-carro-veja-dicas-do-que-checar-e-como-se-preparar-para-eventuais-perrengues.ghtml"
            )
        )
    }

    Scaffold(
        containerColor = pageBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Voltar", tint = textPrimary)
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = if (isDark) 0.20f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "Guia rápido do veículo",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
            dicas.forEach { dica ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, border, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dica.badgeText.isNullOrBlank()) {
                                Icon(
                                    dica.icon ?: Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = dica.badgeText,
                                    color = accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            text = dica.title,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Text(dica.description, color = textSecondary, fontSize = 14.sp, lineHeight = 19.sp)
                    Button(
                        onClick = { openExternalUrl(context, dica.videoUrl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Assistir vídeo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private data class GuideVideoItem(
    val icon: ImageVector?,
    val badgeText: String? = null,
    val title: String,
    val description: String,
    val videoUrl: String
)

private fun openExternalUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Não foi possível abrir o vídeo", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun HomeFaqScreen(
    onDismiss: () -> Unit,
    onOpenVehicleGuide: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val background = if (isDark) Color.Black else colorScheme.background
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant
    val englishUi = isEnglishUi()
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }
    val faqItems = remember {
        if (englishUi) {
            listOf(
                "How do I register a new vehicle?" to "Tap New vehicle, choose the type, then select brand and model. If names are loading, wait a moment and the list will appear.",
                "Why can't I pick the vehicle name right away?" to "The app fetches names after the brand is selected. While loading, the field shows a loading message. Wait a few seconds and try again.",
                "How do I create a reminder faster?" to "Tap New reminder, choose the category, review date, mileage and details, then save. You can also start from the camera flow when available.",
                "Where can I see reminder notifications?" to "Use the bell in Home to open notifications history. You can remove single items or clear everything.",
                "How do I add a service provider to a reminder?" to "Open the reminder details and tap Add provider. Fill name and phone, then save to link the contact.",
                "Where is the vehicle guide now?" to "Open Frequently Asked Questions and use the Vehicle guide selector. It opens quick tips with practical videos.",
                "How do I back up and restore my data?" to "Open Settings > Backup. Use restore on this device when needed and reopen the app after completion.",
                "What changes in Premium?" to "Premium unlocks advanced modules like Fleet features, extra management tools, and expanded operational flows."
            )
        } else {
            listOf(
                "Como cadastrar um novo veículo?" to "Toque em Novo veículo, escolha o tipo e depois selecione marca e modelo. Se os nomes estiverem carregando, aguarde alguns segundos.",
                "Por que o nome do veículo não abre na hora?" to "O app busca os nomes após a escolha da marca. Enquanto carrega, o campo mostra mensagem de carregamento. Depois disso, a lista libera.",
                "Como criar um aviso mais rápido?" to "Toque em Novo aviso, escolha a categoria, revise data, km e detalhes e finalize em salvar. Quando disponível, você também pode iniciar pela câmera.",
                "Onde vejo as notificações dos avisos?" to "Use o sino na Home para abrir o histórico de notificações. Dá para remover individualmente ou limpar tudo.",
                "Como adicionar um prestador no aviso?" to "Abra os detalhes do aviso e toque em Adicionar prestador. Preencha nome e telefone e salve para vincular o contato.",
                "Onde ficou o guia do veículo?" to "Agora ele está em Dúvidas frequentes, no seletor Guia sobre o veículo. Lá você abre dicas rápidas com vídeos.",
                "Como fazer backup e restaurar meus dados?" to "Vá em Configurações > Backup. Use Restaurar backup neste aparelho quando precisar e reabra o app após concluir.",
                "O que muda no Premium?" to "O Premium libera módulos avançados como recursos de frota, ferramentas extras de gestão e fluxos operacionais expandidos."
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = tr("Voltar", "Back"),
                        tint = titleColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("Dúvidas frequentes", "Frequently asked questions"),
                    color = titleColor,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tr("Respostas rápidas para as dúvidas mais comuns", "Quick answers for the most common questions"),
                    color = bodyColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenVehicleGuide() },
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Guia sobre o veículo", "Vehicle guide"),
                            color = titleColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = tr("Abra dicas rápidas com vídeos para cuidar melhor do seu veículo.", "Open quick video tips to take better care of your vehicle."),
                            color = bodyColor,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        tint = bodyColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            faqItems.forEachIndexed { index, (pergunta, resposta) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedFaqIndex = if (expandedFaqIndex == index) -1 else index
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pergunta,
                                color = titleColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expandedFaqIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = titleColor
                            )
                        }
                        if (expandedFaqIndex == index) {
                            Spacer(Modifier.height(8.dp))
                            Divider(
                                color = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.55f),
                                thickness = 1.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = resposta,
                                color = bodyColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(11.dp))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun LegalInfoScreen(
    title: String,
    icon: ImageVector,
    content: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    val background = if (isDark) Color.Black else colorScheme.background
    val titleColor = colorScheme.onSurface
    val bodyColor = colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = titleColor
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.14f)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF111827) else colorScheme.surface,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = if (isDark) 0.5f else 0.75f))
            ) {
                Text(
                    text = content,
                    color = bodyColor,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
