plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    // M3 Expressive needs Compose libraries newer than Kotlin 1.9's compiler
    // extension supports, so this redesign moves the project onto Kotlin 2.x's
    // own Compose compiler plugin (replaces the old composeOptions block).
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}
