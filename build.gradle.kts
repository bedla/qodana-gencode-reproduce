import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.4.4"
    id("io.micronaut.aot") version "4.4.4"
}

version = "0.1"
group = "cz.bedla"

val kotlinVersion = project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.servlet:micronaut-servlet-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.micronaut:micronaut-http-client")
}


application {
    mainClass = "cz.bedla.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}


graalvmNative.toolchainDetection = false

micronaut {
    runtime("tomcat")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("cz.bedla.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
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
            import javax.annotation.processing.Generated

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

// Ensure generated sources are included in the compilation
sourceSets["main"].kotlin.srcDir(tasks.named("generateKotlinSource"))

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn("generateKotlinSource")
}