plugins { kotlin("jvm") version "1.5.21" }

group = "org.mechdancer"
version = "0.2.1-dev-13"

repositories { mavenCentral() }
dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-api:+")
    implementation(fileTree("libs"))

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:+")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:+")
}

java { withSourcesJar(); withJavadocJar() }
tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
tasks.test { useJUnitPlatform() }

//plugins {
//    kotlin("jvm") version "1.3.50" apply (true)
//    id("org.jetbrains.dokka") version "0.10.0"
//    id("com.jfrog.bintray") version "1.8.4"
//    `maven-publish`
//    `build-scan`
//}
//
//buildScan {
//    termsOfServiceUrl = "https://gradle.com/terms-of-service"
//    termsOfServiceAgree = "yes"
//    publishAlways()
//}
//
//group = "org.mechdancer"
//version = "0.2.1-dev-13"
//
//repositories {
//    mavenCentral()
//    jcenter()
//}
//dependencies {
//    implementation(kotlin("stdlib-jdk8"))
//    implementation(kotlin("reflect"))
//    implementation("org.mechdancer", "dependency", "0.1.0-rc-3")
//    implementation("org.slf4j", "slf4j-api", "1.7.29")
//
//    testImplementation("junit", "junit", "+")
//    testImplementation(kotlin("test-junit"))
//}
//tasks.withType<KotlinCompile> {
//    kotlinOptions {
//        jvmTarget = "1.8"
//    }
//}
//
//tasks.dokka {
//    outputFormat = "javadoc"
//    outputDirectory = "$buildDir/javadoc"
//}
//
//val doc = tasks.register<Jar>("javadocJar") {
//    group = JavaBasePlugin.DOCUMENTATION_GROUP
//    description = "Assembles Kotlin docs with Dokka"
//    archiveClassifier.set("javadoc")
//    from(tasks.dokka)
//}
//
//val sources = tasks.register<Jar>("sourcesJar") {
//    group = JavaBasePlugin.BUILD_TASK_NAME
//    description = "Creates sources jar"
//    archiveClassifier.set("sources")
//    from(sourceSets.main.get().allSource)
//}
//
//val fat = tasks.register<Jar>("fatJar") {
//    group = JavaBasePlugin.BUILD_TASK_NAME
//    description = "Packs binary output with dependencies"
//    archiveClassifier.set("all")
//    from(sourceSets.main.get().output)
//    from({
//        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
//    })
//}
//
//tasks.register("allJars") {
//    group = JavaBasePlugin.BUILD_TASK_NAME
//    description = "Assembles all jars in one task"
//    dependsOn(doc, sources, fat, tasks.jar)
//}
//
//val rename = tasks.register("renamePomFile") {
//    dependsOn(tasks.publishToMavenLocal)
//    doLast {
//        val path = "${buildDir.absolutePath}/publications/maven/"
//        val old = File(path + "pom-default.xml")
//        val f = File("$path${project.name}-$version.pom")
//        old.renameTo(f)
//    }
//}
//
//tasks.bintrayUpload.configure {
//    dependsOn(rename)
//}
//
//bintray {
//    user = "berberman"
//    key = System.getenv("BintrayToken")
//    setConfigurations("archives")
//    val v = version.toString()
//    val url = "https://github.com/MechDancer/remote/"
//    publish = true
//    pkg.apply {
//        name = project.name
//        desc = "a communication lib supporting UDP multicast."
//        repo = "maven"
//        userOrg = "mechdancer"
//        githubRepo = "MechDancer/remote"
//        vcsUrl = "$url.git"
//        issueTrackerUrl = "$url/issues"
//        publicDownloadNumbers = true
//        setLicenses("WTFPL")
//        version.apply {
//            name = v
//            vcsTag = v
//            websiteUrl = "$url/releases/tag/$v"
//        }
//    }
//}
//
//publishing {
//
//    repositories {
//        maven("$buildDir/repo")
//        maven {
//            name = "GitHubPackages"
//            url = uri("https://maven.pkg.github.com/MechDancer/remote")
//            credentials {
//                username = "MechDancerProject"
//                password = System.getenv("GitHubToken")
//            }
//        }
//    }
//
//
//    publications {
//        create<MavenPublication>("maven") {
//            from(components["java"])
//        }
//    }
//}
//
//artifacts {
//    add("archives", tasks.jar)
//    add("archives", fat)
//    add("archives", sources)
//    add("archives", doc)
//    add("archives", File("${buildDir.absolutePath}/publications/maven/${project.name}-$version.pom"))
//}
