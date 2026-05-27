package com.buildoptimus.kmp_launcher_icons

import com.buildoptimus.kmp_launcher_icons.tasks.GenerateAndroidIconsTask
import com.buildoptimus.kmp_launcher_icons.tasks.GenerateDesktopIconsTask
import com.buildoptimus.kmp_launcher_icons.tasks.GenerateIosIconsTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

class KmpLauncherIconsPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "launcherIcons",
            LauncherIconsExtension::class,
            project.objects,
            project.layout
        )

        val androidTask = registerAndroidTask(project = project, extension = extension)
        val iosTask = registerIosTask(project = project, extension = extension)
        val desktopTask = registerDesktopTask(project = project, extension = extension)

        project.tasks.register("generateAllIcons") {
            group = "icons"
            description = "Generate launcher icons for all configured platforms"
            dependsOn(androidTask, iosTask, desktopTask)
        }
    }

    private fun registerAndroidTask(
        project: Project,
        extension: LauncherIconsExtension
    ): TaskProvider<GenerateAndroidIconsTask> {
        return project.tasks.register(
            name = "generateAndroidIcons",
            type = GenerateAndroidIconsTask::class
        ) {
            sourceImage.set(extension.androidConfig.sourceImage.orElse(extension.sourceImage))

            generateRaster.set(extension.androidConfig.generateRaster)
            generateRound.set(extension.androidConfig.generateRound)

            generateAdaptive.set(extension.androidConfig.adaptiveConfig.generate)
            adaptiveForegroundImage.set(extension.androidConfig.adaptiveConfig.foregroundImage)
            adaptiveBackgroundImage.set(extension.androidConfig.adaptiveConfig.backgroundImage)
            adaptiveBackgroundColor.set(extension.androidConfig.adaptiveConfig.backgroundColor)
            adaptiveApplyPadding.set(extension.androidConfig.adaptiveConfig.applyPadding)

            outputDirectory.set(extension.androidConfig.outputDirectory)

            // Gradle skips the task entirely (marks it SKIPPED) if no icon types are
            // enabled, avoiding a failing build for platforms the user hasn't configured.
            onlyIf("No Android icon types are enabled. Enable at least one of: raster, round, adaptive.") {
                extension.androidConfig.generateRaster.get() ||
                        extension.androidConfig.generateRound.get() ||
                        extension.androidConfig.adaptiveConfig.generate.get()
            }

            // Runs in doFirst rather than @TaskAction because sourceImage may be wired
            // to another task's output and is not resolvable at configuration time.
            doFirst {
                if (!sourceImage.isPresent) {
                    throw GradleException(
                        "kmp-launcher-icons: sourceImage is not set for Android. " +
                                "Set launcherIcons.sourceImage or launcherIcons.android.sourceImage."
                    )
                }
            }
        }
    }

    private fun registerIosTask(
        project: Project,
        extension: LauncherIconsExtension
    ): TaskProvider<GenerateIosIconsTask> {
        return project.tasks.register(
            name = "generateIosIcons",
            type = GenerateIosIconsTask::class
        ) {
            sourceImage.set(extension.iosConfig.sourceImage.orElse(extension.sourceImage))
            xcodeVersion.set(extension.iosConfig.xcodeVersion)
            appStoreBackgroundColor.set(extension.iosConfig.appStoreBackgroundColor)
            outputDirectory.set(extension.iosConfig.outputDirectory)

            // Skips the task if iOS generation is not explicitly enabled.
            onlyIf("iOS icon generation is not enabled. Set ios { generate = true } to enable it.") {
                extension.iosConfig.generate.get()
            }

            // Deferred to doFirst — sourceImage may not be resolvable at configuration time.
            doFirst {
                if (!sourceImage.isPresent) {
                    throw GradleException(
                        "kmp-launcher-icons: sourceImage is not set for iOS. " +
                                "Set launcherIcons.sourceImage or launcherIcons.ios.sourceImage."
                    )
                }
            }
        }
    }

    private fun registerDesktopTask(
        project: Project,
        extension: LauncherIconsExtension
    ): TaskProvider<GenerateDesktopIconsTask> {
        return project.tasks.register(
            name = "generateDesktopIcons",
            type = GenerateDesktopIconsTask::class
        ) {
            sourceImage.set(extension.desktopConfig.sourceImage.orElse(extension.sourceImage))

            generateWindows.set(extension.desktopConfig.windowsConfig.generate)
            windowsOutputDirectory.set(extension.desktopConfig.windowsConfig.outputDirectory)

            generateMacOS.set(extension.desktopConfig.macOSConfig.generate)
            macOSOutputDirectory.set(extension.desktopConfig.macOSConfig.outputDirectory)

            generateLinux.set(extension.desktopConfig.linuxConfig.generate)
            linuxSizes.set(extension.desktopConfig.linuxConfig.sizes)
            linuxOutputDirectory.set(extension.desktopConfig.linuxConfig.outputDirectory)

            // Skips the task if no desktop sub-platforms are enabled.
            onlyIf("No desktop platforms are enabled. Enable at least one of: windows, macOS, linux.") {
                extension.desktopConfig.windowsConfig.generate.get() ||
                        extension.desktopConfig.macOSConfig.generate.get() ||
                        extension.desktopConfig.linuxConfig.generate.get()
            }

            // Deferred to doFirst — sourceImage may not be resolvable at configuration time.
            doFirst {
                if (!sourceImage.isPresent) {
                    throw GradleException(
                        "kmp-launcher-icons: sourceImage is not set for Desktop. " +
                                "Set launcherIcons.sourceImage or launcherIcons.desktop.sourceImage."
                    )
                }
            }
        }
    }
}