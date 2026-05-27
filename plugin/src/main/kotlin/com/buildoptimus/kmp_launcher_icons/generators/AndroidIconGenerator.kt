package com.buildoptimus.kmp_launcher_icons.generators

import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import java.io.File

object AndroidIconGenerator {
    private data class Density(val name: String, val size: Int)

    // Standard Android density buckets. Sizes follow the mdpi baseline of 48px
    // scaled by each density factor (1×, 1.5×, 2×, 3×, 4×).
    private val DENSITIES = listOf(
        Density(name = "mdpi", size = 48),
        Density(name = "hdpi", size = 72),
        Density(name = "xhdpi", size = 96),
        Density(name = "xxhdpi", size = 144),
        Density(name = "xxxhdpi", size = 192),
    )

    // Exact pixel size required by the Google Play Store for the app listing icon.
    private const val PLAY_STORE_SIZE = 512

    // xxxhdpi canvas size for adaptive icons: 108dp × 4 = 432px.
    // See: https://developer.android.com/develop/ui/views/launch/icon_design_adaptive
    private const val ADAPTIVE_CANVAS_SIZE = 432

    // Android's adaptive icon safe zone is a circle occupying 66% of the canvas.
    // Artwork outside this zone may be clipped by the launcher's icon mask.
    private const val SAFE_ZONE_PERCENT = 0.66f

    fun generateRasterIcons(image: File, directory: File) {
        val loadedImage = ImageUtils.load(image)

        for (density in DENSITIES) {
            val mipmapDirectory = File(directory, "mipmap-${density.name}")
            val file = File(mipmapDirectory, "ic_launcher.png")
            val resizedImage = ImageUtils.resize(image = loadedImage, size = density.size)
            ImageUtils.save(image = resizedImage, file = file)
        }

        val playStoreFile = File(directory, "ic_launcher-playstore.png")
        val playStoreImage = ImageUtils.resize(image = loadedImage, size = PLAY_STORE_SIZE)
        ImageUtils.save(image = playStoreImage, file = playStoreFile)
    }

    fun generateRoundIcons(image: File, directory: File) {
        val loadedImage = ImageUtils.load(image)
        val circularImage = ImageUtils.cropCircular(loadedImage)

        for (density in DENSITIES) {
            val mipmapDirectory = File(directory, "mipmap-${density.name}")
            val file = File(mipmapDirectory, "ic_launcher_round.png")
            val resizedImage = ImageUtils.resize(image = circularImage, size = density.size)
            ImageUtils.save(image = resizedImage, file = file)
        }
    }

    fun generateAdaptiveIcons(
        image: File,
        directory: File,
        foregroundImage: File?,
        backgroundImage: File?,
        backgroundColor: String,
        applyPadding: Boolean
    ) {
        val xxxhDpiFile = File(directory, "mipmap-xxxhdpi")
        xxxhDpiFile.mkdirs()

        val actualForegroundImage = ImageUtils.load(foregroundImage ?: image)

        val modifiedForegroundImage = if (applyPadding) {
            ImageUtils.applyForegroundPadding(
                image = actualForegroundImage,
                canvasSize = ADAPTIVE_CANVAS_SIZE,
                contentPercent = SAFE_ZONE_PERCENT
            )
        } else {
            ImageUtils.resize(image = actualForegroundImage, size = ADAPTIVE_CANVAS_SIZE)
        }

        val foregroundImageFile = File(xxxhDpiFile, "ic_launcher_foreground.png")
        ImageUtils.save(image = modifiedForegroundImage, file = foregroundImageFile)

        // Adaptive icons support either a solid color resource or a drawable bitmap
        // as their background layer. The XML structure differs: a color background
        // references a @color resource, while an image background references a @mipmap.
        val useColorBackground = backgroundImage == null

        if (useColorBackground) {
            generateBackgroundColorXml(directory = directory, color = backgroundColor)
        } else {
            val loadedBackgroundImage = ImageUtils.load(backgroundImage)
            val resizedBackgroundImage = ImageUtils.resize(image = loadedBackgroundImage, size = ADAPTIVE_CANVAS_SIZE)
            val backgroundImageFile = File(xxxhDpiFile, "ic_launcher_background.png")
            ImageUtils.save(image = resizedBackgroundImage, file = backgroundImageFile)
        }

        generateAdaptiveIconXml(directory = directory, useColorBackground = useColorBackground)
    }

    private fun generateBackgroundColorXml(directory: File, color: String) {
        val xml = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<resources>
            |    <color name="ic_launcher_background">$color</color>
            |</resources>
        """.trimMargin()

        val valuesDirectory = File(directory, "values")
        valuesDirectory.mkdirs()

        val backgroundFile = File(valuesDirectory, "ic_launcher_background.xml")
        backgroundFile.writeText(xml)
    }

    private fun generateAdaptiveIconXml(directory: File, useColorBackground: Boolean) {
        val backgroundDrawable = if (useColorBackground) {
            "@color/ic_launcher_background"
        } else {
            "@mipmap/ic_launcher_background"
        }

        val xml = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
            |    <background android:drawable="$backgroundDrawable"/>
            |    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
            |</adaptive-icon>
        """.trimMargin()

        val anyDpiDirectory = File(directory, "mipmap-anydpi-v26")
        anyDpiDirectory.mkdirs()

        val iconFile = File(anyDpiDirectory, "ic_launcher.xml")
        val roundIconFile = File(anyDpiDirectory, "ic_launcher_round.xml")

        iconFile.writeText(xml)
        roundIconFile.writeText(xml)
    }
}