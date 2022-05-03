pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    plugins {
        kotlin("jvm") version "1.6.10"
        id("org.jetbrains.compose") version "1.1.0"
    }
}

rootProject.name = "kotter"

include(":kotter")

include(":examples:anim")
include(":examples:blink")
include(":examples:border")
include(":examples:clock")
include(":examples:compiler")
include(":examples:doomfire")
include(":examples:extend")
include(":examples:input")
include(":examples:keys")
include(":examples:life")
include(":examples:picker")
include(":examples:sliding")
include(":examples:snake")
include(":examples:text")
include(":examples:wordle")

include(":examples:mosaic:counter")
include(":examples:mosaic:jest")
include(":examples:mosaic:robot")