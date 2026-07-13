package br.com.gui.carlembrete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

private data class RNItem(val category: String, val text: String)
private data class RNNote(val id: String, val version: String, val dateMs: Long, val items: List<RNItem>)

private data class RNCategory(val emoji: String, val label: String, val labelEn: String, val color: Color, val bgLight: Color)

private val RN_CATEGORIES = mapOf(
    "novidade"   to RNCategory("✨", "Novidades",  "What's New",   Color(0xFF2563EB), Color(0xFFEFF6FF)),
    "correcao"   to RNCategory("🔧", "Correções",  "Bug Fixes",    Color(0xFFDC2626), Color(0xFFFEF2F2)),
    "melhoria"   to RNCategory("⚡", "Melhorias",  "Improvements", Color(0xFFD97706), Color(0xFFFFFBEB)),
    "lancamento" to RNCategory("🚀", "Em Breve",   "Coming Soon",  Color(0xFF7C3AED), Color(0xFFF5F3FF))
)
private val RN_CATEGORY_ORDER = listOf("novidade", "correcao", "melhoria", "lancamento")

@Composable
fun ReleaseNotesScreen(onDismiss: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) Color(0xFF0A0F1A) else Color(0xFFF7FAFF)
    val cardBg = if (isDark) Color(0xFF0B1220) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE2E8F0)
    val titleColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val isEnglish = isEnglishUi()

    var notes by remember { mutableStateOf<List<RNNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("admin_release_notes")
            .whereEqualTo("published", true)
            .get()
            .addOnSuccessListener { snap ->
                notes = snap.documents.mapNotNull { d ->
                    val version = d.getString("version") ?: return@mapNotNull null
                    val dateMs = d.getTimestamp("date")?.toDate()?.time ?: 0L
                    val rawItems = d.get("items") as? List<*> ?: emptyList<Any>()
                    val items = rawItems.mapNotNull { r ->
                        (r as? Map<*, *>)?.let {
                            RNItem(
                                category = it["category"] as? String ?: return@mapNotNull null,
                                text = it["text"] as? String ?: return@mapNotNull null
                            )
                        }
                    }
                    RNNote(d.id, version, dateMs, items)
                }.sortedByDescending { it.dateMs }
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            // Top bar
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart).size(40.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBackIosNew, null,
                        tint = titleColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    if (isEnglish) "What's New" else "Novidades do App",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = titleColor
                )
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(36.dp)
                    )
                }
                notes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF2563EB).copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            if (isEnglish) "Nothing new yet" else "Nenhuma novidade ainda",
                            color = subColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            if (isEnglish) "Updates coming soon!" else "Em breve novidades por aqui!",
                            color = subColor.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }
                }
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    notes.forEachIndexed { index, note ->
                        RNNoteCard(
                            note = note,
                            isDark = isDark,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            titleColor = titleColor,
                            isEnglish = isEnglish,
                            isLatest = index == 0
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RNNoteCard(
    note: RNNote,
    isDark: Boolean,
    cardBg: Color,
    cardBorder: Color,
    titleColor: Color,
    isEnglish: Boolean,
    isLatest: Boolean
) {
    val dateStr = if (note.dateMs > 0L)
        SimpleDateFormat("d MMM yyyy", Locale("pt", "BR")).format(java.util.Date(note.dateMs))
    else ""

    val gradientStart = if (isDark) Color(0xFF1E3A5F) else Color(0xFF1D4ED8)
    val gradientEnd = if (isDark) Color(0xFF0D2137) else Color(0xFF2563EB)

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .background(cardBg)
    ) {
        Column {
            // Version header with gradient
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(gradientStart, gradientEnd)))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isEnglish) "Version ${note.version}" else "Versão ${note.version}",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            if (isLatest) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isEnglish) "LATEST" else "ATUAL",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                        if (dateStr.isNotEmpty()) {
                            Text(dateStr, color = Color.White.copy(alpha = 0.72f), fontSize = 13.sp)
                        }
                    }
                }
            }

            // Category sections
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                RN_CATEGORY_ORDER.forEach { cat ->
                    val catItems = note.items.filter { it.category == cat }
                    if (catItems.isNotEmpty()) {
                        val style = RN_CATEGORIES[cat] ?: return@forEach
                        RNCategorySection(style, catItems, isDark, isEnglish)
                    }
                }
            }
        }
    }
}

@Composable
private fun RNCategorySection(
    style: RNCategory,
    items: List<RNItem>,
    isDark: Boolean,
    isEnglish: Boolean
) {
    val label = if (isEnglish) style.labelEn else style.label
    val chipBg = if (isDark) style.color.copy(alpha = 0.18f) else style.bgLight
    val textColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(chipBg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(style.emoji, fontSize = 12.sp)
            Text(
                label,
                color = style.color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEach { item ->
                Row(
                    Modifier.padding(start = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(style.color.copy(alpha = 0.55f))
                    )
                    Text(item.text, color = textColor, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
