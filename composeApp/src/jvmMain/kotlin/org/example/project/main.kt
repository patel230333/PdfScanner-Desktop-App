package org.example.project

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import javax.swing.Painter

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PDF Desktop Sync",
    ) {
        PdfApp()
    }
}

