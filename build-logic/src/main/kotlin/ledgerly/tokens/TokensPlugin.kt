package ledgerly.tokens

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Writes the Compose theme constants for `androidApp`. */
@CacheableTask
abstract class GenerateKotlinTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokensFile: RegularFileProperty

    @get:Input
    abstract val packageName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val tokens = DesignTokens.parse(tokensFile.get().asFile.readText())
        val pkg = packageName.get()
        val dir = outputDir.get().asFile.resolve(pkg.replace('.', '/'))
        dir.mkdirs()
        dir.resolve("LedgerlyTokens.kt").writeText(TokensCodegen.kotlin(tokens, pkg))
    }
}

/** Writes `LedgerlyTokens.swift` into the Xcode project, where it is committed. */
abstract class GenerateSwiftTokensTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val tokensFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val tokens = DesignTokens.parse(tokensFile.get().asFile.readText())
        outputFile.get().asFile.apply { parentFile.mkdirs() }.writeText(TokensCodegen.swift(tokens))
    }
}

/**
 * Puts the token tasks on a build script's classpath. Each project registers the task it needs:
 * `:designTokens` the Swift one, `:androidApp` the Kotlin one.
 */
class TokensPlugin : Plugin<Project> {
    override fun apply(project: Project) = Unit
}
