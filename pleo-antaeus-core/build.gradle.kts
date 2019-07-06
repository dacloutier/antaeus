plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.0.0")
    implementation("org.apache.logging.log4j:log4j-api:2.11.1")
    implementation("org.apache.logging.log4j:log4j-core:2.11.1")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.4")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.4")
    compile(project(":pleo-antaeus-models"))
}