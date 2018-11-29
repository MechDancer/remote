import com.novoda.gradle.release.PublishExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {

	repositories {
		mavenCentral()
		jcenter()
	}

	dependencies {
		classpath("org.jetbrains.dokka:dokka-gradle-plugin")
		classpath("com.novoda:bintray-release:+")
	}
}

plugins {
    kotlin("jvm") version "1.3.10"
	id("org.jetbrains.dokka") version "0.9.16"
}

apply {
	plugin("com.novoda.bintray-release")
}

group = "org.mechdancer"
version = "0.2.0-dev-4"

repositories {
	mavenCentral()
}

dependencies {
	compile(kotlin("stdlib-jdk8"))
	testCompile("junit", "junit", "+")
    compile(kotlin("reflect"))
}

configure<JavaPluginConvention> {
	sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "1.8"
}

configure<PublishExtension> {
	userOrg = "mechdancer"
	groupId = "org.mechdancer"
	artifactId = "remote"
	publishVersion = version.toString()
	desc = "communication lib"
	website = "https://github.com/MechDancer/remote"
}
