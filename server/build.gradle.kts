plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
    application
}

kotlin {
    jvm {
        withJava()
        mainRun {
            mainClass.set("com.mafia.server.ApplicationKt")
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.shared)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.server.cors)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.exposed.core)
            implementation(libs.exposed.dao)
            implementation(libs.exposed.jdbc)
            implementation(libs.postgresql)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.logback)
        }
        jvmTest.dependencies { implementation(kotlin("test")) }
    }
}

application {
    mainClass.set("com.mafia.server.ApplicationKt")
}

tasks.shadowJar {
    archiveFileName.set("mafia-server.jar")
    archiveClassifier.set("")

    manifest {
        attributes["Main-Class"] = "com.mafia.server.ApplicationKt"
    }

    mergeServiceFiles()

    // This is the key fix — get the JVM compilation output + its runtime classpath
    val jvmMain = kotlin.jvm().compilations["main"]
    from(jvmMain.output.allOutputs)
    configurations = listOf(jvmMain.runtimeDependencyFiles as Configuration)

    // Ensure shadowJar runs after compilation
    dependsOn(tasks.named("jvmJar"))
}