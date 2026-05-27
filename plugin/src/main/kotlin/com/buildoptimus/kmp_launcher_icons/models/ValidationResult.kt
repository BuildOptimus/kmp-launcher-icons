package com.buildoptimus.kmp_launcher_icons.models

data class ValidationResult(
    val errors: List<String>,
    val warnings: List<String>
) {
    val isValid: Boolean get() = errors.isEmpty()
}