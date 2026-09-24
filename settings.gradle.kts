plugins {
    id("com.gradleup.nmcp.settings") version "1.6.2"
}

rootProject.name = "clarity"

if (file("../clarity-protobuf").exists())
    includeBuild("../clarity-protobuf")

nmcpSettings {
    centralPortal {
        username = providers.gradleProperty("mavenCentralUsername").get()
        password = providers.gradleProperty("mavenCentralPassword").get()
    }
}
