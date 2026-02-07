package br.com.gui.carlembrete

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AondePareiScreen(
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

    var savedLocation by remember { mutableStateOf(AppPreferences.getParkedLocation(context)) }
    val photoUris = remember { mutableStateListOf<Uri>() }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            salvarLocalizacao(context) { savedLocation = it }
        } else {
            Toast.makeText(context, "Permissão de localização negada.", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingPhotoUri?.let { uri ->
                photoUris.add(uri)
            }
        } else {
            pendingPhotoUri = null
        }
    }

    fun requestLocation() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            salvarLocalizacao(context) { savedLocation = it }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun takePhoto() {
        val uri = createTempImageUri(context)
        pendingPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    fun openMaps() {
        val location = savedLocation ?: return
        val uri = Uri.parse("google.navigation:q=${location.lat},${location.lng}&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:${location.lat},${location.lng}?q=${location.lat},${location.lng}(Estacionamento)")
            )
            context.startActivity(fallback)
        }
    }

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text("Aonde eu Parei", color = textPrimary)
                },
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
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = accentBlue)
                        Text("Local salvo", color = textPrimary, fontSize = 16.sp)
                    }
                    if (savedLocation == null) {
                        Text("Nenhuma localização salva ainda.", color = textDim, fontSize = 13.sp)
                    } else {
                        Text(
                            "Lat: %.5f  •  Lng: %.5f".format(Locale.getDefault(), savedLocation?.lat, savedLocation?.lng),
                            color = textDim,
                            fontSize = 13.sp
                        )
                        val date = Date(savedLocation?.timeMillis ?: 0L)
                        val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
                        Text("Salvo em $formatted", color = textDim, fontSize = 12.sp)
                    }
                }
            }

            Button(
                onClick = { requestLocation() },
                colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Default.LocalParking, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar localização atual")
            }

            OutlinedButton(
                onClick = { openMaps() },
                enabled = savedLocation != null,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Abrir no Maps (a pé)")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = bg),
                border = BorderStroke(1.dp, cardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = accentBlue)
                        Text("Fotos do local", color = textPrimary, fontSize = 16.sp)
                    }

                    Button(
                        onClick = { takePhoto() },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tirar foto")
                    }

                    if (photoUris.isEmpty()) {
                        Text("Nenhuma foto adicionada.", color = textDim, fontSize = 13.sp)
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(photoUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun salvarLocalizacao(
    context: Context,
    onSaved: (ParkedLocation) -> Unit
) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var best: Location? = null
    for (provider in providers) {
        try {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                best = loc
                break
            }
        } catch (_: SecurityException) {
            return
        }
    }
    if (best == null) {
        Toast.makeText(context, "Não foi possível obter a localização.", Toast.LENGTH_SHORT).show()
        return
    }
    val location = ParkedLocation(best.latitude, best.longitude, System.currentTimeMillis())
    AppPreferences.setParkedLocation(context, location.lat, location.lng, location.timeMillis)
    Toast.makeText(context, "Local salvo!", Toast.LENGTH_SHORT).show()
    onSaved(location)
}

private fun createTempImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "parked_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
