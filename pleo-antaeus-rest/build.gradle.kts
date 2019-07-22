plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))
    implementation( "org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.apache.logging.log4j:log4j-api:2.11.1")
    implementation("org.apache.logging.log4j:log4j-core:2.11.1")
    implementation("io.javalin:javalin:2.6.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.8")
}
