plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    alias(libs.plugins.waenaPublished)
    id("jacoco")
}

description = "Yamlego Core module - provides parsing capability"

dependencies {
    implementation(libs.snakeyaml.engine)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
    }
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
