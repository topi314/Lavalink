import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    signing
    `maven-publish`
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val archivesBaseName = "protocol"
group = "dev.arbjerg.lavalink"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    js(IR) {
        nodejs()
        browser()
        compilations.all {
            packageJson {
                //language=RegExp
                // npm doesn't support our versioning :(
                val validVersion = """\d+\.\d+\.\d+""".toRegex()
                if (!validVersion.matches(project.version.toString())) {
                    version = "4.0.0"
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }

        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
            }
        }

        getByName("jsTest") {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        getByName("jvmTest") {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }

    targets {
        all {
            mavenPublication {
                pom {
                    name.set("Lavalink Protocol")
                    description.set("Protocol for Lavalink Client development")
                    url.set("https://github.com/lavalink-devs/lavalink")

                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://github.com/lavalink-devs/Lavalink/blob/master/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("freyacodes")
                            name.set("Freya Arbjerg")
                            url.set("https://www.arbjerg.dev")
                        }
                    }

                    scm {
                        connection.set("scm:git:ssh://github.com/lavalink-devs/lavalink.git")
                        developerConnection.set("scm:git:ssh://github.com/lavalink-devs/lavalink.git")
                        url.set("https://github.com/lavalink-devs/lavalink")
                    }
                }
            }
        }
    }
}

tasks {
    withType<KotlinJvmTest> {
        useJUnitPlatform()
    }
}

// Use system Node.Js on NixOS
if (System.getenv("NIX_PROFILES") != null) {
    rootProject.plugins.withType<NodeJsRootPlugin> {
        rootProject.the<NodeJsRootExtension>().download = false
    }
}

publishing {
    if (findProperty("signing.gnupg.keyName") != null) {
        repositories {
            val snapshots = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            val releases = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"

            maven(if ((version as String).endsWith("SNAPSHOT")) snapshots else releases) {
                credentials {
                    password = findProperty("ossrhPassword") as? String
                    username = findProperty("ossrhUsername") as? String
                }
            }
        }

        signing {
            publications.withType<MavenPublication> {
                sign(this)
            }
            useGpgCmd()
        }
    } else {
        println("Not capable of publishing to OSSRH because of missing GPG key")
    }

    if (findProperty("MAVEN_USERNAME") != null && findProperty("MAVEN_PASSWORD") != null) {
        println("Publishing to Maven Repo")
        repositories {
            val snapshots = "https://maven.arbjerg.dev/snapshots"
            val releases = "https://maven.arbjerg.dev/releases"

            maven(if ((version as String).endsWith("SNAPSHOT")) snapshots else releases) {
                credentials {
                    password = findProperty("MAVEN_PASSWORD") as? String
                    username = findProperty("MAVEN_USERNAME") as? String
                }
            }
        }
    } else {
        println("Maven credentials not found, not publishing to Maven Repo")
    }
}
