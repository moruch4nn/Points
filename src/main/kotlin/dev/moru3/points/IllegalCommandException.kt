package dev.moru3.points

class IllegalCommandException(val translationKey: String, vararg val values: Any): Exception() {
}