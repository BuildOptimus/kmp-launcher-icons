package com.buildoptimus.kmp_launcher_icons.tasks

import com.buildoptimus.kmp_launcher_icons.generators.AndroidIconGenerator
import com.buildoptimus.kmp_launcher_icons.utils.ColorUtils
import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
abstract class GenerateAndroidIconsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceImage: RegularFileProperty

    @get:Input
    abstract val generateRaster: Property<Boolean>

    @get:Input
    abstract val generateRound: Property<Boolean>

    @get:Input
    abstract val generateAdaptive: Property<Boolean>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val adaptiveForegroundImage: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val adaptiveBackgroundImage: RegularFileProperty

    @get:Input
    abstract val adaptiveBackgroundColor: Property<String>

    @get:Input
    abstract val adaptiveApplyPadding: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        group = "icons"
        description = "Generates Android launcher icons (raster, round, and adaptive)"
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

        if (generateAdaptive.get()) {
            val backgroundColor = adaptiveBackgroundColor.get()
            val backgroundColorError = ColorUtils.validateHexColor(backgroundColor)
            if (backgroundColorError != null) {
                throw GradleException(backgroundColorError)
            }
        }

        if (generateRaster.get()) {
            logger.lifecycle("kmp-launcher-icons: Generating Android raster icons...")
            AndroidIconGenerator.generateRasterIcons(image = source, directory = directory)
        }

        if (generateRound.get()) {
            logger.lifecycle("kmp-launcher-icons: Generating Android round icons...")
            AndroidIconGenerator.generateRoundIcons(image = source, directory = directory)
        }

        if (generateAdaptive.get()) {
            logger.lifecycle("kmp-launcher-icons: Generating Android adaptive icons...")

            AndroidIconGenerator.generateAdaptiveIcons(
                image = source,
                directory = directory,
                foregroundImage = adaptiveForegroundImage.orNull?.asFile,
                backgroundImage = adaptiveBackgroundImage.orNull?.asFile,
                backgroundColor = adaptiveBackgroundColor.get(),
                applyPadding = adaptiveApplyPadding.get()
            )
        }

        logger.lifecycle("kmp-launcher-icons: Android icons generated in ${directory.absolutePath}")
    }
}