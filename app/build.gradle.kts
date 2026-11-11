plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
if (file("google-services.json").exists()) { apply(plugin = "com.google.gms.google-services") }
android {
    namespace = "com.app.jobtrackpro"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.app.jobtrackpro"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:home"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:applications"))
    implementation(project(":feature:jobs"))
    implementation(project(":feature:settings"))
    implementation(libs.activity)
    implementation(libs.appcompat)
    implementation(libs.fragment)
    implementation(libs.navigation)
    implementation(libs.hilt.navigation)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.hilt)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.preview)
    implementation(platform(libs.compose.bom))
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    debugImplementation(libs.compose.test.manifest)
    debugImplementation(libs.compose.tooling)
}
