import ledgerly.tokens.GenerateSwiftTokensTask

plugins {
    id("ledgerly.tokens")
}

// tokens.json is the single source for both apps. Android regenerates its Kotlin on every build;
// the Swift file is committed so Xcode never needs Gradle for it. CI fails if it is stale.
tasks.register<GenerateSwiftTokensTask>("generateSwiftTokens") {
    group = "ledgerly"
    description = "Regenerates iosApp/Ledgerly/DesignSystem/Generated/LedgerlyTokens.swift from tokens.json."
    tokensFile.set(layout.projectDirectory.file("tokens.json"))
    outputFile.set(rootProject.layout.projectDirectory.file("iosApp/Ledgerly/DesignSystem/Generated/LedgerlyTokens.swift"))
}
