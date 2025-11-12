import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    alias(libs.plugins.waenaPublished)
    alias(libs.plugins.testLogger)
    id("jacoco")
}

description = "Yamlego Core module - provides parsing capability"

dependencies {
    implementation(libs.snakeyaml.engine)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}

testlogger {
    theme = ThemeType.MOCHA
    showPassed = true
    showSkipped = true
    showFailed = true
}