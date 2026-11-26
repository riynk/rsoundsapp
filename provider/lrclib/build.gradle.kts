plugins { alias(libs.plugins.android.library) }

android {
    namespace = "com.rsounds.provider.lrclib"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:lyrics"))
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
