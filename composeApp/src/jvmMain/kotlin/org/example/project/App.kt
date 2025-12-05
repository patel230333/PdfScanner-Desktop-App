package org.example.project

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose. foundation.clickable
import androidx. compose.foundation.layout.*
import androidx. compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation. shape.RoundedCornerShape
import androidx.compose.material. icons.Icons
import androidx.compose.material.icons.automirrored. filled.ArrowForward
import androidx.compose.material. icons.automirrored.filled.OpenInNew
import androidx.compose.material. icons.filled.*
import androidx.compose.material3.*
import androidx.compose. runtime.*
import androidx.compose.ui. Alignment
import androidx.compose.ui. Modifier
import androidx.compose.ui. draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose. ui.draw.scale
import androidx.compose.ui.graphics. Brush
import androidx. compose.ui.graphics.Color
import androidx.compose.ui. graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx. compose.ui.text.font.FontWeight
import androidx.compose.ui.text. style.TextAlign
import androidx.compose. ui.text.style.TextDecoration
import androidx. compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui. window.Window
import androidx.compose.ui. window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

import io. ktor.client.*
import io. ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins. logging.*
import io. ktor.client. request.*
import io.ktor.client.request.forms.*
import io. ktor.client. call.*
import io. ktor.http.*
import io. ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.example.project.network.client
import org.example. project.network.getDevice
import org.example.project.network. getVault
import org.example.project.network. saveFile
import org. example.project.network.upload

@kotlinx.serialization. Serializable
data class PdfInfo(val name: String, val size: Long)

// Custom Color Scheme
val customLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color. White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color. White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color. White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
)

val customDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10131C),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF10131C),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
)

