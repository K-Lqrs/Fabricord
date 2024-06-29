plugins {
    id("java")
}

group = "net.rk4z.fabricord"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

tasks.test {
    useJUnitPlatform()
}