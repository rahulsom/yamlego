plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    alias(libs.plugins.waenaPublished)
}

dependencies {
    api(libs.commons.math3)
    implementation(libs.guava)
    implementation(libs.snakeyaml.engine)
    
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.11.1")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
