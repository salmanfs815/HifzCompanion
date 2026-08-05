plugins {
    alias(libs.plugins.android.library)
    
}

android {
    namespace = "com.hifzcompanion.analytics"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }
}

androidComponents {
    beforeVariants(selector().all()) { it.enableAndroidTest = false }
}

dependencies {
    implementation(project(":core-domain"))
    implementation(project(":core-ui"))
    implementation(libs.hilt.android)
    testImplementation(libs.junit)
}



