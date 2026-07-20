plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.realestate.app"
    compileSdk = 34


    signingConfigs {
        create("release") {
            storeFile = file("RealEstate2026")
            storePassword = "android"
            keyAlias = "realestate2026"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.realestate.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "12.0"

        // Read from local.properties
        val localProps = org.jetbrains.kotlin.konan.properties.loadProperties(
            rootProject.file("local.properties").absolutePath
        )

        val baseUrl = localProps.getProperty("BASE_URL") ?: "http://13.220.215.107:8000/api/v1/"
        val mapsKey = localProps.getProperty("GOOGLE_MAPS_API_KEY") ?: ""
        val googleClientId = localProps.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        val supabaseUrl = localProps.getProperty("SUPABASE_URL") ?: ""
        val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY") ?: ""

        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleClientId\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        manifestPlaceholders["mapsApiKey"] = mapsKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("Boolean", "USE_MOCK_DATA", "false")
        }

        debug {
            signingConfig = signingConfigs.getByName("release")
            // All ViewModels use MockData.kt; no real API calls in debug
            buildConfigField("Boolean", "USE_MOCK_DATA", "false")
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging { 
        resources { 
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        } 
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Google Auth
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Accompanist Pager
    implementation("com.google.accompanist:accompanist-pager:0.34.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")

    // UCrop — image crop after camera capture or gallery pick
    implementation("com.github.yalantis:ucrop:2.2.8")

    // Coroutines + Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt { correctErrorTypes = true }
