textToMorseMap = {
    'A': '.-', 'B': '-...', 'C': '-.-.', 'D': '-..', 'E': '.', 'F': '..-.',
    'G': '--.', 'H': '....', 'I': '..', 'J': '.---', 'K': '-.-', 'L': '.-..',
    'M': '--', 'N': '-.', 'O': '---', 'P': '.--.', 'Q': '--.-', 'R': '.-.',
    'S': '...', 'T': '-', 'U': '..-', 'V': '...-', 'W': '.--', 'X': '-..-',
    'Y': '-.--', 'Z': '--..',
    '0': '-----', '1': '.----', '2': '..---', '3': '...--', '4': '....-',
    '5': '.....', '6': '-....', '7': '--...', '8': '---..', '9': '----.',
    '.': '.-.-.-', ',': '--..--', '?': '..--..', '\'': '.----.', '!': '-.-.--',
    '/': '-..-.', '(': '-.--.', ')': '-.--.-', '&': '.-...', ':': '---...',
    ';': '-.-.-.', '=': '-...-', '+': '.-.-.', '-': '-....-', '_': '..--.-',
    '\"': '.-..-.', '$': '...-..-', '@': '.--.-.'
}

morseToTextMap = {v: k for k, v in textToMorseMap.items()}

import re

def convertToMorse(input_str):
    words = re.split(r'\s+', input_str.upper())
    morseWords = []
    for word in words:
        chars = [textToMorseMap.get(char, '') for char in word]
        chars = [c for c in chars if c]
        morseWords.append(' '.join(chars))
    return ' / '.join(morseWords)

def convertToText(input_str):
    temp = input_str.replace(' / ', '|||').replace('   ', '|||').replace('/', '|||')
    words = temp.split('|||')
    textWords = []
    for word in words:
        chars = re.split(r'\s+', word.strip())
        chars = [morseToTextMap.get(char, '') for char in chars]
        textWords.append(''.join(chars))
    return ' '.join(textWords)

print('HELLO WORLD ->', convertToMorse('HELLO WORLD'))
print('Morse -> Text ->', convertToText(convertToMorse('HELLO WORLD')))
