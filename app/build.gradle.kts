plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android {
  namespace = "digital.alghad.protechmusic"
  compileSdk = 35
  defaultConfig {
    applicationId = "digital.alghad.protechmusic"
    minSdk = 26
    targetSdk = 35
    versionCode = 20
    versionName = "0.4.7-beta.9"
    testInstrumentationRunner = "digital.alghad.protechmusic.RcSmokeInstrumentation"
  }
  buildTypes { release { isMinifyEnabled = false } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
  buildFeatures { compose = true }
  packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}
dependencies {
  implementation("androidx.core:core-ktx:1.16.0")
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.compose.ui:ui:1.8.2")
  implementation("androidx.compose.foundation:foundation:1.8.2")
  implementation("androidx.compose.material3:material3:1.3.2")
  implementation("androidx.compose.ui:ui-tooling-preview:1.8.2")
  implementation("androidx.media3:media3-exoplayer:1.6.1")
  implementation("androidx.media3:media3-common:1.6.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  testImplementation("junit:junit:4.13.2")
}
