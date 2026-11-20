buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 has built-in Kotlin. A newer KGP on the classpath moves the whole build (app and :core)
        // to it. Keep these three equal to `kotlin` in gradle/libs.versions.toml.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.21")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
