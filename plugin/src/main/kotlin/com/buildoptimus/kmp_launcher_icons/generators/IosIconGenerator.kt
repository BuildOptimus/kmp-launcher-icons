package com.buildoptimus.kmp_launcher_icons.generators

import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import java.awt.Color
import java.io.File

object IosIconGenerator {
    private data class IconEntry(
        val idiom: String,
        val sizePoint: String,
        val scale: Int,
        val pixelSize: Int
    ) {
        val filename: String get() = "AppIcon-${pixelSize}x${pixelSize}.png"
    }

    // Sizes required by Xcode for pre-Xcode 15 projects (iPhone, iPad, App Store).
    // These are the exact sizes Apple's asset catalog validator expects; removing or
    // changing any entry will cause Xcode to flag the asset catalog as incomplete.
    private val LEGACY_ICON_ENTRIES = listOf(
        IconEntry(idiom = "iphone", sizePoint = "20x20", scale = 2, pixelSize = 40),
        IconEntry(idiom = "iphone", sizePoint = "20x20", scale = 3, pixelSize = 60),
        IconEntry(idiom = "iphone", sizePoint = "29x29", scale = 2, pixelSize = 58),
        IconEntry(idiom = "iphone", sizePoint = "29x29", scale = 3, pixelSize = 87),
        IconEntry(idiom = "iphone", sizePoint = "40x40", scale = 2, pixelSize = 80),
        IconEntry(idiom = "iphone", sizePoint = "40x40", scale = 3, pixelSize = 120),
        IconEntry(idiom = "iphone", sizePoint = "60x60", scale = 2, pixelSize = 120),
        IconEntry(idiom = "iphone", sizePoint = "60x60", scale = 3, pixelSize = 180),
        IconEntry(idiom = "ipad", sizePoint = "20x20", scale = 1, pixelSize = 20),
        IconEntry(idiom = "ipad", sizePoint = "20x20", scale = 2, pixelSize = 40),
        IconEntry(idiom = "ipad", sizePoint = "29x29", scale = 1, pixelSize = 29),
        IconEntry(idiom = "ipad", sizePoint = "29x29", scale = 2, pixelSize = 58),
        IconEntry(idiom = "ipad", sizePoint = "40x40", scale = 1, pixelSize = 40),
        IconEntry(idiom = "ipad", sizePoint = "40x40", scale = 2, pixelSize = 80),
        IconEntry(idiom = "ipad", sizePoint = "76x76", scale = 1, pixelSize = 76),
        IconEntry(idiom = "ipad", sizePoint = "76x76", scale = 2, pixelSize = 152),
        IconEntry(idiom = "ipad", sizePoint = "83.5x83.5", scale = 2, pixelSize = 167),
        IconEntry(idiom = "ios-marketing", sizePoint = "1024x1024", scale = 1, pixelSize = 1024),
    )

    // Xcode 15+ uses a single 1024×1024 universal icon instead of the full size
    // matrix. The Contents.json format changed to reference a single "universal" image.
    fun generateModernIcons(image: File, background: Color, directory: File) {
        directory.mkdirs()

        val file = File(directory, "AppIcon.png")

        val loadedImage = ImageUtils.load(image)
        val resizedImage = ImageUtils.resize(image = loadedImage, size = 1024)
        val flattenedImage = ImageUtils.flattenTransparency(image = resizedImage, color = background)
        // iOS icons must use the sRGB color space; Xcode will reject icons with
        // embedded Display P3 or other wide-gamut profiles in legacy asset catalogs.
        val srgbImage = ImageUtils.toSRGB(flattenedImage)

        ImageUtils.save(image = srgbImage, file = file)

        val contentsJson = buildModernContentsJson()

        val contentsJsonFile = File(directory, "Contents.json")

        contentsJsonFile.writeText(contentsJson)
    }

    private fun buildModernContentsJson(): String {
        return """{
  "images": [
    {
      "filename": "AppIcon.png",
      "idiom": "universal",
      "platform": "ios",
      "scale": "1x"
    }
  ],
  "info": {
    "author": "kmp-launcher-icons",
    "version": 1
  }
}"""
    }

    fun generateLegacyIcons(image: File, background: Color, directory: File) {
        directory.mkdirs()

        val loadedImage = ImageUtils.load(image)

        LEGACY_ICON_ENTRIES.forEach { entry ->
            val file = File(directory, entry.filename)

            val resizedImage = ImageUtils.resize(image = loadedImage, size = entry.pixelSize)

            // Apple requires the App Store icon (ios-marketing, 1024×1024) to be fully
            // opaque. Device icons may retain transparency and are not flattened.
            val srgbImage = if (entry.idiom == "ios-marketing") {
                val flattenedImage = ImageUtils.flattenTransparency(image = resizedImage, color = background)
                ImageUtils.toSRGB(flattenedImage)
            } else {
                ImageUtils.toSRGB(resizedImage)
            }

            ImageUtils.save(image = srgbImage, file = file)
        }

        val contentsJson = buildLegacyContentsJson()

        val contentsJsonFile = File(directory, "Contents.json")

        contentsJsonFile.writeText(contentsJson)
    }

    private fun buildLegacyContentsJson(): String {
        val images = LEGACY_ICON_ENTRIES.joinToString(",\n") { entry ->
            """    {
      "filename": "${entry.filename}",
      "idiom": "${entry.idiom}",
      "scale": "${entry.scale}x",
      "size": "${entry.sizePoint}"
    }"""
        }

        return """{
  "images": [
$images
  ],
  "info": {
    "author": "kmp-launcher-icons",
    "version": 1
  }
}"""
    }
}