package com.buildoptimus.kmp_launcher_icons.generators

import com.buildoptimus.kmp_launcher_icons.utils.ImageUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO


object DesktopIconGenerator {
    // Standard sizes Windows expects in a multi-resolution .ico file. Windows selects
    // the closest available size for each display context (taskbar, Explorer, Alt+Tab, etc).
    private val ICO_SIZES = listOf(16, 32, 48, 64, 128, 256)

    // Base sizes for a macOS .iconset directory. Each size also generates a @2x Retina
    // variant. iconutil requires exactly this set to produce a valid .icns.
    private val ICNS_SIZES = listOf(16, 32, 128, 256, 512)

    fun generateWindowsIcons(image: File, directory: File) {
        directory.mkdirs()

        val loadedImage = ImageUtils.load(image)

        val resizedImages = ICO_SIZES.map { size ->
            ImageUtils.resize(image = loadedImage, size = size).awt()
        }

        val icoFile = File(directory, "icon.ico")

        writeMultiResolutionIco(images = resizedImages, file = icoFile)
    }

    // Writes a multi-resolution ICO file following the ICO format specification.
    // Reference: https://en.wikipedia.org/wiki/ICO_(file_format)
    private fun writeMultiResolutionIco(images: List<BufferedImage>, file: File) {
        val pngDataList = images.map { image ->
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", outputStream)
            outputStream.toByteArray()
        }

        val count = images.size
        val headerSize = 6
        val directorySize = 16 * count
        var dataOffset = headerSize + directorySize

        file.outputStream().buffered().use { outputStream ->
            // ICO header: reserved (must be 0), type (1 = ICO), image count.
            outputStream.write(littleEndian16(0))
            outputStream.write(littleEndian16(1))
            outputStream.write(littleEndian16(count))

            // ICONDIRENTRY loop — one 16-byte entry per image.
            images.forEachIndexed { index, _ ->
                val image = images[index]
                val pngData = pngDataList[index]

                // Width/height are stored as a single byte each. A value of 0 means
                // 256px because the field cannot represent 256 directly.
                val width = if (image.width >= 256) 0 else image.width
                val height = if (image.height >= 256) 0 else image.height

                outputStream.write(width)              // image width
                outputStream.write(height)             // image height
                outputStream.write(0)                  // colour count (0 = no palette)
                outputStream.write(0)                  // reserved
                outputStream.write(littleEndian16(1))  // colour planes
                outputStream.write(littleEndian16(32)) // bits per pixel
                outputStream.write(littleEndian32(pngData.size))  // image data size
                outputStream.write(littleEndian32(dataOffset))    // offset to image data

                dataOffset += pngData.size
            }

            // Pixel data section — raw PNG bytes for each image, in the same order
            // as the directory entries above.
            for (pngData in pngDataList) {
                outputStream.write(pngData)
            }
        }
    }

    private fun littleEndian16(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte()
        )
    }

    private fun littleEndian32(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    // Produces an .iconset directory rather than a compiled .icns binary because
    // iconutil (required to compile) is only available on macOS. The logger callback
    // surfaces the manual follow-up step to the user.
    fun generateMacOSIcons(image: File, directory: File, logger: ((String) -> Unit)? = null) {
        directory.mkdirs()

        val loadedImage = ImageUtils.load(image)

        val iconSetDirectory = File(directory, "icon.iconset")
        iconSetDirectory.mkdirs()

        ICNS_SIZES.forEach { size ->
            val file = File(iconSetDirectory, "icon_${size}x${size}.png")
            val file2x = File(iconSetDirectory, "icon_${size}x${size}@2x.png")

            val resizedImage = ImageUtils.resize(image = loadedImage, size = size)
            val resizedImage2x = ImageUtils.resize(image = loadedImage, size = size * 2)

            ImageUtils.save(image = resizedImage, file = file)
            ImageUtils.save(image = resizedImage2x, file = file2x)
        }

        logger?.invoke(
            "kmp-launcher-icons: Generated icon.iconset/ with PNG assets at all required sizes.\n" +
                    "To produce a standalone icon.icns, run on macOS:\n" +
                    "  iconutil -c icns icon.iconset"
        )
    }

    fun generateLinuxIcons(image: File, directory: File, sizes: List<Int>) {
        directory.mkdirs()

        val loadedImage = ImageUtils.load(image)

        sizes.forEach { size ->
            val file = File(directory, "icon_${size}x${size}.png")
            val resizedImage = ImageUtils.resize(image = loadedImage, size = size)
            ImageUtils.save(image = resizedImage, file = file)
        }
    }
}