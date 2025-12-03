import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation("org.jetbrains.compose.material:material-icons-extended:1.6.0")
//            implementation("org.jetbrains.compose.components:components-resource:1.6.1")
            implementation("org.jetbrains.compose.components:components-splitpane:1.6.1")
//            implementation("org.jetbrains.compose.components:components-file-picker:1.6.1")


            implementation("io.ktor:ktor-client-core:2.3.7")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            implementation("io.ktor:ktor-client-cio:2.3.7")
            implementation("io.ktor:ktor-client-logging:2.3.7")
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"


            buildTypes.release.proguard { isEnabled = false }

        nativeDistributions {
            targetFormats( TargetFormat.Msi,
                TargetFormat.Dmg,
                TargetFormat.Deb,
                TargetFormat.Exe)
            packageName = "pdfdesktop"
            packageVersion = "1.0.0"


            val iconPath = "composeApp/src/jvmMain/composeResources/files/"

            windows {
                iconFile.set(file("src/jvmMain/composeResources/files/appicon.ico"))
            }
            macOS {
                iconFile.set(file("src/jvmMain/composeResources/files/appicon.icns"))
            }
            linux {
                iconFile.set(file("src/jvmMain/composeResources/files/appicon.png"))
            }
        }
    }
}