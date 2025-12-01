package org.example.project.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.PdfInfo
import java.io.File
import javax.swing.JFileChooser

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