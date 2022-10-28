plugins {
    id("org.orph2020.pst.common-plugin")
}

dependencies {

    implementation("io.quarkus:quarkus-smallrye-graphql")
    implementation("io.quarkus:quarkus-kafka-streams")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    implementation("io.quarkus:quarkus-rest-client-reactive-jackson")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-resteasy-reactive")
    testImplementation("io.rest-assured:rest-assured")
}

version = "0.1"
