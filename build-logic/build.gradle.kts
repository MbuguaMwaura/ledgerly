plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(localGroovy())
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        register("ledgerlyTokens") {
            id = "ledgerly.tokens"
            implementationClass = "ledgerly.tokens.TokensPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
