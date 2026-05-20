plugins {
    scala
    application
}
repositories {
    mavenCentral()
}

dependencies {
    val pekkoVersion = "1.6.0"
    val scalaVersion = "3.7.4"
    val scalaBinary = "3"

    // Runtime dependencies
    implementation("org.apache.pekko:pekko-actor-typed_$scalaBinary:$pekkoVersion")
    implementation("com.typesafe.scala-logging:scala-logging_$scalaBinary:3.9.6")
    implementation("org.scala-lang:scala3-library_$scalaBinary:$scalaVersion")
    implementation("ch.qos.logback:logback-classic:1.5.32")

    // Test dependencies
    testImplementation("org.scalatest:scalatest_$scalaBinary:3.2.20")
    testImplementation("org.apache.pekko:pekko-actor-testkit-typed_$scalaBinary:$pekkoVersion")
}

application {
    mainClass.set("alarm.Main")
}
