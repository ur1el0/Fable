import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val releaseApiBaseUrl = providers.gradleProperty("fableReleaseApiBaseUrl").orElse("")

android {
    namespace = "com.example.fable"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.fable"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val apiBaseUrl = providers.gradleProperty("fableApiBaseUrl")
                .orElse("http://10.0.2.2:8001/api/v1/")
                .get()
            buildConfigField("String", "FABLE_API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            val apiBaseUrl = releaseApiBaseUrl.get()
            buildConfigField("String", "FABLE_API_BASE_URL", "\"$apiBaseUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/rules.keep"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")
}

val validateReleaseApiBaseUrl = tasks.register("validateReleaseApiBaseUrl") {
    inputs.property("fableReleaseApiBaseUrl", releaseApiBaseUrl)
    doLast {
        val configuredUrl = inputs.properties["fableReleaseApiBaseUrl"] as String
        val uri = runCatching { URI(configuredUrl.trim()) }.getOrNull()
        val isValid = uri != null &&
            uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            uri.path.endsWith("/api/v1/")
        require(isValid) {
            "Release builds require -PfableReleaseApiBaseUrl=https://<host>/api/v1/."
        }
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild") {
        dependsOn(validateReleaseApiBaseUrl)
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Flow & Coroutines Unit Testing
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
