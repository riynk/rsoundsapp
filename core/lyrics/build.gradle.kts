plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.rsounds.core.lyrics"
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
    implementation(libs.snakeyaml)
    testImplementation(libs.junit)
}
