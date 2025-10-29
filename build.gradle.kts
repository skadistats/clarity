plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

group = "com.skadistats"
version = "3.1.2"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.skadistats:clarity-protobuf:[5.3,6.0)")
    api("org.xerial.snappy:snappy-java:1.1.10.4")
    api("org.slf4j:slf4j-api:2.0.7")
    api("org.atteo.classindex:classindex:3.13")
    annotationProcessor("org.atteo.classindex:classindex:3.13")
    testImplementation("org.testng:testng:7.8.0")
}

tasks.named<Test>("test") {
    useTestNG()
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

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

