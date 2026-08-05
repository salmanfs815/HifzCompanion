plugins {
    alias(libs.plugins.android.library)
    
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hifzcompanion.core.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }
}

androidComponents {
    beforeVariants(selector().all()) { it.enableAndroidTest = false }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(platform(libs.androidx.compose.bom))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    implementation(libs.hilt.android)
}



