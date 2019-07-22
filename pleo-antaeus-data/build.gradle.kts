plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    implementation( "com.github.debop:koda-time:1.2.1")
    implementation(project(":pleo-antaeus-models"))
}
