plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.loginandregistration"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.loginandregistration"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            // Disable debugging features for better performance testing
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // Enable View Binding and BuildConfig
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    // Performance optimizations
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom)) // Firebase BOM manages all Firebase library versions
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx) // Version managed by Firebase BOM
    implementation(libs.firebase.firestore.ktx) // Version managed by Firebase BOM
    implementation(libs.firebase.storage.ktx) // Version managed by Firebase BOM
    implementation(libs.firebase.messaging.ktx) // Version managed by Firebase BOM
    implementation(libs.firebase.appcheck.playintegrity) // Firebase App Check with Play Integrity
    implementation(libs.glide) // For image loading
    annotationProcessor(libs.glide.compiler) // Glide annotation processor
    
    // Splash Screen API for Android 12+
    implementation(libs.androidx.core.splashscreen)
    
    // Admin Dashboard Dependencies
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.mp.android.chart) // For charts
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) // For lifecycleScope
    implementation(libs.androidx.work.runtime.ktx) // For background jobs
    
    // Kotlin Coroutines Dependencies
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services) // For Firebase await()
    
    // PDF Generation Dependencies
    implementation(libs.itext7.core) // For PDF generation
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.play.services.auth) // For Google Sign-In
}
