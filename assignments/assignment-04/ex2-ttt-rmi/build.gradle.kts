plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ./gradlew runServer   -> starts the RMI registry and the game server
tasks.register<JavaExec>("runServer") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ttt.server.ServerMain")
}

// ./gradlew runClient --console=plain   -> console client (run it in two terminals)
tasks.register<JavaExec>("runClient") {
    group = "application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ttt.client.ClientMain")
    standardInput = System.`in`
}
