
import me.champeau.jmh.JMHTask

plugins {
    id("java-library")
    id("me.champeau.jmh")
}

apply(from = "../gradle/kotlin.gradle")
apply(from = "../gradle/publish.gradle")
apply(from = "../gradle/dokka.gradle")
apply(from = "../gradle/detekt.gradle")

description = "Test cases for HTTP client and server adapters."

tasks.named<JavaCompile>("jmhCompileGeneratedClasses") {
    targetCompatibility = "17"
}

tasks.withType<JMHTask> {
    val jhmVersion = properties["jhmVersion"] as? String ?: "1.34"

    jmhVersion.set(jhmVersion)
    benchmarkMode.set(listOf("thrpt"))

    iterations.set(5)
    batchSize.set(1)
    fork.set(1)
    operationsPerInvocation.set(5)
    timeOnIteration.set("1s")

    warmup.set("1s")
    warmupBatchSize.set(5)
    warmupIterations.set(1)
}

dependencies {
    val junitVersion = properties["junitVersion"]
    val swaggerParserVersion = properties["swaggerParserVersion"]
    val gatlingVersion = properties["gatlingVersion"]

    "api"(project(":logging_slf4j_jul"))
    "api"(project(":serialization"))
    "api"(project(":http_client"))
    "api"(project(":http_server"))
    "api"("org.jetbrains.kotlin:kotlin-test")
    "api"("io.swagger.parser.v3:swagger-parser:$swaggerParserVersion")
    "api"("org.junit.jupiter:junit-jupiter:$junitVersion")
    "api"("io.gatling.highcharts:gatling-charts-highcharts:$gatlingVersion")

    "testImplementation"(project(":http_client_jetty"))
    "testImplementation"(project(":http_server_jetty"))
    "testImplementation"(project(":serialization_jackson_json"))
    "testImplementation"(project(":serialization_jackson_yaml"))
}
