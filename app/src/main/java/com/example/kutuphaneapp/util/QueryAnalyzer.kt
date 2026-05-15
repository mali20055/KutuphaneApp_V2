package com.example.kutuphaneapp.util

/**
 * Kullanıcının arama sorgusunun tipini temsil eden sealed class.
 */
sealed class SearchQuery {
    data class Isbn(val value: String) : SearchQuery()
    data class Title(val value: String) : SearchQuery()
    data class Author(val value: String) : SearchQuery()
    data class TitleAndAuthor(val title: String, val author: String) : SearchQuery()
}

/**
 * Kullanıcının ham arama girdisini analiz edip SearchQuery tipine dönüştürür.
 *
 * Desteklenen formatlar:
 *   - ISBN (10 veya 13 haneli rakam): ISBN araması
 *   - "kitap adı by Yazar Adı" / "kitap adı yazar: Yazar Adı": Başlık+Yazar araması
 *   - 1-3 kelime, rakam yok, her kelime büyük harfle başlıyor: Yazar araması
 *   - Diğer her şey: Başlık araması
 */
object QueryAnalyzer {

    fun analyze(raw: String): SearchQuery {
        val trimmed = raw.trim()

        // 1. ISBN kontrolü
        if (isIsbn(trimmed)) return SearchQuery.Isbn(trimmed)

        // 2. "Başlık by Yazar" veya "Başlık yazar: Yazar" formatı
        val byPattern = Regex(
            """(.+?)\s+(?:by|yazar:|author:)\s+(.+)""",
            RegexOption.IGNORE_CASE
        )
        val byMatch = byPattern.find(trimmed)
        if (byMatch != null) {
            return SearchQuery.TitleAndAuthor(
                title = byMatch.groupValues[1].trim(),
                author = byMatch.groupValues[2].trim()
            )
        }

        // 3. Yazar mı başlık mı?
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val hasDigits = trimmed.any { it.isDigit() }

        return if (!hasDigits && words.size in 1..3 && looksLikeAuthorName(words)) {
            SearchQuery.Author(trimmed)
        } else {
            SearchQuery.Title(trimmed)
        }
    }

    fun isIsbn(value: String): Boolean {
        val digits = value.replace("-", "").replace(" ", "")
        return digits.all { it.isDigit() } && (digits.length == 10 || digits.length == 13)
    }

    /**
     * Kelimelerin bir isim kombinasyonu gibi görünüp görünmediğini kontrol eder.
     * Örn: "Tolkien", "J.R.R. Tolkien", "Orhan Pamuk" → yazar
     * Örn: "yüzüklerin efendisi", "savaş ve barış" → başlık
     */
    private fun looksLikeAuthorName(words: List<String>): Boolean {
        if (words.isEmpty()) return false
        // Tüm anlamlı kelimeler büyük harfle başlıyor mu?
        val meaningfulWords = words.filter { it.length > 1 }
        if (meaningfulWords.isEmpty()) return false
        return meaningfulWords.all { it[0].isUpperCase() }
    }

    /**
     * Google Books API için optimize edilmiş query string oluşturur.
     * intitle: / inauthor: prefix'leri çok daha kesin sonuç verir.
     */
    fun buildGoogleQuery(searchQuery: SearchQuery): String {
        return when (searchQuery) {
            is SearchQuery.Isbn -> "isbn:${searchQuery.value}"
            is SearchQuery.Title -> "intitle:${searchQuery.value}"
            is SearchQuery.Author -> "inauthor:${searchQuery.value}"
            is SearchQuery.TitleAndAuthor ->
                "intitle:${searchQuery.title} inauthor:${searchQuery.author}"
        }
    }
}
