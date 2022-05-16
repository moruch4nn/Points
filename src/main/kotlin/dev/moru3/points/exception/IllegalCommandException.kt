package dev.moru3.points.exception

class IllegalCommandException(val translationKey: String, vararg val values: Any): Exception() {
}