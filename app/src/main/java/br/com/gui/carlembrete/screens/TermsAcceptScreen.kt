package br.com.gui.carlembrete

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TermsAcceptScreen(
    onAccepted: () -> Unit,
    cardBg: Color = Color(0xFF1E293B),
    accentColor: Color = Color(0xFF22C55E)
) {
    var aceitouTermos by remember { mutableStateOf(false) }
    var aceitouPrivacidade by remember { mutableStateOf(false) }

    val termosUsoTexto = remember {
        """
        1. Aceite: ao usar o Zellu, você concorda com estes Termos e com a Política de Privacidade.

        2. Objeto: o app oferece gestão de veículos, lembretes, manutenções, viagens, frota, estoque e recursos de inteligência artificial.

        3. Recursos de IA: a Zellu AI é ferramenta de apoio para interpretar dados cadastrados, responder perguntas e preparar ações solicitadas. Ela pode cometer erros e não substitui diagnóstico técnico, vistoria, mecânico, seguro ou decisão do usuário.

        4. Uso adequado: você se compromete a usar o app de forma lícita, sem fraude, abuso técnico ou violação de direitos de terceiros.

        5. Conta e segurança: você é responsável pelos dados da conta e pela guarda do acesso.

        6. Planos e cobrança: planos pagos (como Lite/Frota) seguem regras da loja/plataforma de pagamento para renovação, cancelamento e reembolso.

        7. Limitação: o Zellu é ferramenta de apoio e não substitui diagnóstico técnico, vistoria, seguro, assistência mecânica ou orientação profissional.

        8. Disponibilidade: funcionalidades podem ser alteradas, corrigidas, suspensas ou descontinuadas por evolução do produto, segurança ou obrigação legal.

        9. Propriedade intelectual: marca, software, layout e conteúdo do app são protegidos por lei.

        10. Legislação e foro: aplica-se a legislação brasileira, com foro da comarca de Sao Carlos/SP, salvo competência legal específica.

        11. Contato legal e suporte: guilhermedevsistemas@gmail.com
        """.trimIndent()
    }

    val politicaPrivacidadeTexto = remember {
        """
        1. Dados tratados: o app pode tratar dados de conta (nome, e-mail e identificadores), cadastro de veículos, lembretes, contatos, viagens, itens de estoque, localização, câmera, notificações, dados técnicos essenciais e interações com recursos de IA.

        2. Finalidades: autenticação, execução das funcionalidades, recursos inteligentes/IA, segurança, prevenção de abuso/fraude, suporte e melhoria contínua.

        3. Bases legais (LGPD): execução de contrato, consentimento quando exigido, legítimo interesse para segurança/estabilidade e cumprimento de obrigação legal.

        4. Permissões: câmera, localização e notificações são usadas somente com autorização e podem ser revogadas a qualquer momento no dispositivo.

        5. IA e provedores técnicos: a Zellu AI pode usar dados cadastrados e mensagens do chat para responder e preparar ações. Quando recursos online estiverem habilitados, o conteúdo necessário pode ser processado por provedores técnicos de infraestrutura e/ou IA.

        6. Compartilhamento: não vendemos dados pessoais. Podemos compartilhar com operadores/provedores técnicos necessários ao funcionamento do app e com autoridades quando houver obrigação legal.

        7. Retenção e armazenamento: parte dos dados pode ficar no dispositivo e parte em nuvem, pelo tempo necessário às finalidades e obrigações legais.

        8. Direitos do titular: você pode solicitar confirmação de tratamento, acesso, correção, anonimização, exclusão e revogação do consentimento, nos termos da LGPD.

        9. Exclusão de conta e dados: ao solicitar exclusão, removemos dados pessoais e registros vinculados, ressalvadas retenções legais obrigatórias.

        10. Transferência internacional: alguns provedores podem processar dados fora do Brasil, com salvaguardas adequadas.

        11. Contato oficial de privacidade, remoção de dados, dúvidas e suporte:
        - guilhermedevsistemas@gmail.com
        Páginas oficiais:
        - https://zellu-privacidade.vercel.app/privacy-policy.html
        - https://zellu-privacidade.vercel.app/terms-of-use.html
        """.trimIndent()
    }

    // Content scrolls; button is always pinned at the bottom
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
            item {
                Text(
                    "Termos e Privacidade",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Text(
                    "Para continuar, aceite os Termos de Uso e a Política de Privacidade do Zellu.",
                    color = Color(0xFFBFDBFE),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Termos de Uso",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(termosUsoTexto, color = Color(0xFFBFDBFE), fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Política de Privacidade",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(politicaPrivacidadeTexto, color = Color(0xFFBFDBFE), fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
            item {
                val tudoMarcado = aceitouTermos && aceitouPrivacidade
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (tudoMarcado) Color(0xFF22C55E) else Color(0xFF334155)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Confirmações obrigatórias",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        // "Concordo com tudo" — marks/unmarks all at once
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (tudoMarcado) Color(0xFF14532D).copy(alpha = 0.5f)
                                    else Color(0xFF1E293B).copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = tudoMarcado,
                                onCheckedChange = { checked ->
                                    aceitouTermos = checked
                                    aceitouPrivacidade = checked
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF22C55E),
                                    uncheckedColor = Color(0xFF94A3B8),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                "Concordo com tudo",
                                color = if (tudoMarcado) Color(0xFF22C55E) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = aceitouTermos,
                                onCheckedChange = { aceitouTermos = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF22C55E),
                                    uncheckedColor = Color(0xFF94A3B8),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                "Li e aceito os Termos de Uso.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = aceitouPrivacidade,
                                onCheckedChange = { aceitouPrivacidade = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF22C55E),
                                    uncheckedColor = Color(0xFF94A3B8),
                                    checkmarkColor = Color.White
                                )
                            )
                            Text(
                                "Li e aceito a Política de Privacidade.",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Button always visible at the bottom, outside the scroll area
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
            Button(
                onClick = onAccepted,
                enabled = aceitouTermos && aceitouPrivacidade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (aceitouTermos && aceitouPrivacidade) Color(0xFF2563EB) else Color(0xFF475569),
                    contentColor = Color.White
                )
            ) { Text("Próximo", fontSize = 19.sp) }
        }
    }
}
