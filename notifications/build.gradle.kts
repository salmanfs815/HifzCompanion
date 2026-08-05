plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.hifzcompanion.notifications"
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
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
}



