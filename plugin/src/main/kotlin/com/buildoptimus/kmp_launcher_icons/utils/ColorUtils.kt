package com.buildoptimus.kmp_launcher_icons.utils

import java.awt.Color

object ColorUtils {
    // Accepted hex color formats: 3-digit shorthand (#RGB), standard 6-digit
    // (#RRGGBB), and 8-digit with alpha (#AARRGGBB) in Android's byte order (alpha first).
    private val HEX3_PATTERN = Regex("^#[0-9A-Fa-f]{3}$")
    private val HEX6_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
    private val HEX8_PATTERN = Regex("^#[0-9A-Fa-f]{8}$")

    fun isValidHexColor(value: String): Boolean {
        return HEX3_PATTERN.matches(value) ||
                HEX6_PATTERN.matches(value) ||
                HEX8_PATTERN.matches(value)
    }

    fun parseHexColor(value: String): Color {
        require(isValidHexColor(value)) {
            hexColorErrorMessage(value)
        }

        return when (value.length) {
            4 -> {
                val red = value[1].digitToInt(16)
                val green = value[2].digitToInt(16)
                val blue = value[3].digitToInt(16)
                // Expanding a single hex nibble N to a full byte: N * 16 + N = N * 17.
                // This correctly maps #F → 0xFF (255), #0 → 0x00 (0), etc.
                Color(red * 17, green * 17, blue * 17)
            }

            7 -> {
                val red = value.substring(1, 3).toInt(16)
                val green = value.substring(3, 5).toInt(16)
                val blue = value.substring(5, 7).toInt(16)
                Color(red, green, blue)
            }

            else -> {
                val alpha = value.substring(1, 3).toInt(16)
                val red = value.substring(3, 5).toInt(16)
                val green = value.substring(5, 7).toInt(16)
                val blue = value.substring(7, 9).toInt(16)
                Color(red, green, blue, alpha)
            }
        }
    }

    fun validateHexColor(value: String): String? {
        return  if(isValidHexColor(value)) null else hexColorErrorMessage(value)
    }

    private fun hexColorErrorMessage(value: String): String {
        return "'$value' is not a valid color. Use hex format: #RGB, #RRGGBB or #AARRGGBB."
    }
}