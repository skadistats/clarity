plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.gradlex.extra-java-module-info") version "1.14"
}

group = "com.skadistats"
version = "4.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    create("processor") {
        java.srcDir("src/processor/java")
        resources.srcDir("src/processor/resources")
    }
}

repositories {
    mavenCentral()
}

val processorImplementation by configurations

dependencies {
    processorImplementation("com.palantir.javapoet:javapoet:0.14.0")
    api("com.skadistats:clarity-protobuf:[6.0,7.0)")
    api("org.xerial.snappy:snappy-java:1.1.10.4")
    api("org.slf4j:slf4j-api:2.0.7")
    annotationProcessor(sourceSets["processor"].output)
    annotationProcessor("com.palantir.javapoet:javapoet:0.14.0")
    testImplementation("org.testng:testng:7.8.0")
}

extraJavaModuleInfo {
    failOnMissingModuleInfo.set(false)
    automaticModule("org.xerial.snappy:snappy-java", "snappy.java")
}

tasks.named("compileJava") {
    dependsOn("compileProcessorJava")
}

tasks.named<ProcessResources>("processProcessorResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Jar>("jar") {
    from(sourceSets["processor"].output)
    from(configurations.named("processorRuntimeClasspath").map { config ->
        config.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named<Test>("test") {
    useTestNG()
}

tasks.named<Javadoc>("javadoc") {
    source(files("build/generated/sources/annotationProcessor/java/main"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set(rootProject.name)
                description.set("Clarity is an open source replay parser for Dota 2, CSGO, CS2 and Deadlock written in Java.")
                url.set("https://github.com/skadistats/clarity")
                licenses {
                    license {
                        name.set("BSD style license")
                        url.set("https://github.com/skadistats/clarity/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("spheenik")
                        name.set("Martin Schrodt")
                        email.set("github@martin.schrodt.org")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:skadistats/clarity.git")
                    developerConnection.set("scm:git:git@github.com:skadistats/clarity.git")
                    url.set("https://github.com/skadistats/clarity")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    setRequired({
        gradle.taskGraph.allTasks.any { it.name.startsWith("publishAggregation") }
    })
    sign(publishing.publications["mavenJava"])
}

