import com.github.rahulsom.waena.WaenaExtension
import nebula.plugin.contacts.Contact
import nebula.plugin.contacts.ContactsExtension
import org.gradle.kotlin.dsl.findByType
import kotlin.apply

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.waenaRoot)
    alias(libs.plugins.waenaPublished).apply(false)
}

allprojects {
    group = "io.github.rahulsom"

    extensions.findByType<ContactsExtension>()?.apply {
      addPerson("rahulsom@noreply.github.com", delegateClosureOf<Contact> {
        moniker("Rahul Somasunderam")
        roles("owner")
        github("https://github.com/rahulsom")
      })
    }
}

subprojects {
    repositories {
        mavenCentral()
    }
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

waena {
    publishMode.set(WaenaExtension.PublishMode.Central)
}
