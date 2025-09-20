package ua.wwind.convention

plugins {
    // Vanniktech Maven Publish plugin
    id("com.vanniktech.maven.publish")

    // Needed to generate Javadoc (Dokka HTML → javadocJar)
    id("org.jetbrains.dokka")
}

val pomName: String = providers.gradleProperty("POM_NAME").get()
val pomDescription: String = providers.gradleProperty("POM_DESCRIPTION").get()
val pomInceptionYear: String = providers.gradleProperty("POM_INCEPTION_YEAR").get()
val pomUrl: String = providers.gradleProperty("POM_URL").get()

val pomLicenseName: String = providers.gradleProperty("POM_LICENSE_NAME").get()
val pomLicenseUrl: String = providers.gradleProperty("POM_LICENSE_URL").get()
val pomLicenseDist: String = providers.gradleProperty("POM_LICENSE_DIST").get()

val pomDeveloperId: String = providers.gradleProperty("POM_DEVELOPER_ID").get()
val pomDeveloperName: String = providers.gradleProperty("POM_DEVELOPER_NAME").get()
val pomDeveloperUrl: String = providers.gradleProperty("POM_DEVELOPER_URL").get()

val pomScmUrl: String = providers.gradleProperty("POM_SCM_URL").get()
val pomScmConnection: String = providers.gradleProperty("POM_SCM_CONNECTION").get()
val pomScmDevConnection: String = providers.gradleProperty("POM_SCM_DEV_CONNECTION").get()

val artifactId: String = project.name

mavenPublishing {
    // Targets Maven Central via Sonatype; host can be customized if needed
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        project.group.toString(),
        artifactId,
        project.version.toString()
    )

    pom {
        name.set(pomName)
        description.set(pomDescription)
        inceptionYear.set(pomInceptionYear)
        url.set(pomUrl)

        licenses {
            license {
                name.set(pomLicenseName)
                url.set(pomLicenseUrl)
                distribution.set(pomLicenseDist)
            }
        }

        developers {
            developer {
                id.set(pomDeveloperId)
                name.set(pomDeveloperName)
                url.set(pomDeveloperUrl)
            }
        }

        scm {
            url.set(pomScmUrl)
            connection.set(pomScmConnection)
            developerConnection.set(pomScmDevConnection)
        }
    }
}

// Javadoc jar based on Dokka HTML output (works for KMP and Android/Java)
tasks.register<org.gradle.jvm.tasks.Jar>("javadocJar") {
    dependsOn("dokkaGeneratePublicationHtml")
    from(layout.buildDirectory.dir("dokka/html"))
    archiveClassifier.set("javadoc")
}

// Build should fail if tests fail, like in your example
tasks.named("build").configure {
    dependsOn("check")
}
