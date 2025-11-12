import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.rahulsom.waena.WaenaExtension
import nebula.plugin.contacts.Contact

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.waenaRoot)
    alias(libs.plugins.waenaPublished).apply(false)
    alias(libs.plugins.spotless)
}

allprojects {
    group = "io.github.rahulsom"

    repositories {
        mavenCentral()
    }

    contacts {
        addPerson(
            "rahulsom@noreply.github.com",
            delegateClosureOf<Contact> {
                moniker("Rahul Somasunderam")
                roles("owner")
                github("https://github.com/rahulsom")
            },
        )
    }
}

subprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

waena {
    publishMode.set(WaenaExtension.PublishMode.Central)
}

configure<SpotlessExtension> {
    kotlin {
        target("**/*.kt")
        ktlint("1.7.1")
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint("1.7.1")
    }
}