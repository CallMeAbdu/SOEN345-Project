import java.io.File

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    id("jacoco")
}

fun loadDotEnv(file: File): Map<String, String> {
    if (!file.exists()) {
        return emptyMap()
    }
    val values = mutableMapOf<String, String>()
    file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .forEach { line ->
                val separatorIndex = line.indexOf("=")
                val key = line.substring(0, separatorIndex).trim()
                val rawValue = line.substring(separatorIndex + 1).trim()
                val value = rawValue.removePrefix("\"").removeSuffix("\"")
                if (key.isNotEmpty()) {
                    values[key] = value
                }
            }
    return values
}

fun escapeForBuildConfig(value: String): String {
    return value.replace("\\", "\\\\").replace("\"", "\\\"")
}

val dotEnvValues = loadDotEnv(rootProject.file(".env"))
val resendApiKey = dotEnvValues["RESEND_API_KEY"] ?: ""
val resendFromEmail = dotEnvValues["RESEND_FROM_EMAIL"] ?: ""

android {
    namespace = "com.soen345.project"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.soen345.project"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "RESEND_API_KEY", "\"${escapeForBuildConfig(resendApiKey)}\"")
        buildConfigField("String", "RESEND_FROM_EMAIL", "\"${escapeForBuildConfig(resendFromEmail)}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    extensions.getByType(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class.java).apply {
        isIncludeNoLocationClasses = true
        excludes = mutableListOf("jdk.internal.*")
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.espresso.intents)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-analytics")
}

// ─────────────────────────────────────────────────────────────────
// JaCoCo Report Task – generates XML report for Codecov
// ─────────────────────────────────────────────────────────────────
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree(
        "${layout.buildDirectory.get()}/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
    ) {
        exclude(fileFilter)
    }

    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java"))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
        )
    })
}
