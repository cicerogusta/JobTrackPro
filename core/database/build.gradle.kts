plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.app.core.database"
    compileSdk = 37
    defaultConfig { minSdk = 26; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures {   }
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(project(":core:model"))
    implementation(libs.room)
    implementation(libs.coroutines)
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.room.test)
}
