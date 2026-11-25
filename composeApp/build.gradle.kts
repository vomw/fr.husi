import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.android.lint")
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.buildkonfig)
}

val versionNameProvider = requireMetadata("VERSION_NAME")
val versionCodeProvider = requireMetadata("VERSION_CODE")

val libcoreAarFile = layout.projectDirectory.file("libs/libcore.aar").asFile
val checkLibcoreAar = tasks.register("checkLibcoreAar") {
    description = "Fails with an explanation when the Android libcore.aar has not been built yet."
    val aarPath = libcoreAarFile.path
    doFirst {
        if (!File(aarPath).isFile) {
            error("Missing Android libcore aar '$aarPath'. Build it first: make libcore_android.")
        }
    }
}


val bundledAssetFiles = listOf("geoip.tgz", "geosite.tgz")
val bundledAssetsDir = layout.projectDirectory.dir("src/commonMain/composeResources/files/sing-box").asFile
val warnMissingAssets = tasks.register("warnMissingBundledAssets") {
    description = "Warns when the geoip/geosite assets have not been downloaded yet."
    val assetsDirPath = bundledAssetsDir.path
    val assetFileNames = bundledAssetFiles.toList()
    doFirst {
        val missing = assetFileNames.filterNot { File(assetsDirPath, it).isFile }
        if (missing.isNotEmpty()) {
            logger.warn(
                "Missing bundled route assets in '$assetsDirPath': ${missing.joinToString()}. " +
                    "The app will ship without geoip/geosite rule sets; run 'make assets' to download them.",
            )
        }
    }
}

tasks.matching { it.name.startsWith("compile") }.configureEach {
    if (name.contains("Android", ignoreCase = true)) {
        dependsOn(checkLibcoreAar)
    }
    dependsOn(warnMissingAssets)
}

buildkonfig {
    packageName = "fr.husi"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(STRING, "VERSION_NAME", versionNameProvider.get(), const = true)
        buildConfigField(INT, "VERSION_CODE", versionCodeProvider.get(), const = true)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }

    android {
        namespace = "fr.husi.lib"
        buildToolsVersion = "37.0.0"
        compileSdk {
            version = release(37) {
                minorApiLevel = 2
            }
        }
        minSdk = 24
        androidResources {
            enable = true
        }
        lint {
            checkOnly += "NewApi"
            abortOnError = true
        }
    }

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(libs.kurpc)
                implementation(libs.jetbrains.compose.foundation)
                implementation(libs.jetbrains.compose.material3)
                implementation(libs.jetbrains.compose.animation.graphics)
                implementation(libs.jetbrains.compose.components.resources)
                implementation(libs.jetbrains.compose.ui.tooling.preview)
                implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
                implementation(libs.jetbrains.lifecycle.runtime.compose)
                implementation(libs.jetbrains.navigation3.ui)
                implementation(libs.kotlinx.collections.immutable)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.sqlite.bundled)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.serialization.protobuf)
                implementation(libs.kotlinx.datetime)

                implementation(libs.ini4j)
                implementation(libs.okio)
                implementation(libs.fastscroller.material3)
                implementation(libs.filekit.dialogs.compose)
                implementation(libs.aboutlibraries.compose.m3)
                implementation(libs.haze.blur.material3)
                implementation(libs.zxing.core)
                implementation(libs.kpuri)
                implementation(libs.libghostty.compose)
                implementation(project(":proto"))
                implementation(project(":library:DragDropSwipeLazyColumn"))
                implementation(project(":library:ComposePreference"))

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.compose.viewmodel)
                implementation(libs.koin.compose.navigation3)
            }
        }
        val androidMain = getByName("androidMain") {
            languageSettings.optIn("androidx.tv.material3.ExperimentalTvMaterial3Api")
            dependencies {
                implementation(
                    fileTree("libs") {
                        include("*.aar")
                    },
                )

                implementation(libs.kotlinx.coroutines.android)

                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.work.multiprocess)
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.androidx.activity.compose)

                implementation(libs.koin.android)

                implementation(libs.shizuku.api)
                implementation(libs.shizuku.provider)
                implementation(libs.hidden.api.bypass)

                implementation(libs.accompanist.drawablepainter)

                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.compose)

                implementation(libs.smali.dexlib2.get().toString()) {
                    exclude(group = "com.google.guava", module = "guava")
                }

                implementation(libs.process.phoenix)

                implementation(libs.androidx.tv.material)
            }
        }
        val commonTest = getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
                implementation(project.dependencies.platform(libs.junit.bom))
                implementation(libs.junit.jupiter.api)
                runtimeOnly(libs.junit.platform.launcher)
                runtimeOnly(libs.junit.jupiter.engine)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "fr.husi.resources"
}

val commonAboutLibrariesDir = layout.projectDirectory.dir("src/commonMain/aboutlibraries")
aboutLibraries {
    offlineMode = true
    collect {
        configPath = commonAboutLibrariesDir.asFile
    }
    export {
        variant = "android"
        outputFile = file("src/androidMain/composeResources/files/aboutlibraries.json")
    }
}

ksp {
    arg("room.incremental", "true")
    arg("room.schemaLocation", "${projectDir}/schemas")
}

dependencies {
    kspAndroid(libs.androidx.room.compiler)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
