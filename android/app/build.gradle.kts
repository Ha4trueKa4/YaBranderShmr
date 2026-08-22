plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.yandex.avatargenerator"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.yandex.avatargenerator"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Слэш на конце обязателен: без него Ktor заменит сегмент v1.
        // Локальный бэкенд поднимают через -PapiBaseUrl=... (на эмуляторе
        // машина разработчика — это 10.0.2.2, а не localhost).
        val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
            .getOrElse("http://158.160.155.144:8080/api/v1/")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        // Значение по умолчанию здесь, иначе новый build type уронит компиляцию
        buildConfigField("boolean", "NETWORK_LOGGING", "false")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "NETWORK_LOGGING", "true")
        }
        release {
            optimization {
                enable = false
            }
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
    // Язык переключается в приложении (подмена локали на корне Compose), поэтому все
    // локали должны лежать в base APK: иначе в AAB из Play на устройстве оказались бы
    // ресурсы только системной локали и смена языка ничего бы не поменяла.
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(project(":core:mvi"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}