@Composable
fun PdfApp() {
    var isDarkMode by remember { mutableStateOf(true) }

    MaterialTheme(
        colorScheme = if (isDarkMode) customDarkColorScheme else customLightColorScheme
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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

    // Enhanced animations
    val connectionScale by animateFloatAsState(
        targetValue = if (isConnected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring. StiffnessLow
        ), label = "connectionScale"
    )

    val headerAnimation by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow
        ), label = "headerAnimation"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.scale(headerAnimation)
                    ) {
                        AnimatedCloudIcon()
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "PDF Desktop Sync",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight. Bold
                            )
                            Text(
                                "Seamless file synchronization",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme. onPrimaryContainer. copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    AnimatedThemeToggle(isDarkMode = isDarkMode, onToggle = onToggleTheme)
                },
                colors = TopAppBarDefaults. topAppBarColors(
                    containerColor = MaterialTheme. colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 16. dp, bottomEnd = 16.dp))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                . fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme. surface
                        ),
                        radius = 1200f
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier. fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    // Enhanced Connection Card
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn()
                    ) {
                        EnhancedConnectionCard(
                            ip = ip,
                            onIpChange = { ip = it },
                            isLoading = isLoading,
                            isConnected = isConnected,
                            onConnect = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        device = getDevice(ip)
                                        vault = getVault(ip)
                                        val total = device.size + vault.size
                                        status = "Connected successfully!  Total PDFs: $total"
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
                            modifier = Modifier.scale(connectionScale)
                        )
                    }
                }

                item {
                    // Enhanced Status Card
                    AnimatedVisibility(
                        visible = status. isNotEmpty(),
                        enter = slideInHorizontally { it / 2 } + fadeIn(),
                        exit = slideOutHorizontally { -it / 2 } + fadeOut()
                    ) {
                        EnhancedStatusCard(status, statusType)
                    }
                }

                item {
                    // Enhanced PDFs Section
                    AnimatedVisibility(
                        visible = isConnected,
                        enter = expandVertically() + fadeIn()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier. fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Enhanced Device PDFs
                                EnhancedPdfListCard(
                                    title = "Device Storage",
                                    icon = Icons.Default. PhoneAndroid,
                                    pdfs = device,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme. colorScheme.primaryContainer,
                                    onDownload = { pdf -> downloadPdf(pdf, ip, scope) { newStatus, newType ->
                                        status = newStatus
                                        statusType = newType
                                    }}
                                )

                                // Enhanced Vault PDFs
                                EnhancedPdfListCard(
                                    title = "Secure Vault",
                                    icon = Icons.Default.Lock,
                                    pdfs = vault,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    onDownload = { pdf -> downloadPdf(pdf, ip, scope) { newStatus, newType ->
                                        status = newStatus
                                        statusType = newType
                                    }}
                                )
                            }

                            // Enhanced Upload Section
                            EnhancedUploadSection(
                                onUploadToDevice = {
                                    uploadFile("device", ip, scope) { file ->
                                        scope.launch {
                                            device = getDevice(ip)

                                        }
                                        status = "Uploaded to Device successfully!"
                                        statusType = StatusType. SUCCESS
                                    }
                                },
                                onUploadToVault = {
                                    uploadFile("vault", ip, scope) { file ->
                                        scope.launch {
                                            vault = getVault(ip)

                                        }
                                        status = "Uploaded to Vault successfully!"
                                        statusType = StatusType.SUCCESS
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    // Enhanced Developer Info Card
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically { it / 3 } + fadeIn()
                    ) {
                        EnhancedDeveloperInfoCard()
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCloudIcon() {
    val rotation by rememberInfiniteTransition(label = "rotation"). animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    )

    Box(
        modifier = Modifier
            . size(40.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = "App Icon",
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun AnimatedThemeToggle(isDarkMode: Boolean, onToggle: () -> Unit) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkMode) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring. DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ), label = "themeRotation"
    )

    IconButton(
        onClick = onToggle,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Icon(
            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons. Default.DarkMode,
            contentDescription = "Toggle theme",
            modifier = Modifier.rotate(rotation),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EnhancedConnectionCard(
    ip: String,
    onIpChange: (String) -> Unit,
    isLoading: Boolean,
    isConnected: Boolean,
    onConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier. fillMaxWidth(),
        colors = CardDefaults. cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults. cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier. padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme. primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Cable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme. onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "Network Connection",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Connect to your mobile device",
                        style = MaterialTheme. typography.bodyMedium,
                        color = MaterialTheme.colorScheme. onSurface. copy(alpha = 0.7f)
                    )
                }
            }

            OutlinedTextField(
                value = ip,
                onValueChange = onIpChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Server IP Address") },
                placeholder = { Text("192.168.1. 100:8080") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Button(
                onClick = onConnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = ! isLoading && ip. isNotBlank(),
                shape = RoundedCornerShape(16. dp),
                colors = ButtonDefaults. buttonColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = {
                        fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                    }, label = "buttonContent"
                ) { loading ->
                    if (loading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Connecting...")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isConnected) Icons.Default.CheckCircle else Icons. Default.Cable,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                if (isConnected) "Connected" else "Connect to Device",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight. SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedStatusCard(message: String, type: StatusType) {
    val (containerColor, contentColor, icon) = when (type) {
        StatusType.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        StatusType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        StatusType.INFO -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme. colorScheme.onTertiaryContainer,
            Icons. Default.Info
        )
        StatusType. IDLE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default. Circle
        )
    }

    Card(
        modifier = Modifier. fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = contentColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = FontWeight. Medium
            )
        }
    }
}

@Composable
fun EnhancedPdfListCard(
    title: String,
    icon: ImageVector,
    pdfs: List<PdfInfo>,
    modifier: Modifier = Modifier,
    color: Color,
    onDownload: (PdfInfo) -> Unit
) {
    val itemCount by animateIntAsState(
        targetValue = pdfs. size,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "itemCount"
    )

    Card(
        modifier = modifier.heightIn(min = 300.dp, max = 500.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Enhanced Header
            Surface(
                modifier = Modifier. fillMaxWidth(),
                color = color,
                shape = RoundedCornerShape(topStart = 20. dp, topEnd = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface. copy(alpha = 0.2f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            style = MaterialTheme. typography.titleMedium,
                            fontWeight = FontWeight. Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${itemCount} files available",
                            style = MaterialTheme. typography.bodySmall,
                            color = MaterialTheme.colorScheme. onPrimaryContainer. copy(alpha = 0.8f)
                        )
                    }
                    AnimatedCounter(count = itemCount)
                }
            }

            // PDF List
            if (pdfs.isEmpty()) {
                Box(
                    modifier = Modifier. fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme. colorScheme.onSurfaceVariant. copy(alpha = 0.4f)
                        )
                        Text(
                            "No PDFs found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme. colorScheme.onSurfaceVariant. copy(alpha = 0.7f),
                            textAlign = TextAlign. Center
                        )
                        Text(
                            "Upload files to get started",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier. fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pdfs) { index, pdf ->
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically { it * (index + 1) / 3 } + fadeIn(
                                animationSpec = tween(delayMillis = index * 50)
                            )
                        ) {
                            EnhancedPdfListItem(pdf, onDownload)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCounter(count: Int) {
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
        }, label = "counter"
    ) { targetCount ->
        Badge(
            containerColor = MaterialTheme.colorScheme. primary,
            contentColor = MaterialTheme.colorScheme. onPrimary
        ) {
            Text(
                targetCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight. Bold
            )
        }
    }
}

@Composable
fun EnhancedPdfListItem(pdf: PdfInfo, onDownload: (PdfInfo) -> Unit) {
    var isDownloading by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isDownloading) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ), label = "itemScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            . scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme. error.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = pdf.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme. colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFileSize(pdf.size),
                        style = MaterialTheme. typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant. copy(alpha = 0.7f)
                    )
                }
            }

            FilledTonalIconButton(
                onClick = {
                    isDownloading = true
                    onDownload(pdf)
                },
                enabled = !isDownloading,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary. copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                AnimatedContent(
                    targetState = isDownloading,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }, label = "downloadButton"
                ) { downloading ->
                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24. dp),
                            strokeWidth = 3. dp,
                            color = MaterialTheme.colorScheme. primary
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedUploadSection(
    onUploadToDevice: () -> Unit,
    onUploadToVault: () -> Unit
) {
    Card(
        modifier = Modifier. fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults. cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier. padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme. primaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme. onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Upload Files",
                    style = MaterialTheme. typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier. fillMaxWidth(),
                horizontalArrangement = Arrangement. spacedBy(12. dp)
            ) {
                // Upload to Device
                OutlinedButton(
                    onClick = onUploadToDevice,
                    modifier = Modifier. weight(1f). height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme. primary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder. copy(
                        width = 2.dp
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                        Text("To Device", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Upload to Vault
                Button(
                    onClick = onUploadToVault,
                    modifier = Modifier.weight(1f).height(56. dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme. colorScheme.tertiary
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Text("To Vault", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedDeveloperInfoCard() {
    val uriHandler = LocalUriHandler.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier. fillMaxWidth(),
        elevation = CardDefaults. cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(10,100,100)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                . fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isExpanded = ! isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme. primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment. Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dhruvil Patel",
                        style = MaterialTheme.typography. headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Full Stack Application Developer",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme. onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default. ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme. colorScheme.onPrimaryContainer
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        thickness = 1. dp
                    )

                    // Contact
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("mailto:developerscanmate@gmail.com") }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default. Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "developerscanmate@gmail.com",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme. primary,
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    // Expertise
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Expertise & Technologies:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        val technologies = listOf(
                            "Android & iOS Development",
                            "SwiftUI & Compose Multiplatform",
                            "Spring Boot & Ktor Backend",
                            "Go Lang & Cloud Services",
                            "API Development & Storage Systems"
                        )

                        technologies.forEach { tech ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme. colorScheme.tertiary,
                                    modifier = Modifier.size(16. dp)
                                )
                                Spacer(Modifier.width(8. dp))
                                Text(
                                    text = tech,
                                    style = MaterialTheme. typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer. copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme. colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Services Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            . clickable { uriHandler.openUri("https://engagemintlab.in") },
                        colors = CardDefaults. cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = " Custom Development Services",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme. onPrimaryContainer
                                )
                                Text(
                                    text = "Mobile Apps • Web Applications • Desktop Solutions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored. Filled.ArrowForward,
                                contentDescription = "Visit",
                                tint = MaterialTheme.colorScheme. primary,
                                modifier = Modifier. size(28.dp)
                            )
                        }
                    }

                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            . clickable { uriHandler.openUri("https://engagemintlab.in") }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement. Center,
                        verticalAlignment = Alignment. CenterVertically
                    ) {
                        Text(
                            text = "Powered by ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme. colorScheme.onPrimaryContainer. copy(alpha = 0.7f),
                            fontSize = 32.sp
                        )
                        Text(
                            text = "engagemintlab. in",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight. Bold,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme. primary,
                            textDecoration = TextDecoration.Underline
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open website",
                            tint = MaterialTheme.colorScheme. primary,
                            modifier = Modifier. size(16.dp)
                        )
                    }

                    // Platform Badge
                    Box(
                        modifier = Modifier. fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme. colorScheme.tertiary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🖥️ Built with Compose Multiplatform",
                                style = MaterialTheme.typography. labelLarge,
                                color = MaterialTheme.colorScheme. onPrimaryContainer. copy(alpha = 0.9f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper Functions
fun downloadPdf(
    pdf: PdfInfo,
    ip: String,
    scope: kotlinx.coroutines. CoroutineScope,
    onResult: (String, StatusType) -> Unit
) {
    scope.launch {
        saveFile(pdf. name) { file ->
            if (file != null) {
                try {
                    scope.launch {
                        val data = client.get("${ip}/pdfs/download/${pdf.name}"). body<ByteArray>()
                        file.writeBytes(data)
                        onResult("✅ Downloaded to ${file.absolutePath}", StatusType. SUCCESS)
                    }
                } catch (e: Exception) {
                    onResult("❌ Download failed: ${e.message}", StatusType.ERROR)
                }
            }
        }
    }
}

fun uploadFile(
    destination: String,
    ip: String,
    scope: kotlinx.coroutines. CoroutineScope,
    onSuccess: (File) -> Unit
) {
    pickFile { file ->
        scope.launch {
            try {
                if (file != null) {
                    upload(ip, file, destination)
                    onSuccess(file)
                }
            } catch (e: Exception) {
                // Handle error
            }
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
        else -> "%.2f MB". format(bytes / (1024.0 * 1024.0))
    }
}

fun pickFile(onResult: (File?) -> Unit) {
    val chooser = JFileChooser(). apply {
        fileSelectionMode = JFileChooser. FILES_ONLY
        dialogTitle = "Select PDF File"
    }

    val result = chooser.showOpenDialog(null)
    if (result == JFileChooser.APPROVE_OPTION) {
        onResult(chooser.selectedFile)
    } else onResult(null)
}
