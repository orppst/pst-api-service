plugins {
    id("org.orph2020.pst.common-plugin")
}
version = "0.3"

dependencies {


    implementation("io.quarkus:quarkus-smallrye-graphql")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-jwt") // AAI
    implementation("io.quarkus:quarkus-oidc")

    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-rest-client-reactive-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-kubernetes-service-binding")

//    implementation("io.quarkiverse.helm:quarkus-helm:0.1.2")

    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-keycloak-admin-client-reactive")
    implementation("io.quarkus:quarkus-resteasy-reactive-jackson")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.quarkus:quarkus-test-security-oidc")
    testImplementation("io.quarkus:quarkus-test-security")

    implementation("uk.ac.starlink:stil:4.1.4")

    implementation("commons-io:commons-io:2.15.1")
}


