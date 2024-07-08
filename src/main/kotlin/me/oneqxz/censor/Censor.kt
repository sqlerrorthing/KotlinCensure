package me.oneqxz.censor

import me.oneqxz.censor.lang.CensorLanguage
import me.oneqxz.censor.lang.CommonLanguagePatterns
import me.oneqxz.censor.lang.impl.RussianCensorLanguageImpl

class Censor (
    private val censorLanguage: CensorLanguage
) {

    private val patternReplacements: Array<Pair<Regex, String>> = censorLanguage.getPatternReplacements()

    private val excludesData: Map<String, Array<Regex>> = censorLanguage.getExcludesData()
    private val excludesCore: Map<String, Regex> = censorLanguage.getExcludesCore()
    private val foulData: Map<String, Array<Regex>> = censorLanguage.getFoulData()
    private val foulCore: Map<String, Regex> = censorLanguage.getFoulCore()

    private val badSemiPhrases: Array<Regex> = censorLanguage.getBadSemiPhrases()
    private val badPhrases: Array<Regex> = censorLanguage.getBadPhrases()
    private val translationsTable: Map<Char, Char> = censorLanguage.getTranslationsTable()

    fun cleanLine(line: String, beep: String = "[beep]"): CensorResponse
    {
        val detectedBadWords = mutableListOf<String>()
        val detectedBadPhrases = mutableListOf<String>()
        val detectedPats = mutableListOf<String>()

        val words = line.split(CommonLanguagePatterns.SPACE)
        var cleanedLine = line

        for(word in words)
        {
            val wordInfo = checkWord(word)
            if (!wordInfo.good)
            {
                cleanedLine = cleanedLine.replaceFirst(word.toRegex(), beep)
                detectedBadWords.add(word)
                detectedPats.add(wordInfo.accuse[0])
            }
        }

        val lineInfo = checkLineBadPhrases(line)
        if (!lineInfo.good) {
            for (pat in lineInfo.accuse)
            {
                val line2 = cleanedLine.replace(Regex(pat), beep)
                if (line2 != cleanedLine) {
                    detectedBadPhrases.add(pat)
                    detectedPats.add(pat)
                }
                cleanedLine = line2
            }
        }

        return CensorResponse(
            cleanedLine,
            detectedBadWords,
            detectedBadPhrases,
            detectedPats
        )
    }

    private fun checkLine(line: String): CensorLineResponse {
        val censorLineResponse = CensorLineResponse()
        val words: List<String> = censorLanguage.splitLine(line)

        if (words.isNotEmpty()) {
            for (word: String in words) {
                val wordInfo = checkWord(word)

                if (!wordInfo.good) {
                    censorLineResponse.good = false
                    censorLineResponse.badWordInfo = wordInfo
                    break
                }
            }
        }

        if (censorLineResponse.good) {
            val phrasesInfo: WordInfo = checkLineBadPhrases(line)
            if (!phrasesInfo.good) {
                censorLineResponse.good = false
                censorLineResponse.badWordInfo = phrasesInfo
            }
        }

        return censorLineResponse
    }

    private fun checkLineBadPhrases(line: String): WordInfo {
        val wordInfo = WordInfo(line)
        checkRegexpsArray(badPhrases, wordInfo)

        return wordInfo
    }

    private fun checkWord(sourceWord: String): WordInfo {
        val word = prepareWord(sourceWord)
        val wordInfo = WordInfo(word)

        val firstLetter: Char = word.first()

        if (foulData.containsKey(firstLetter.toString()))
            checkRegexpsMapOfArray(foulData, wordInfo)

        if (wordInfo.good)
            checkRegexpsMap(foulCore, wordInfo)

        if (wordInfo.good)
            checkRegexpsArray(badSemiPhrases, wordInfo)

        if (!wordInfo.good)
            checkRegexpsMap(excludesCore, wordInfo, accuse = false)

        if (!wordInfo.good && excludesData.containsKey(firstLetter.toString()))
            checkRegexpsArray(excludesData[firstLetter.toString()]!!, wordInfo, accuse = false)

        return wordInfo
    }

    private fun checkRegexpsArray(regexps: Array<Regex>, wordInfo: WordInfo, accuse: Boolean = true, breakOnFirst: Boolean = true) {
        for(regex: Regex in regexps)
        {
            if(checkRegex(regex, wordInfo, accuse, regex.toString(), breakOnFirst))
                break
        }
    }

    private fun checkRegexpsMap(regexps: Map<String, Regex>, wordInfo: WordInfo, accuse: Boolean = true, breakOnFirst: Boolean = true) {
        for(rule: String in regexps.keys)
        {
            val regex: Regex = regexps[rule]!!
            if(checkRegex(regex, wordInfo, accuse, rule, breakOnFirst))
                break
        }
    }

    private fun checkRegexpsMapOfArray(regexps: Map<String, Array<Regex>>, wordInfo: WordInfo, accuse: Boolean = true, breakOnFirst: Boolean = true) {
        for(rule: String in regexps.keys)
        {
            for(regex: Regex in regexps[rule]!!) {
                if(checkRegex(regex, wordInfo, accuse, rule, breakOnFirst))
                    break
            }
        }
    }

    private fun checkRegex(
        regex: Regex,
        wordInfo: WordInfo,
        accuse: Boolean,
        rule: String,
        breakOnFirst: Boolean
    ): Boolean {
        if (!regex.containsMatchIn(wordInfo.word))
            return false

        if (accuse) {
            wordInfo.good = false
            wordInfo.accuse.add(rule)
        } else {
            wordInfo.good = true
            wordInfo.excuse.add(rule)
        }

        return breakOnFirst
    }

    private fun prepareWord(word: String): String {
        var processedWord = word

        if(!isPiOrEWord(word))
            processedWord = processedWord.replace(CommonLanguagePatterns.PUNCTUATION_3, "")

        processedWord = processedWord.lowercase()

        for ((pattern, replacement) in patternReplacements)
            processedWord = pattern.replace(processedWord, replacement)

        processedWord = replaceCharacters(processedWord)

        return removeDuplicates(processedWord)
    }

    private fun removeDuplicates(word: String): String {
        var buf = ""
        var prevChar = ' '
        var count = 1

        for (char in word) {
            if (char == prevChar) {
                count++
                if (count < 3) {
                    buf += char
                }
            } else {
                count = 1
                buf += char
                prevChar = char
            }
        }

        return buf
    }

    private fun replaceCharacters(word: String): String {
        val stringBuilder = StringBuilder()
        word.forEach { char ->
            val translatedChar = translationsTable[char]
            if (translatedChar != null) {
                stringBuilder.append(translatedChar)
            } else {
                stringBuilder.append(char)
            }
        }
        return stringBuilder.toString()
    }

    private fun isPiOrEWord(word: String): Boolean =
        word.contains("3.14") || word.contains("2.72")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val censor = Censor(RussianCensorLanguageImpl())
            println(censor.cleanLine("выбоены выебоны оскорблять"))
        }
    }
}

data class CensorResponse (
    var line: String,

    var detectedBadWords: MutableList<String>,
    var detectedBadPhrases: MutableList<String>,
    var detectedPats: MutableList<String>,
)

private data class CensorLineResponse (
    var good: Boolean = true,
    var badWordInfo: WordInfo? = null
)

private data class WordInfo (
    val word: String,
    var good: Boolean = true,
    val accuse: MutableList<String> = mutableListOf(),
    val excuse: MutableList<String> = mutableListOf()
)