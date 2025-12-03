//package org.example.project.network
//
//import io.ktor.client.HttpClient
//import io.ktor.client.call.body
//import io.ktor.client.engine.cio.CIO
//import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
//import io.ktor.client.plugins.logging.LogLevel
//import io.ktor.client.plugins.logging.Logging
//import io.ktor.client.request.forms.MultiPartFormDataContent
//import io.ktor.client.request.forms.formData
//import io.ktor.client.request.get
//import io.ktor.client.request.post
//import io.ktor.client.request.setBody
//import io.ktor.http.Headers
//import io.ktor.http.HttpHeaders
//import io.ktor.serialization.kotlinx.json.json
//import kotlinx.serialization.json.Json
//import org.example.project.PdfInfo
//import java.io.File
//import javax.swing.JFileChooser
//
//// ============================================
//// NETWORK CLIENT (fixed + final)
//// ============================================
//
//val client = HttpClient(CIO) {
//    install(Logging) { level = LogLevel.ALL }                 // Show logs ✔
//    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true}) }
//}
//
//private fun U(ip:String)= if(ip.contains(":"))"http://$ip" else {" http://$ip:8080"}
//
//// GET
//suspend fun getDevice(ip:String)= client.get("${U(ip)}/pdfs/device").body<List<PdfInfo>>()
//suspend fun getVault(ip:String)= client.get("${U(ip)}/pdfs/vault").body<List<PdfInfo>>()
//
//// DOWNLOAD
//fun saveFile(defaultName: String, onResult: (File?) -> Unit) {
//    val chooser = JFileChooser().apply {
//        selectedFile = File(defaultName)
//        dialogTitle = "Save PDF"
//    }
//
//    val result = chooser.showSaveDialog(null)
//    if (result == JFileChooser.APPROVE_OPTION) {
//        onResult(chooser.selectedFile)
//    } else onResult(null)
//}
//// UPLOAD
//suspend fun upload(ip:String,file:File,loc:String)= client.post("${U(ip)}/pdfs/upload/$loc") {
//    setBody(MultiPartFormDataContent(
//        formData { append("file",file.readBytes(),Headers.build {
//            append(HttpHeaders.ContentDisposition,"filename=${file.name}")
//        }) }
//    ))
//}
//
//// OPEN FILE PICKER
//fun pickFile(onPick:(File)->Unit){
//    JFileChooser().apply {
//        if(showOpenDialog(null)==JFileChooser.APPROVE_OPTION) onPick(selectedFile)
//    }
//}

package org.example.project.network

import io.ktor.client. HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io. ktor.client.plugins.logging.LogLevel
import io. ktor.client.plugins.logging. Logging
import io.ktor.client.request.forms.MultiPartFormDataContent
import io. ktor.client.request.forms.formData
import io. ktor.client.request.get
import io.ktor.client. request.post
import io. ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project. PdfInfo
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io. FilenameFilter

// ============================================
// NETWORK CLIENT (fixed + final)
// ============================================

val client = HttpClient(CIO) {
    install(Logging) { level = LogLevel.ALL }                 // Show logs ✔
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys=true}) }
}

private fun U(ip:String)= if(ip.contains(":"))"http://$ip" else "http://$ip:8080"

// GET
suspend fun getDevice(ip:String)= client. get("${U(ip)}/pdfs/device").body<List<PdfInfo>>()
suspend fun getVault(ip:String)= client.get("${U(ip)}/pdfs/vault").body<List<PdfInfo>>()

// DOWNLOAD with native macOS dialog
fun saveFile(defaultName: String, onResult: (File?) -> Unit) {
    val fileDialog = FileDialog(null as Frame?, "Save PDF", FileDialog.SAVE). apply {
        file = defaultName
        isVisible = true
    }

    val selectedFile = if (fileDialog.file != null) {
        File(fileDialog.directory, fileDialog.file)
    } else null

    onResult(selectedFile)
}

// UPLOAD
suspend fun upload(ip:String,file:File,loc:String)= client.post("${U(ip)}/pdfs/upload/$loc") {
    setBody(MultiPartFormDataContent(
        formData { append("file",file.readBytes(),Headers.build {
            append(HttpHeaders. ContentDisposition,"filename=${file. name}")
        }) }
    ))
}

// OPEN FILE PICKER with native macOS dialog
fun pickFile(onPick:(File)->Unit){
    val fileDialog = FileDialog(null as Frame?, "Select PDF", FileDialog.LOAD). apply {
        // Optional: Filter for PDF files only
        filenameFilter = FilenameFilter { _, name ->
            name.lowercase().endsWith(".pdf")
        }
        isVisible = true
    }

    if (fileDialog.file != null) {
        val selectedFile = File(fileDialog. directory, fileDialog.file)
        onPick(selectedFile)
    }
}

// Optional: Pick multiple files (if needed in future)
fun pickFiles(onPick:(List<File>)->Unit){
    val fileDialog = FileDialog(null as Frame?, "Select PDFs", FileDialog.LOAD). apply {
        isMultipleMode = true
        filenameFilter = FilenameFilter { _, name ->
            name.lowercase().endsWith(".pdf")
        }
        isVisible = true
    }

    val files = fileDialog.files. toList()
    if (files.isNotEmpty()) {
        onPick(files)
    }
}