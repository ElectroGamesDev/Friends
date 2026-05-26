plugins {
    java
}

group = "com.electro"
version = "1.1.1"

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/release/")
    //maven("https://maven.hytale.com/pre-release/")
}

dependencies {
    compileOnly("com.hypixel.hytale:Server:latest.release")
    //compileOnly("com.hypixel.hytale:Server:0.5.0-pre.9.1")

    implementation(files("libs/HyUI-0.8.4-all.jar"))

    implementation("com.mysql:mysql-connector-j:9.1.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}
