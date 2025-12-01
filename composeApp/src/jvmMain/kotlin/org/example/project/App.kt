package org.example.project

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.io.File
import javax.swing.JFileChooser

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*



@kotlinx.serialization.Serializable
data class PdfInfo(val name: String, val size: Long)

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PDF Desktop Sync") {
        PdfApp()
    }
}

@Composable
fun PdfApp() {
    val scope = rememberCoroutineScope()

    var ip by remember { mutableStateOf("") }
    var device by remember { mutableStateOf<List<PdfInfo>>(emptyList()) }
    var vault by remember { mutableStateOf<List<PdfInfo>>(emptyList()) }
    var status by remember { mutableStateOf("Idle") }

    val downloadDir = File("Downloads-PC").apply { mkdirs() }

    Column(Modifier.padding(20.dp)) {

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Enter phone IP: 10.247.207.18:8080") }
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        device = getDevice(ip)
                        vault = getVault(ip)
                        status = "Connected ✔ Total PDFs = ${device.size + vault.size}"
                    } catch (e: Exception) {
                        status = "❌ ${e.message}"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Connect") }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth()) {

            // ========================================================= DEVICE PDFs
            Column(Modifier.weight(1f)) {
                Text("📂 Device PDFs")
                LazyColumn {
                    items(device) { pdf ->
                        Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pdf.name)

                            Button(
                                onClick = {
                                    scope.launch { // <── FIX HERE
                                        download(ip, pdf.name, downloadDir) { status = it }
                                    }
                                }
                            ) { Text("Download") }
                        }
                    }
                }
            }

            Spacer(Modifier.width(20.dp))

            // ========================================================= VAULT PDFs
            Column(Modifier.weight(1f)) {
                Text("🔐 Vault PDFs")
                LazyColumn {
                    items(vault) { pdf ->
                        Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(pdf.name)

                            Button(
                                onClick = {
                                    scope.launch { // <── FIX HERE
                                        download(ip, pdf.name, downloadDir) { status = it }
                                    }
                                }
                            ) { Text("Download") }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ==================== Upload Device
        Button(
            onClick = {
                pickFile { file ->
                    scope.launch { // <── FIX HERE
                        upload(ip, file, "device")
                        device = getDevice(ip)
                        status = "Uploaded → Device ✓"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Upload to DEVICE") }


        // ==================== Upload Vault
        Button(
            onClick = {
                pickFile { file ->
                    scope.launch {
                        upload(ip, file, "vault")
                        vault = getVault(ip)
                        status = "Uploaded → Vault ✓"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Upload to VAULT") }

        Spacer(Modifier.height(10.dp))
        Text("STATUS: $status")
    }
}
// ============================================
// NETWORK CLIENT (fixed + final)
// ============================================

val client = HttpClient(CIO) {
    install(Logging) { level = LogLevel.ALL }                 // Show logs ✔
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true}) }
}

private fun U(ip:String)= if(ip.contains(":"))"http://$ip" else {" http://$ip:8080"}

// GET
suspend fun getDevice(ip:String)= client.get("${U(ip)}/pdfs/device").body<List<PdfInfo>>()
suspend fun getVault(ip:String)= client.get("${U(ip)}/pdfs/vault").body<List<PdfInfo>>()

// DOWNLOAD
suspend fun download(ip:String,name:String,dir:File,cb:(String)->Unit){
    val data = client.get("${U(ip)}/pdfs/download/$name").body<ByteArray>()
    dir.resolve(name).writeBytes(data)
    cb("Saved → ${dir.absolutePath}")
}

// UPLOAD
suspend fun upload(ip:String,file:File,loc:String)= client.post("${U(ip)}/pdfs/upload/$loc") {
    setBody(MultiPartFormDataContent(
        formData { append("file",file.readBytes(),Headers.build {
            append(HttpHeaders.ContentDisposition,"filename=${file.name}")
        }) }
    ))
}

// OPEN FILE PICKER
fun pickFile(onPick:(File)->Unit){
    JFileChooser().apply {
        if(showOpenDialog(null)==JFileChooser.APPROVE_OPTION) onPick(selectedFile)
    }
}