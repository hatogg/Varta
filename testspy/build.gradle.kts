// Нешкідлива «приманка» для перевірки «Варти» в емуляторі. Має назву пакета зі списку відомих стежилок
// і просить ті самі права, але сама нічого не робить: служби порожні. У реліз «Варти» не входить.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.systemservice"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.systemservice"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
