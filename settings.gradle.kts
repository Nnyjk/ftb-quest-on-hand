pluginManagement {
    repositories {
        maven {
            name = 'NeoForge'
            url = 'https://maven.neoforged.net/releases'
        }
        maven {
            name = 'MinecraftForge'
            url = 'https://maven.minecraftforge.net/releases'
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0'
}

rootProject.name = 'plan-board'
