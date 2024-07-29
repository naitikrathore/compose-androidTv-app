package com.iwedia.cltv.sdk.content_aggregator

import java.lang.StringBuilder

object StringConverter {

    var list = mutableListOf<Pair<Char, String>>()

    fun init(function: () -> Unit) {

        list.add(Pair('а', "a"))
        list.add(Pair('б', "b"))
        list.add(Pair('в', "v"))
        list.add(Pair('г', "g"))
        list.add(Pair('д', "d"))
        list.add(Pair('е', "e"))
        list.add(Pair('ё', "yo"))
        list.add(Pair('ж', "ž"))
        list.add(Pair('з', "z"))
        list.add(Pair('и', "i"))
        list.add(Pair('й', "j"))
        list.add(Pair('к', "k"))
        list.add(Pair('л', "l"))
        list.add(Pair('м', "m"))
        list.add(Pair('љ', "lj"))
        list.add(Pair('њ', "nj"))
        list.add(Pair('н', "n"))
        list.add(Pair('о', "o"))
        list.add(Pair('п', "p"))
        list.add(Pair('р', "r"))
        list.add(Pair('с', "s"))
        list.add(Pair('т', "t"))
        list.add(Pair('у', "u"))
        list.add(Pair('ф', "f"))
        list.add(Pair('х', "h"))
        list.add(Pair('ц', "c"))
        list.add(Pair('ћ', "ć"))
        list.add(Pair('ч', "č"))
        list.add(Pair('ш', "š"))
        list.add(Pair('щ', "sch"))
        list.add(Pair('ъ', "j"))
        list.add(Pair('ы', "i"))
        list.add(Pair('ь', "j"))
        list.add(Pair('э', "e"))
        list.add(Pair('ю', "yu"))
        list.add(Pair('я', "ya"))
        list.add(Pair('А', "A"))
        list.add(Pair('Б', "B"))
        list.add(Pair('В', "V"))
        list.add(Pair('Г', "G"))
        list.add(Pair('Д', "D"))
        list.add(Pair('Е', "E"))
        list.add(Pair('Ё', "Yo"))
        list.add(Pair('Ж', "Zh"))
        list.add(Pair('З', "Z"))
        list.add(Pair('И', "I"))
        list.add(Pair('Й', "J"))
        list.add(Pair('К', "K"))
        list.add(Pair('Л', "L"))
        list.add(Pair('М', "M"))
        list.add(Pair('Љ', "Lj"))
        list.add(Pair('Њ', "Nj"))
        list.add(Pair('Н', "N"))
        list.add(Pair('О', "O"))
        list.add(Pair('П', "P"))
        list.add(Pair('Р', "R"))
        list.add(Pair('С', "S"))
        list.add(Pair('Т', "T"))
        list.add(Pair('У', "U"))
        list.add(Pair('Ф', "F"))
        list.add(Pair('Х', "H"))
        list.add(Pair('Ц', "C"))
        list.add(Pair('Ч', "Č"))
        list.add(Pair('Ш', "Sh"))
        list.add(Pair('Щ', "Sch"))
        list.add(Pair('Ъ', "J"))
        list.add(Pair('Ы', "I"))
        list.add(Pair('Ь', "J"))
        list.add(Pair('Э', "E"))
        list.add(Pair('Ю', "Yu"))
        list.add(Pair('Я', "Ya"))
        list.add(Pair('Ћ', "Ć"))
    }

    fun convertToLatin(source: String): String {
        if (list.size == 0) {
            init {

            }
        }

        var sb = StringBuilder()
        for (i in 0 until source.length) {
            var isFound = false
            list.forEach { pair ->
                if (pair.first == source[i]) {
                    sb.append(pair.second)
                    isFound = true
                }
            }

            if (!isFound) {
                sb.append(source[i])
            }
        }
        return sb.toString()

    }
}