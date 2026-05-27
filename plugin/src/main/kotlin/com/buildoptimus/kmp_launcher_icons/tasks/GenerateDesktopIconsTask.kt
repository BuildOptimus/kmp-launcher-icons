package com.buildoptimus.kmp_launcher_icons.tasks

import com.buildoptimus.kmp_launcher_icons.generators.DesktopIconGenerator
import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

@CacheableTask
abstract class GenerateDesktopIconsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceImage: RegularFileProperty

    @get:Input
    abstract val generateWindows: Property<Boolean>

    @get:OutputDirectory
    abstract val windowsOutputDirectory: DirectoryProperty

    @get:Input
    abstract val generateMacOS: Property<Boolean>

    @get:OutputDirectory
    abstract val macOSOutputDirectory: DirectoryProperty

    @get:Input
    abstract val generateLinux: Property<Boolean>

    @get:Input
    abstract val linuxSizes: ListProperty<Int>

    @get:OutputDirectory
    abstract val linuxOutputDirectory: DirectoryProperty

    init {
        group = "icons"
        description = "Generates desktop launcher icons (Windows ICO, MacOS ICNS, Linux PNGs)"
    }

    @TaskAction
    fun generate() {
        val source = sourceImage.get().asFile

        val sourceImageValidationResult = ImageUtils.validateSourceImage(source)

        sourceImageValidationResult.warnings.forEach { warning ->
            logger.warn("kmp-launcher-icons: $warning")
        }

        if (!sourceImageValidationResult.isValid) {
            throw GradleException(sourceImageValidationResult.errors.joinToString("\n"))
        }

        if (generateWindows.get()) {
            val directory = windowsOutputDirectory.get().asFile
            validateOutputDirectory(directory)
            logger.lifecycle("kmp-launcher-icons: Generating Windows ICO...")
            DesktopIconGenerator.generateWindowsIcons(image = source, directory = directory)
        }

        if (generateMacOS.get()) {
            val directory = macOSOutputDirectory.get().asFile
            validateOutputDirectory(directory)
            logger.lifecycle("kmp-launcher-icons: Generating MacOS ICNS...")
            DesktopIconGenerator.generateMacOSIcons(
                image = source,
                directory = directory
            ) { message ->
                logger.warn(message)
            }
        }

        if (generateLinux.get()) {
            val sizes = linuxSizes.get()

            if (sizes.isNotEmpty()) {
                val directory = linuxOutputDirectory.get().asFile
                validateOutputDirectory(directory)
                logger.lifecycle("kmp-launcher-icons: Generating Linux PNGs...")
                DesktopIconGenerator.generateLinuxIcons(image = source, directory = directory, sizes = sizes)
            }
        }

        logger.lifecycle("kmp-launcher-icons: Desktop icons generated successfully")
    }

    private fun validateOutputDirectory(directory: File) {
        directory.mkdirs()

        val validationResult = ImageUtils.validateOutputDirectory(directory)

        validationResult.warnings.forEach { warning ->
            logger.warn("kmp-launcher-icons: $warning")
        }

        if (!validationResult.isValid) {
            throw GradleException(validationResult.errors.joinToString("\n"))
        }
    }
}