import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.kotlin.kapt") version "1.6.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.micronaut.application") version "3.7.10"
}

version = "0.1"
group = "cz.bedla"

val kotlinVersion = project.properties["kotlinVersion"]
repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-jackson-databind")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
}


application {
    mainClass = "cz.bedla.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("11")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}
graalvmNative.toolchainDetection.set(false)
micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("cz.bedla.*")
    }
}

tasks.register("generateKotlinSource") {
    val outputDir = layout.buildDirectory.dir("generated/kotlin")

    outputs.dir(outputDir)

    doLast {
        val generatedFile = outputDir.get().asFile.resolve("generatedClasses.kt")
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(
            """
            package generated

            import io.micronaut.http.annotation.Controller
            import io.micronaut.http.annotation.Get
            import javax.annotation.Generated

            @Controller
            @Generated
            open class MyController(
                private val adapter: MyAdapter,
            ) {
                @Get("/fooo", produces = ["application/json"], consumes = ["application/json"])
                open fun doAction(): String {
                    return adapter.doAction()
                }
            }

            interface MyAdapter {
                fun doAction(): String
            }           
            """.trimIndent()
        )
        println("Generated Kotlin source at: ${generatedFile.absolutePath}")
    }
}

// Variant A
sourceSets["main"].java.srcDir(tasks.named("generateKotlinSource"))

// Variant B
//sourceSets {
//    main {
//        java {
//            srcDirs(tasks.named("generateKotlinSource"))
//        }
//    }
//}

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn("generateKotlinSource")
}