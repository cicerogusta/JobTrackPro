plugins {
    alias(libs.plugins.android.library)
}
android {
    namespace = "com.app.core.common"
    compileSdk = 37
    defaultConfig { minSdk = 26; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures {   }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.coroutines)
    implementation(libs.coroutines)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.espresso)
}
