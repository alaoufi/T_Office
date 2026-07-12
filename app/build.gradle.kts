plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.toffice.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.toffice.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 10
        versionName = "1.10"
        vectorDrawables { useSupportLibrary = true }
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.coil.compose)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation("junit:junit:4.13.2")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-text")
    // org.json حقيقي للاختبارات (نسخة أندرويد مجرّد stub يرمي "not mocked")
    testImplementation("org.json:json:20231013")
    // تنفيذ XmlPullParser حقيقي للاختبارات (قراءة DOCX)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
}
