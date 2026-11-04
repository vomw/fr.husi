repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        mavenContent { snapshotsOnly() }
        content {
            includeModule("io.github.xchacha20-poly1305", "kurpc")
            includeModule("io.github.xchacha20-poly1305", "kurpc-jvm")
            includeModule("io.github.xchacha20-poly1305", "kurpc-android")
            includeModule("io.github.xchacha20-poly1305", "kurpc-natives-jvm")
            includeModule("io.github.xchacha20-poly1305", "protoc-gen-kurpc")
        }
    }
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    gradlePluginPortal()
}
