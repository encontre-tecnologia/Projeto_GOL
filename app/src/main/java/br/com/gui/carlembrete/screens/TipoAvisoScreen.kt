package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AvisoItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val tipo: TipoManutencao? = null,
    val iconOverride: ImageVector? = null,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipoAvisoScreen(
    itensAviso: List<AvisoItem>,
    backgroundBrush: Brush,
    surfaceDark: Color,
    textLight: Color,
    textDim: Color,
    onDismiss: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tipo de aviso",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Voltar",
                            tint = textDim
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceDark)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-40).dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EventNote,
                        contentDescription = null,
                        tint = textDim,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Novo aviso",
                        color = textLight,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Selecione a categoria para continuar",
                        color = textDim,
                        fontSize = 13.sp
                    )

                    Text(
                        "Escolha o tipo de aviso:",
                        color = textDim,
                        fontSize = 13.sp
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itensAviso.chunked(2).forEach { linha ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                linha.forEach { item ->
                                    OutlinedButton(
                                        onClick = item.onClick,
                                        border = BorderStroke(1.dp, Color(0xFF334155)),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            10.dp
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(72.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (item.iconOverride != null) {
                                                Icon(
                                                    imageVector = item.iconOverride,
                                                    contentDescription = null,
                                                    tint = item.color,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else if (item.tipo != null) {
                                                TipoIcon(
                                                    tipo = item.tipo,
                                                    tint = item.color,
                                                    size = 24.dp,
                                                    textSize = 15.sp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = null,
                                                    tint = item.color,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Text(
                                                item.label,
                                                color = textLight,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                        }
                                    }
                                }
                                if (linha.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }}
