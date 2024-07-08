package me.oneqxz.censor.lang

object CommonLanguagePatterns {

    val PUNCTUATION_1: Regex = Regex("[\"\\-+;.,*?()]+")
    val PUNCTUATION_2: Regex = Regex("[!:_]+")
    val PUNCTUATION_3: Regex = Regex("[\"\\-+;.,*?()!:_]+")

    val SPACE: Regex = Regex("\\s+")
}