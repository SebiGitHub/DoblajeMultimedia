plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.dobalejemultimedia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dobalejemultimedia"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)


    // Soporte para AppCompat
    implementation ("androidx.appcompat:appcompat:1.6.1")
    // Layouts modernos
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    // Material Design components (opcional, pero recomendado)
    implementation ("com.google.android.material:material:1.6.1")
    // CardView para las tarjetas del menú
    implementation ("androidx.cardview:cardview:1.0.0")
    // ExoPlayer para la reproducción de vídeo
    implementation ("com.google.android.exoplayer:exoplayer:2.19.1")



    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}