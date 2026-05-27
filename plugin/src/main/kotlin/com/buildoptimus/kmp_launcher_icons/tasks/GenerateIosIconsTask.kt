package com.buildoptimus.kmp_launcher_icons.tasks

import com.buildoptimus.kmp_launcher_icons.generators.IosIconGenerator
import com.buildoptimus.kmp_launcher_icons.models.XcodeVersion
import com.buildoptimus.kmp_launcher_icons.utils.ColorUtils
import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateIosIconsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceImage: RegularFileProperty

    @get:Input
    abstract val xcodeVersion: Property<XcodeVersion>

    @get:Input
    abstract val appStoreBackgroundColor: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "icons"
        description = "Generates iOS launcher icons and Contents.json"
    }

    @TaskAction
    fun generate() {
        val source = sourceImage.get().asFile
        val directory = outputDirectory.get().asFile

        val sourceImageValidationResult = ImageUtils.validateSourceImage(source)

        sourceImageValidationResult.warnings.forEach { warning ->
            logger.warn("kmp-launcher-icons: $warning")
        }

        if (!sourceImageValidationResult.isValid) {
            throw GradleException(sourceImageValidationResult.errors.joinToString("\n"))
        }

        directory.mkdirs()
        val directoryValidationResult = ImageUtils.validateOutputDirectory(directory)

        directoryValidationResult.warnings.forEach { warning ->
            logger.warn("kmp-launcher-icons: $warning")
        }

        if (!directoryValidationResult.isValid) {
            throw GradleException(directoryValidationResult.errors.joinToString("\n"))
        }

        val backgroundColorHex = appStoreBackgroundColor.get()
        val backgroundColorError = ColorUtils.validateHexColor(backgroundColorHex)

        if (backgroundColorError != null) {
            throw GradleException(backgroundColorError)
        }

        val backgroundColor = ColorUtils.parseHexColor(backgroundColorHex)
        val version = xcodeVersion.get()

        when (version) {
            XcodeVersion.MODERN -> {
                logger.lifecycle("kmp-launcher-icons: Generating iOS icons (modern/Xcode 15+)...")

                IosIconGenerator.generateModernIcons(
                    image = source,
                    background = backgroundColor,
                    directory = directory
                )
            }

            XcodeVersion.LEGACY -> {
                logger.lifecycle("kmp-launcher-icons: Generating iOS icons (legacy/pre-Xcode 15)...")

                IosIconGenerator.generateLegacyIcons(
                    image = source,
                    background = backgroundColor,
                    directory = directory
                )
            }
        }

        logger.lifecycle("kmp-launcher-icons: iOS icons generated in ${directory.absolutePath}")
    }
}