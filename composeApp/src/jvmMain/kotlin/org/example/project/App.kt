package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import javax.swing.JFileChooser


import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

// ---------- NETWORK CLIENT (FULL FIXED) ----------

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.*


@kotlinx.serialization.Serializable
data class PdfInfo(val name: String, val size: Long)

// ---------- MAIN ----------
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "📄 PDF Sync Desktop") {
        PdfDesktopApp()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfDesktopApp() {
    val scope = rememberCoroutineScope()

    var ip by remember { mutableStateOf("") }
    var devicePdfs by remember { mutableStateOf(emptyList<PdfInfo>()) }
    var vaultPdfs by remember { mutableStateOf(emptyList<PdfInfo>()) }
    var status by remember { mutableStateOf("Idle") }

    val downloadDir = File("PDF-Downloads").apply { mkdirs() }

    MaterialTheme {
        Column(Modifier.padding(18.dp)) {

            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("Enter Phone IP (Example: 10.247.207.18:8080)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            devicePdfs = getDevicePdfs(ip)
                            vaultPdfs = getVaultPdfs(ip)
                            status = "Connected ✓ Loaded ${devicePdfs.size + vaultPdfs.size} PDFs"
                        } catch (e: Exception) {
                            status = "❌ Error: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Connect & Load PDFs") }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxSize()) {

                // ========= DEVICE ==========
                Column(Modifier.weight(1f)) {
                    Text("📁 Device PDFs", style = MaterialTheme.typography.titleLarge)
                    LazyColumn {
                        items(devicePdfs) { pdf ->
                            Row(
                                Modifier.fillMaxWidth().padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pdf.name)
                                Button(onClick = {
                                    scope.launch {
                                        downloadPdf(ip, pdf.name, downloadDir)
                                        status = "Saved → ${downloadDir.absolutePath}"
                                    }
                                }) { Text("⬇ Download") }
                            }
                        }
                    }
                }

                Spacer(Modifier.width(18.dp))

                // ========= VAULT ==========
                Column(Modifier.weight(1f)) {
                    Text("🔐 Vault PDFs", style = MaterialTheme.typography.titleLarge)
                    LazyColumn {
                        items(vaultPdfs) { pdf ->
                            Row(
                                Modifier.fillMaxWidth().padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(pdf.name)
                                Button(onClick = {
                                    scope.launch {
                                        downloadPdf(ip, pdf.name, downloadDir)
                                        status = "Saved → ${downloadDir.absolutePath}"
                                    }
                                }) { Text("⬇ Download") }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ========= UPLOAD ==========
            Button(
                onClick = {
                    val chooser = JFileChooser()
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        scope.launch {
                            uploadToDevice(ip, chooser.selectedFile)
                            devicePdfs = getDevicePdfs(ip)
                            status = "📤 Uploaded to Device"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("Upload → DEVICE") }

            Button(
                onClick = {
                    val chooser = JFileChooser()
                    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                        scope.launch {
                            uploadToVault(ip, chooser.selectedFile)
                            vaultPdfs = getVaultPdfs(ip)
                            status = "📤 Uploaded to Vault"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text("Upload → VAULT") }

            Spacer(Modifier.height(10.dp))
            Text("Status: $status")
        }
    }
}





val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

// Auto port fix -> allows typing just IP
private fun url(ip: String) = if (ip.contains(":")) "http://$ip" else "http://$ip:8080"

// ---- API Calls ----
suspend fun getDevicePdfs(ip: String) =
    client.get("${url(ip)}/pdfs/device").body<List<PdfInfo>>()

suspend fun getVaultPdfs(ip: String) =
    client.get("${url(ip)}/pdfs/vault").body<List<PdfInfo>>()

suspend fun downloadPdf(ip: String, name: String, folder: File) {
    val bytes = client.get("${url(ip)}/pdfs/download/$name").body<ByteArray>()
    folder.resolve(name).writeBytes(bytes)
}

suspend fun uploadToDevice(ip: String, file: File) = upload(ip, file, "device")
suspend fun uploadToVault(ip: String, file: File) = upload(ip, file, "vault")

private suspend fun upload(ip: String, file: File, location: String) {
    client.post("${url(ip)}/pdfs/upload/$location") {
        setBody(
            MultiPartFormDataContent(
                formData {
                    append("file", file.readBytes(), Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                        append(HttpHeaders.ContentType, "application/pdf")
                    })
                }
            )
        )
    }
}