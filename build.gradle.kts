plugins {
	java
	id("org.springframework.boot") version "4.1.2" apply false
	id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {
	apply(plugin = "java")
	apply(plugin = "org.springframework.boot")
	apply(plugin = "io.spring.dependency-management")

	group = "io.github.jsshin8128"
	version = "0.0.1-SNAPSHOT"

	extensions.configure<JavaPluginExtension> {
		toolchain {
			languageVersion = JavaLanguageVersion.of(21)
		}
	}

	repositories {
		mavenCentral()
	}

	tasks.withType<Test> {
		useJUnitPlatform()
	}
}
