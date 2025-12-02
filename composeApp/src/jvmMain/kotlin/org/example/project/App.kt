package org.example.project

import androidx.compose.animation.*
import androidx.compose.foundation. layout.*
import androidx.compose.foundation.lazy.*
import androidx. compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Upload

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx. compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

import io.ktor. client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor. client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor. client.request.forms.*
import io.ktor.client.call.*
import io.ktor. http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.example.project.network.download
import org.example.project.network.getDevice
import org.example.project.network.getVault
import org.example.project.network.upload

@kotlinx.serialization.Serializable
data class PdfInfo(val name: String, val size: Long)


@Composable
fun PdfApp() {
    var isDarkMode by remember { mutableStateOf(true) }

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme. colorScheme.background
        ) {
            PdfSyncContent(
                isDarkMode = isDarkMode,
                onToggleTheme = { isDarkMode = !isDarkMode }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSyncContent(isDarkMode: Boolean, onToggleTheme: () -> Unit) {
    val scope = rememberCoroutineScope()

    var ip by remember { mutableStateOf("10.247.207.18:8080") }
    var device by remember { mutableStateOf<List<PdfInfo>>(emptyList()) }
    var vault by remember { mutableStateOf<List<PdfInfo>>(emptyList()) }
    var status by remember { mutableStateOf("Ready to connect") }
    var statusType by remember { mutableStateOf(StatusType.IDLE) }
    var isConnected by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val downloadDir = File("Downloads-PC").apply { mkdirs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "App Icon",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "PDF Desktop Sync",
                            style = MaterialTheme.typography. headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->

        LazyColumn {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement. spacedBy(16.dp)
                )
                {
                    // Connection Card
                    Card(
                        modifier = Modifier. fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement. spacedBy(12.dp)
                        ) {
                            Text(
                                "Connection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = ip,
                                onValueChange = { ip = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Server IP Address") },
                                placeholder = { Text("10.247. 207.18:8080") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone")
                                },
                                singleLine = true,
                                enabled = ! isLoading
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            device = getDevice(ip)
                                            vault = getVault(ip)
                                            val total = device.size + vault.size
                                            status = "Connected successfully! Total PDFs: $total"
                                            statusType = StatusType.SUCCESS
                                            isConnected = true
                                        } catch (e: Exception) {
                                            status = "Connection failed: ${e.message}"
                                            statusType = StatusType.ERROR
                                            isConnected = false
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(). height(50.dp),
                                enabled = ! isLoading && ip.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.Cable, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Connect to Device", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }

                    // Status Card
                    AnimatedVisibility(visible = status.isNotEmpty()) {
                        StatusCard(status, statusType)
                    }

                    // PDFs Section
                    AnimatedVisibility(visible = isConnected) {
                        Row(
                            modifier = Modifier. fillMaxWidth(). weight(1f),
                            horizontalArrangement = Arrangement. spacedBy(16.dp)
                        ) {
                            // Device PDFs
                            PdfListCard(
                                title = "Device PDFs",
                                icon = Icons.Default.PhoneAndroid,
                                pdfs = device,
                                modifier = Modifier.weight(1f),
                                onDownload = { pdf ->
                                    scope.launch {
                                        download(ip, pdf.name, downloadDir) {
                                            status = it
                                            statusType = StatusType.INFO
                                        }
                                    }
                                }
                            )

                            // Vault PDFs
                            PdfListCard(
                                title = "Vault PDFs",
                                icon = Icons.Default.Lock,
                                pdfs = vault,
                                modifier = Modifier.weight(1f),
                                onDownload = { pdf ->
                                    scope.launch {
                                        download(ip, pdf. name, downloadDir) {
                                            status = it
                                            statusType = StatusType.INFO
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // Upload Section
                    AnimatedVisibility(visible = isConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Upload to Device
                            OutlinedButton(
                                onClick = {
                                    pickFile { file ->
                                        scope.launch {
                                            try {
                                                upload(ip, file, "device")
                                                device = getDevice(ip)
                                                status = "Uploaded to Device successfully!"
                                                statusType = StatusType.SUCCESS
                                            } catch (e: Exception) {
                                                status = "Upload failed: ${e.message}"
                                                statusType = StatusType.ERROR
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f). height(50.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Spacer(Modifier. width(8.dp))
                                Text("Upload to Device")
                            }

                            // Upload to Vault
                            Button(
                                onClick = {
                                    pickFile { file ->
                                        scope.launch {
                                            try {
                                                upload(ip, file, "vault")
                                                vault = getVault(ip)
                                                status = "Uploaded to Vault successfully!"
                                                statusType = StatusType.SUCCESS
                                            } catch (e: Exception) {
                                                status = "Upload failed: ${e.message}"
                                                statusType = StatusType.ERROR
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null)
                                Spacer(Modifier. width(8.dp))
                                Text("Upload to Vault")
                            }
                        }
                    }
                }
            }

        }

    }
}

@Composable
fun PdfListCard(
    title: String,
    icon: ImageVector,
    pdfs: List<PdfInfo>,
    modifier: Modifier = Modifier,
    onDownload: (PdfInfo) -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier. fillMaxWidth(),
                color = MaterialTheme.colorScheme. secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme. colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.weight(1f))
                    Badge {
                        Text(pdfs.size.toString())
                    }
                }
            }

            // PDF List
            if (pdfs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement. spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48. dp),
                            tint = MaterialTheme.colorScheme. onSurfaceVariant. copy(alpha = 0.5f)
                        )
                        Text(
                            "No PDFs found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant. copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier. fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pdfs) { pdf ->
                        PdfListItem(pdf, onDownload)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfListItem(pdf: PdfInfo, onDownload: (PdfInfo) -> Unit) {
    var isDownloading by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier. fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme. colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement. SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier. weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier. size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = pdf.name,
                        style = MaterialTheme.typography. bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(pdf.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            FilledTonalIconButton(
                onClick = {
                    isDownloading = true
                    onDownload(pdf)
                },
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier. size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Download, contentDescription = "Download")
                }
            }
        }
    }
}

@Composable
fun StatusCard(message: String, type: StatusType) {
    val (color, icon) = when (type) {
        StatusType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer to Icons.Default.CheckCircle
        StatusType.ERROR -> MaterialTheme.colorScheme.errorContainer to Icons.Default.Error
        StatusType.INFO -> MaterialTheme.colorScheme.tertiaryContainer to Icons.Default. Info
        StatusType. IDLE -> MaterialTheme.colorScheme.surfaceVariant to Icons.Default. Circle
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier. padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

enum class StatusType {
    SUCCESS, ERROR, INFO, IDLE
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
    }
}


fun pickFile(onFilePicked: (File) -> Unit) {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.FILES_ONLY
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        onFilePicked(chooser.selectedFile)
    }
}