import ledgerly.tokens.GenerateKotlinTokensTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("ledgerly.tokens")
}

android {
    namespace = "app.ledgerly.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.ledgerly"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// The Compose theme constants come from designTokens/tokens.json on every build.
val generateKotlinTokens = tasks.register<GenerateKotlinTokensTask>("generateKotlinTokens") {
    tokensFile.set(rootProject.layout.projectDirectory.file("designTokens/tokens.json"))
    packageName.set("app.ledgerly.android.ui.theme.tokens")
    outputDir.set(layout.buildDirectory.dir("generated/ledgerlyTokens"))
}

androidComponents {
    onVariants { variant ->
        // Registered as Java sources: the Kotlin Android plugin compiles Kotlin files from these dirs,
        // while generated `kotlin` dirs from the variant API are not passed to compileKotlin on AGP 8.
        variant.sources.java?.addGeneratedSourceDirectory(generateKotlinTokens, GenerateKotlinTokensTask::outputDir)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
}
