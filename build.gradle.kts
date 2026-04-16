plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.gradlex.extra-java-module-info") version "1.14"
    id("me.champeau.jmh") version "0.7.2"
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
    jmhRuntimeOnly("ch.qos.logback:logback-classic:1.5.20")
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

tasks.register("bench") {
    description = "Run the entity state benchmark harness. Pass args with -PbenchArgs=\"...\"."
    group = "benchmark"
    dependsOn("jmhCompileGeneratedClasses")
    doLast {
        val cp = (files(
            "build/jmh-generated-classes",
            "build/jmh-generated-resources",
        ) + sourceSets["jmh"].runtimeClasspath).asPath
        val javaHome = System.getProperty("java.home")
        val userArgs = (project.findProperty("benchArgs") as? String)
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val cmd = listOf(
            "$javaHome/bin/java",
            "-Xmx4g",
            "-cp", cp,
            "skadistats.clarity.bench.Main",
        ) + userArgs
        val pb = ProcessBuilder(cmd)
        pb.directory(rootDir)
        pb.inheritIO()
        val exit = pb.start().waitFor()
        if (exit != 0) throw GradleException("bench exited with $exit")
    }
}

tasks.register<JavaExec>("smokeTraceRun") {
    description = "Run SmokeTraceMain to exercise trace capture + materialize + replay. Pass -Preplay=<path>."
    group = "benchmark"
    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("skadistats.clarity.bench.SmokeTraceMain")
    jvmArgs = listOf("-Xmx4g")
    workingDir = rootDir
    standardOutput = System.out
    errorOutput = System.err
    outputs.upToDateWhen { false }
    doFirst {
        val replay = project.findProperty("replay") as? String
            ?: throw GradleException("pass -Preplay=<path>")
        args = listOf(replay)
    }
}

tasks.register<JavaExec>("s1SmokeRun") {
    description = "Run S1SmokeMain to parse one or more S1 replays end-to-end. Pass -Preplays=\"a.dem b.dem\"."
    group = "benchmark"
    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("skadistats.clarity.bench.S1SmokeMain")
    jvmArgs = listOf("-Xmx4g")
    workingDir = rootDir
    standardOutput = System.out
    errorOutput = System.err
    outputs.upToDateWhen { false }
    doFirst {
        val replays = project.findProperty("replays") as? String
            ?: throw GradleException("pass -Preplays=\"<path1> <path2> ...\"")
        args = replays.split(" ").filter { it.isNotBlank() }
    }
}

tasks.register<JavaExec>("s1SmokeTraceRun") {
    description = "Run S1SmokeTraceMain — capture trace from S1 replay then materialize+replay per impl."
    group = "benchmark"
    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("skadistats.clarity.bench.S1SmokeTraceMain")
    jvmArgs = listOf("-Xmx4g")
    workingDir = rootDir
    standardOutput = System.out
    errorOutput = System.err
    outputs.upToDateWhen { false }
    doFirst {
        val replay = project.findProperty("replay") as? String
            ?: throw GradleException("pass -Preplay=<path>")
        args = listOf(replay)
    }
}

tasks.register("s1Bench") {
    description = "Run S1EntityStateParseBench (OBJECT_ARRAY vs FLAT) on the chosen S1 replays. Pass -PbenchArgs=\"...\"."
    group = "benchmark"
    dependsOn("jmhCompileGeneratedClasses")
    doLast {
        val cp = (files(
            "build/jmh-generated-classes",
            "build/jmh-generated-resources",
        ) + sourceSets["jmh"].runtimeClasspath).asPath
        val javaHome = System.getProperty("java.home")
        val userArgs = (project.findProperty("benchArgs") as? String)
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val cmd = listOf(
            "$javaHome/bin/java",
            "-Xmx4g",
            "-cp", cp,
            "skadistats.clarity.bench.S1Main",
        ) + userArgs
        val pb = ProcessBuilder(cmd)
        pb.directory(rootDir)
        pb.inheritIO()
        val exit = pb.start().waitFor()
        if (exit != 0) throw GradleException("s1Bench exited with $exit")
    }
}

tasks.register<JavaExec>("s1ParityCapture") {
    description = "Capture entity create/update/delete event stream for parity diffing. Pass -Preplay=<path> -Pout=<file> [-Ptype=OBJECT_ARRAY|FLAT]."
    group = "benchmark"
    classpath = sourceSets["jmh"].runtimeClasspath
    mainClass.set("skadistats.clarity.bench.S1ParityCaptureMain")
    jvmArgs = listOf("-Xmx4g")
    workingDir = rootDir
    standardOutput = System.out
    errorOutput = System.err
    outputs.upToDateWhen { false }
    doFirst {
        val replay = project.findProperty("replay") as? String
            ?: throw GradleException("pass -Preplay=<path>")
        val out = project.findProperty("out") as? String
            ?: throw GradleException("pass -Pout=<file>")
        val type = project.findProperty("type") as? String ?: "OBJECT_ARRAY"
        val materialize = project.findProperty("materialize") as? String
        val a = mutableListOf(replay, out, type)
        if (materialize == "true") a.add("--materialize")
        args = a
    }
}

tasks.register("traceBench") {
    description = "Run the mutation-trace benchmark harness. Pass args with -PbenchArgs=\"...\"."
    group = "benchmark"
    dependsOn("jmhCompileGeneratedClasses")
    doLast {
        val cp = (files(
            "build/jmh-generated-classes",
            "build/jmh-generated-resources",
        ) + sourceSets["jmh"].runtimeClasspath).asPath
        val javaHome = System.getProperty("java.home")
        val userArgs = (project.findProperty("benchArgs") as? String)
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val cmd = listOf(
            "$javaHome/bin/java",
            "-cp", cp,
            "skadistats.clarity.bench.TraceMain",
        ) + userArgs
        val pb = ProcessBuilder(cmd)
        pb.directory(rootDir)
        pb.inheritIO()
        val exit = pb.start().waitFor()
        if (exit != 0) throw GradleException("traceBench exited with $exit")
    }
}

tasks.register("copyBench") {
    description = "Run the FlatCopyBench micro. Pass args with -PbenchArgs=\"...\"."
    group = "benchmark"
    dependsOn("jmhCompileGeneratedClasses")
    doLast {
        val cp = (files(
            "build/jmh-generated-classes",
            "build/jmh-generated-resources",
        ) + sourceSets["jmh"].runtimeClasspath).asPath
        val javaHome = System.getProperty("java.home")
        val userArgs = (project.findProperty("benchArgs") as? String)
            ?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        val cmd = listOf(
            "$javaHome/bin/java",
            "-Xmx16g",
            "-cp", cp,
            "skadistats.clarity.bench.FlatCopyMain",
        ) + userArgs
        val pb = ProcessBuilder(cmd)
        pb.directory(rootDir)
        pb.inheritIO()
        val exit = pb.start().waitFor()
        if (exit != 0) throw GradleException("copyBench exited with $exit")
    }
}

