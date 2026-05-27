package com.buildoptimus.kmp_launcher_icons

import com.buildoptimus.kmp_launcher_icons.models.XcodeVersion
import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class LauncherIconsExtension @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {
    abstract val sourceImage: RegularFileProperty
    val androidConfig: AndroidConfig = objects.newInstance(layout)
    val iosConfig: IosConfig = objects.newInstance(layout)
    val desktopConfig: DesktopConfig = objects.newInstance(layout)

    fun android(action: Action<AndroidConfig>) {
        action.execute(androidConfig)
    }

    fun ios(action: Action<IosConfig>) {
        action.execute(iosConfig)
    }

    fun desktop(action: Action<DesktopConfig>) {
        action.execute(desktopConfig)
    }
}

abstract class AndroidConfig @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {
    abstract val sourceImage: RegularFileProperty
    abstract val generateRaster: Property<Boolean>
    abstract val generateRound: Property<Boolean>
    val adaptiveConfig: AdaptiveConfig = objects.newInstance(layout)
    abstract val outputDirectory: DirectoryProperty

    fun adaptive(action: Action<AdaptiveConfig>) {
        action.execute(adaptiveConfig)
    }

    init {
        generateRaster.convention(false)
        generateRound.convention(false)
        outputDirectory.convention(layout.projectDirectory.dir("androidApp/src/main/res"))
    }
}

abstract class AdaptiveConfig @Inject constructor(layout: ProjectLayout) {
    abstract val generate: Property<Boolean>
    abstract val foregroundImage: RegularFileProperty
    abstract val backgroundImage: RegularFileProperty
    abstract val backgroundColor: Property<String>
    abstract val applyPadding: Property<Boolean>

    init {
        generate.convention(false)
        backgroundColor.convention("#FFFFFF")
        applyPadding.convention(true)
    }
}

abstract class IosConfig @Inject constructor(layout: ProjectLayout) {
    abstract val generate: Property<Boolean>
    abstract val sourceImage: RegularFileProperty
    abstract val xcodeVersion: Property<XcodeVersion>
    abstract val appStoreBackgroundColor: Property<String>
    abstract val outputDirectory: DirectoryProperty

    init {
        generate.convention(false)
        xcodeVersion.convention(XcodeVersion.MODERN)
        appStoreBackgroundColor.convention("#FFFFFF")
        outputDirectory.convention(layout.projectDirectory.dir("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"))
    }
}

abstract class DesktopConfig @Inject constructor(objects: ObjectFactory, layout: ProjectLayout) {
    abstract val sourceImage: RegularFileProperty

    val windowsConfig: WindowsConfig = objects.newInstance(layout)
    val macOSConfig: MacOSConfig = objects.newInstance(layout)
    val linuxConfig: LinuxConfig = objects.newInstance(layout)

    fun windows(action: Action<WindowsConfig>) {
        action.execute(windowsConfig)
    }

    fun macOS(action: Action<MacOSConfig>) {
        action.execute(macOSConfig)
    }

    fun linux(action: Action<LinuxConfig>) {
        action.execute(linuxConfig)
    }
}

abstract class WindowsConfig @Inject constructor(layout: ProjectLayout) {
    abstract val generate: Property<Boolean>
    abstract val outputDirectory: DirectoryProperty

    init {
        generate.convention(false)
        outputDirectory.convention(layout.projectDirectory.dir("desktopApp/src/main/resources/windows"))
    }
}

abstract class MacOSConfig @Inject constructor(layout: ProjectLayout) {
    abstract val generate: Property<Boolean>
    abstract val outputDirectory: DirectoryProperty

    init {
        generate.convention(false)
        outputDirectory.convention(layout.projectDirectory.dir("desktopApp/src/main/resources/macos"))
    }
}

abstract class LinuxConfig @Inject constructor(layout: ProjectLayout) {
    abstract val generate: Property<Boolean>
    abstract val sizes: ListProperty<Int>
    abstract val outputDirectory: DirectoryProperty

    init {
        generate.convention(false)
        sizes.convention(listOf(16, 32, 48, 64, 128, 256))
        outputDirectory.convention(layout.projectDirectory.dir("desktopApp/src/main/resources/linux"))
    }
}