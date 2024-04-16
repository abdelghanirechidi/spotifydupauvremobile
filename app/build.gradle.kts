plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.spotifydupauvremobile"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.spotifydupauvremobile"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.media)
    implementation("androidx.core:core-ktx:1.6.0")
    implementation(libs.guava)
    implementation("io.grpc:grpc-okhttp:1.63.0")

    //implementation(files("C:\\Users\\Utilisateur\\Desktop\\Etude\\M1\\S2\\spotifydupauvremobile\\jar_files"))
    //implementation(fileTree(mapOf("dir" to "C:\\Users\\Utilisateur\\Desktop\\Etude\\M1\\S2\\spotifydupauvremobile\\jar_files", "include" to listOf<String>("*.aar", "*.jar"), "exclude" to listOf<String>())))
    implementation("com.google.cloud:google-cloud-speech:4.36.0")

    //implementation(libs.ice.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
