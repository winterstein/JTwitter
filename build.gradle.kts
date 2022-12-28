/**
 * A very simplistic gradle build script
 *
 * To publish to your local ~/.m2 run 'gradle publishToMavenLocal'
 *
 * Definitely not DRY wrt the Bob side of things. Whenever changes are made there
 * to either the version number or dependencies they need to be replicated here.
 */

tasks.wrapper {
    gradleVersion = "7.4"
    distributionType = Wrapper.DistributionType.BIN
}

plugins {
    `java-library`
    `maven-publish`
}

group = "com.winterwell"
version = "3.8.6"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.winterwell","utils","1.3.2")

    // Bob uses local files in lib
    //implementation(files("lib/commons-codec.jar"))
    //implementation(files("lib/signpost-core.jar"))
    //implementation(files("lib/signpost-commonshttp4.jar"))
    implementation("oauth.signpost", "signpost-core", "1.2.1.2")
    implementation("oauth.signpost", "signpost-commonshttp4", "1.2.1.2")

    testImplementation("com.winterwell","web","1.1.4")
    testImplementation("com.thoughtworks.xstream","xstream","1.4.19")
    testImplementation("junit","junit","4.13.2")
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["test"].java {
    srcDir("test")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
