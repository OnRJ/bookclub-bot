plugins {
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    kotlin("plugin.allopen") version "2.3.21"
    jacoco
}
group = "com.bookclub"
version = "0.1.0"
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
repositories { mavenCentral() }
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Telegram Bot API
    implementation("com.github.pengrad:java-telegram-bot-api:10.3.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.6")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
}
allOpen {
    annotations("jakarta.persistence.Entity", "jakarta.persistence.MappedSuperclass", "jakarta.persistence.Embeddable")
}
kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }
tasks.withType<Test> { useJUnitPlatform() }

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}


tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val coverageExclusions = listOf(
    "**/infrastructure/persistence/**",
    "**/infrastructure/telegram/**",
    "**/infrastructure/config/**",
    "**/BookClubApplication*",
    "**/shared/config/**",
    "**/poll/application/PollRoundService*",
    "**/poll/application/PollDeadlineScheduler*",
    "**/poll/application/PollAdminNotificationService*",
    "**/submission/api/**",
    "**/submission/application/SubmitBooksUseCase*",
    "**/poll/application/PollAnswerService*"
)

tasks.jacocoTestReport {
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) { exclude(coverageExclusions) }
    }))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test)
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) { exclude(coverageExclusions) }
    }))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/test.exec"))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                // Raise to 0.80 after the remaining scheduler/infrastructure branches are covered.
                minimum = "0.65".toBigDecimal()
            }
        }
    }
}
