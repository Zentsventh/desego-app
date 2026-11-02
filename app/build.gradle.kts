import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/** Servidor oficial. Para desarrollo local usa: -PdesegoApiUrl=http://10.0.2.2:8000/ */
val productionApiUrl = "https://desego.zentdev.store/"

val developmentApiUrls = providers.gradleProperty("desegoLanUrl").orNull ?: runCatching {
    Collections.list(NetworkInterface.getNetworkInterfaces())
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { Collections.list(it.inetAddresses) }
        .filterIsInstance<Inet4Address>()
        .filter { it.isSiteLocalAddress }
        .map { "http://${it.hostAddress}:8000/" }.distinct().joinToString("|")
}.getOrDefault("")

android {
    namespace = "com.desego"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.desego"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_LOCAL_URLS", "\"\"")
        buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("desegoApiUrl").orElse(productionApiUrl).get()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Keep development installs separate from an app signed on another machine.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_LOCAL_URLS", "\"$developmentApiUrls\"")
            buildConfigField("String", "API_BASE_URL", "\"${providers.gradleProperty("desegoApiUrl").orElse(productionApiUrl).get()}\"")
        }
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

