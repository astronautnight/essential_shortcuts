val textToMorseMap = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
    'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
    'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
    'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
    'Y' to "-.--", 'Z' to "--..",
    '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-",
    '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
    '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
    ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
    '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
)

val morseToTextMap = textToMorseMap.entries.associateBy({ it.value }) { it.key }

fun convertToMorse(input: String): String {
    val words = input.uppercase().split(Regex("\\s+"))
    val morseWords = words.map { word ->
        word.map { char ->
            textToMorseMap[char] ?: ""
        }.filter { it.toString().isNotEmpty() }.joinToString(" ")
    }
    return morseWords.joinToString(" / ")
}

fun convertToText(input: String): String {
    val words = input.split(" / ", "/", "   ")
    val textWords = words.map { word ->
        word.trim().split(Regex("\\s+")).map { char ->
            morseToTextMap[char] ?: ""
        }.joinToString("")
    }
    return textWords.joinToString(" ")
}

println("HELLO WORLD -> " + convertToMorse("HELLO WORLD"))
println("Morse -> Text -> " + convertToText(convertToMorse("HELLO WORLD")))
