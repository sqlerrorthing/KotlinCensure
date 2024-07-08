package me.oneqxz.censor.lang

interface CensorLanguage : LanguageFeatures {

    /**
     * Replaces letters in a pattern
     *
     * @return list of pairs, where [Regex] is where to replace, [String] is what to replace with.
     */
    fun getPatternReplacements(): Array<Pair<Regex, String>>

    fun getPatternPrep(): Regex

    fun getExcludesData(): Map<String, Array<Regex>>

    fun getExcludesCore(): Map<String, Regex>

    fun getFoulData(): Map<String, Array<Regex>>

    fun getFoulCore(): Map<String, Regex>

    fun getBadSemiPhrases(): Array<Regex>

    fun getBadPhrases(): Array<Regex>

    fun getTranslationsTable(): Map<Char, Char>


}