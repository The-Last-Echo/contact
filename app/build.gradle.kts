plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.thelastecho.contact"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thelastecho.contact"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    implementation("net.zetetic:sqlcipher-android:4.20.0@aar")
    implementation("androidx.sqlite:sqlite:2.7.1")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(platform("androidx.compose:compose-bom:2025.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val verifyPrivacyBoundary = tasks.register("verifyPrivacyBoundary") {
    dependsOn("processDebugMainManifest", "processReleaseMainManifest")

    doLast {
        val internetPermission = Regex(
            """(?is)<uses-permission(?:-[\w-]+)?\b[^>]*android:name\s*=\s*["']android\.permission\.INTERNET["']"""
        )
        val disallowedDependency = Regex(
            """(?i)(analytics|telemetry|tracking|crash(?:lytics|report|reporting)?|bugsnag|sentry|firebase|admob|advertis|appsflyer|adjust|mixpanel|amplitude|segment|posthog|matomo|braze|kochava|onesignal)"""
        )

        listOf("debug", "release").forEach { variant ->
            val mergedManifest = layout.buildDirectory
                .file(
                    "intermediates/merged_manifest/$variant/" +
                        "process${variant.replaceFirstChar { it.uppercase() }}MainManifest/AndroidManifest.xml",
                )
                .get()
                .asFile
            check(mergedManifest.isFile) {
                "Merged $variant manifest was not produced: $mergedManifest"
            }

            check(!internetPermission.containsMatchIn(mergedManifest.readText())) {
                "The merged $variant manifest must not request android.permission.INTERNET."
            }

            val forbiddenArtifacts = configurations
                .getByName("${variant}RuntimeClasspath")
                .resolvedConfiguration
                .resolvedArtifacts
                .filter { artifact ->
                    disallowedDependency.containsMatchIn(artifact.moduleVersion.id.toString()) ||
                        disallowedDependency.containsMatchIn(artifact.name)
                }
            check(forbiddenArtifacts.isEmpty()) {
                "Disallowed analytics, advertising, or reporting dependencies in $variant: " +
                    forbiddenArtifacts.joinToString { it.moduleVersion.id.toString() }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyPrivacyBoundary)
}
