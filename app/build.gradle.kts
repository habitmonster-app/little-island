plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.littleisland.game"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.littleisland.game"
        minSdk = 26
        targetSdk = 34
        // CI가 빌드 번호를 넘겨주면 그걸 버전으로 씀 (덮어 설치 가능하게)
        versionCode = (System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1." + (System.getenv("BUILD_NUMBER") ?: System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    signingConfigs {
        // 테스트용 고정 키: 빌드가 바뀌어도 같은 서명이라 앱을 지우지 않고 덮어 설치 가능
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("debug") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // 광고 (AdMob). 지금은 구글 테스트 광고 ID라 진짜 돈은 안 들어온다
    implementation("com.google.android.gms:play-services-ads:23.0.0")
}
