package com.buildoptimus.kmp_launcher_icons.utils

import com.buildoptimus.kmp_launcher_icons.models.ValidationResult
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.ScaleMethod
import com.sksamuel.scrimage.nio.PngWriter
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File

object ImageUtils {
    fun load(file: File): ImmutableImage {
        return ImmutableImage.loader().fromFile(file)
    }

    fun resize(image: ImmutableImage, size: Int): ImmutableImage {
        // Lanczos3 preserves sharpness better than bilinear interpolation when
        // downscaling to the small sizes used for launcher icons (e.g. 20×20, 29×29).
        return image.scaleTo(size, size, ScaleMethod.Lanczos3)
    }

    fun cropCircular(image: ImmutableImage): ImmutableImage {
        val source = image.awt()

        val width = source.width
        val height = source.height

        val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        val graphics = canvas.createGraphics()

        graphics.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )

        graphics.clip = Ellipse2D.Float(0f, 0f, width.toFloat(), height.toFloat())
        graphics.drawImage(source, 0, 0, null)

        graphics.dispose()

        return ImmutableImage.fromAwt(canvas)
    }

    fun flattenTransparency(image: ImmutableImage, color: Color): ImmutableImage {
        val source = image.awt()

        // TYPE_INT_RGB (no alpha channel) ensures the output has no transparency,
        // which is required for App Store icons and certain background drawables.
        val canvas = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_RGB)

        val graphics = canvas.createGraphics()

        graphics.color = color
        graphics.fillRect(0, 0, source.width, source.height)

        graphics.drawImage(source, 0, 0, null)

        graphics.dispose()

        return ImmutableImage.fromAwt(canvas)
    }

    fun applyForegroundPadding(
        image: ImmutableImage,
        canvasSize: Int,
        contentPercent: Float,
    ): ImmutableImage {
        val contentSize = canvasSize.times(contentPercent).toInt()

        // Integer division centres the content on the canvas; the result is symmetric.
        val offset = canvasSize.minus(contentSize).div(2)

        val resizedImage = resize(image = image, size = contentSize)

        val canvas = BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB)

        val graphics = canvas.createGraphics()

        // AlphaComposite.Src clears the canvas to fully transparent before compositing,
        // preventing artifacts from the default BufferedImage background.
        graphics.composite = AlphaComposite.Src
        graphics.color = Color(0, 0, 0, 0)
        graphics.fillRect(0, 0, canvasSize, canvasSize)

        graphics.composite = AlphaComposite.SrcOver
        graphics.drawImage(resizedImage.awt(), offset, offset, null)

        graphics.dispose()

        return ImmutableImage.fromAwt(canvas)
    }

    fun save(image: ImmutableImage, file: File) {
        file.parentFile?.mkdirs()
        image.output(PngWriter.MaxCompression, file)
    }

    fun toSRGB(image: ImmutableImage): ImmutableImage {
        val source = image.awt()

        if (source.colorModel.colorSpace.isCS_sRGB) {
            return image
        }

        val canvas = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)

        val graphics = canvas.createGraphics()

        graphics.drawImage(source, 0, 0, null)

        graphics.dispose()

        return ImmutableImage.fromAwt(canvas)
    }

    fun validateSourceImage(file: File): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!file.exists()) {
            errors.add("Source image '${file.absolutePath}' does not exist. Set launcherIcons.sourceImage to a valid PNG file.")
            return ValidationResult(errors = errors, warnings = warnings)
        }

        if (!file.extension.equals("png", ignoreCase = true)) {
            errors.add("Source image '${file.absolutePath}' is not a PNG file. Only PNG format is supported.")
            return ValidationResult(errors = errors, warnings = warnings)
        }

        // The 8-byte PNG signature defined in the PNG spec (ISO/IEC 15948). Checking
        // it guards against files renamed to .png without being re-encoded.
        val pngMagicBytes = byteArrayOf(
            0x89.toByte(),
            0x50,
            0x4E,
            0x47,
            0x0D,
            0x0A,
            0x1A.toByte(),
            0x0A
        )

        val fileHeader = file.inputStream().use { it.readNBytes(8) }

        if (!fileHeader.contentEquals(pngMagicBytes)) {
            errors.add(
                "Source image '${file.absolutePath}' has a .png extension but is not a valid PNG file. " +
                "Ensure the file is a genuine PNG image."
            )

            return ValidationResult(errors = errors, warnings = warnings)
        }

        val image = load(file)

        val width = image.width
        val height = image.height

        if (width != height) {
            errors.add("Source image '${file.absolutePath}' is ${width}x${height}. Launcher icons must be square.")
        }

        if (width < 1024 || height < 1024) {
            warnings.add("Source image '${file.absolutePath}' is ${width}x${height}. A minimum of 1024x1024 is recommended to avoid quality loss at larger densities.")
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }

    fun validateOutputDirectory(directory: File): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (!directory.exists()) {
            errors.add("Output directory '${directory.absolutePath}' does not exist.")
            return ValidationResult(errors = errors, warnings = warnings)
        }

        if (!directory.canWrite()) {
            errors.add("Output directory '${directory.absolutePath}' is not writable.")
        }

        return ValidationResult(errors = errors, warnings = warnings)
    }
}