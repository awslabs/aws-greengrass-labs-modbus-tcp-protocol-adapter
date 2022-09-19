import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.7.10"
    application
}

group = "aws.greengrass.labs.modbustcp"
version = "1.0.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://d2jrmugq4soldf.cloudfront.net/snapshots")
    }
}

val kotlinVersion = "1.7.10"
val coroutinesVersion = "1.6.4"
val serializationVersion = "1.4.0"
val digitalpetriModbusVersion = "1.2.0"
val ioTDeviceSdkVersion = "1.10.3"
val greengrassNucleusVersion = "2.7.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", coroutinesVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", coroutinesVersion)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-json", serializationVersion)

    implementation("com.digitalpetri.modbus", "modbus-master-tcp", digitalpetriModbusVersion)
    implementation("io.netty", "netty-handler", "4.1.48.Final")

    implementation("software.amazon.awssdk.iotdevicesdk", "aws-iot-device-sdk", ioTDeviceSdkVersion)

    testImplementation(kotlin("test", kotlinVersion))
    testImplementation("io.mockk", "mockk", "1.12.8")
    testImplementation("com.github.stefanbirkner", "system-lambda", "1.2.1")
    testImplementation("com.aws.greengrass", "nucleus", greengrassNucleusVersion)
    testImplementation("com.aws.greengrass", "nucleus", greengrassNucleusVersion, classifier="tests")
    testImplementation("com.digitalpetri.modbus", "modbus-slave-tcp", digitalpetriModbusVersion)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
