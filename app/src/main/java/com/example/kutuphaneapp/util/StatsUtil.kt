package com.example.kutuphaneapp.util

import com.example.kutuphaneapp.model.Book
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.random.Random

object StatsUtil {

    data class StatsData(
        val readingGoal: Int,
        val readCount: Int,
        val readingCount: Int,
        val toReadCount: Int,
        val totalReadPages: Int,
        val averageBookLength: Int,
        val thickestBook: Book?,
        val shortestBook: Book?,
        val oldestPublishedBook: Book?,
        val firstAddedBook: Book?,
        val favoriteAuthor: Map.Entry<String, Int>?,
        val favoritePublisher: Map.Entry<String, Int>?,
        val topTags: List<String>,
        val masterpieceCount: Int,
        val randomMasterpiece: Book?,
        val monthlyFinished: IntArray,
        val avgFinishDays: Int,
        val dustyShelfBook: Book?,
        val dailySpeed: Float,
        val ratingCounts: IntArray,
        val authorDiversityPercent: Int,
        val completionPercent: Int,
        val streakMonths: Int
    )

    fun computeAllStats(books: List<Book>, readingGoal: Int): StatsData {
        val totalBooks = books.size
        val readBooks = books.filter { it.status == "read" }
        val readingBooks = books.filter { it.status == "reading" }
        val toReadBooks = books.filter { it.status == "to_read" }

        val totalReadPages = readBooks.sumOf { it.pageCount.coerceAtLeast(0) }
        val averageBookLength = books.map { it.pageCount }.filter { it > 0 }.average().let { if (it.isNaN()) 0 else it.roundToInt() }

        val validPageBooks = books.filter { it.pageCount > 0 }
        val thickestBook = validPageBooks.maxByOrNull { it.pageCount }
        val shortestBook = validPageBooks.minByOrNull { it.pageCount }
        val oldestPublishedBook = books.mapNotNull { book ->
            extractYear(book.publishedDate)?.let { year -> book to year }
        }.minByOrNull { it.second }?.first
        val firstAddedBook = books.filter { it.addedAt > 0L }.minByOrNull { it.addedAt }

        val favoriteAuthor = books.map { it.author.trim() }.filter { it.isNotBlank() && it != "Bilinmiyor" }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }
        val favoritePublisher = books.map { it.publisher.trim() }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }

        val topTags = books.flatMap { it.tags }.map { it.trim() }.filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.take(3)
            .map { "${it.first} (${it.second})" }

        val masterpieces = books.filter { it.rating >= 5f }
        val randomMasterpiece = if (masterpieces.isNotEmpty()) masterpieces[Random.nextInt(masterpieces.size)] else null

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val monthlyFinished = IntArray(12)
        readBooks.filter { it.finishedAt > 0L }.forEach { book ->
            val cal = Calendar.getInstance().apply { timeInMillis = book.finishedAt }
            if (cal.get(Calendar.YEAR) == currentYear) {
                val month = cal.get(Calendar.MONTH)
                monthlyFinished[month] = monthlyFinished[month] + 1
            }
        }

        val finishDurations = readBooks.mapNotNull { book ->
            if (book.addedAt > 0L && book.finishedAt > book.addedAt) {
                ((book.finishedAt - book.addedAt) / (1000 * 60 * 60 * 24)).toInt()
            } else null
        }
        val avgFinishDays = finishDurations.average().let { if (it.isNaN()) 0 else it.roundToInt() }

        val dustyShelfBook = toReadBooks.filter { it.addedAt > 0L }.minByOrNull { it.addedAt }

        val firstAddedAt = books.filter { it.addedAt > 0L }.minOfOrNull { it.addedAt } ?: System.currentTimeMillis()
        val daysSinceStart = (((System.currentTimeMillis() - firstAddedAt).coerceAtLeast(1L)) / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(1)
        val dailySpeed = totalReadPages.toFloat() / daysSinceStart.toFloat()

        val ratingCounts = IntArray(5)
        books.forEach { book ->
            val ratingInt = book.rating.roundToInt()
            if (ratingInt in 1..5) {
                ratingCounts[ratingInt - 1] = ratingCounts[ratingInt - 1] + 1
            }
        }

        val distinctAuthors = books.map { it.author.trim() }.filter { it.isNotBlank() && it != "Bilinmiyor" }.distinct().size
        val authorDiversityPercent = if (totalBooks == 0) 0 else (distinctAuthors * 100 / totalBooks)
        val completionPercent = if (totalBooks == 0) 0 else (readBooks.size * 100 / totalBooks)
        val streakMonths = calculateMonthlyStreak(readBooks)

        return StatsData(
            readingGoal = readingGoal,
            readCount = readBooks.size,
            readingCount = readingBooks.size,
            toReadCount = toReadBooks.size,
            totalReadPages = totalReadPages,
            averageBookLength = averageBookLength,
            thickestBook = thickestBook,
            shortestBook = shortestBook,
            oldestPublishedBook = oldestPublishedBook,
            firstAddedBook = firstAddedBook,
            favoriteAuthor = favoriteAuthor,
            favoritePublisher = favoritePublisher,
            topTags = topTags,
            masterpieceCount = masterpieces.size,
            randomMasterpiece = randomMasterpiece,
            monthlyFinished = monthlyFinished,
            avgFinishDays = avgFinishDays,
            dustyShelfBook = dustyShelfBook,
            dailySpeed = dailySpeed,
            ratingCounts = ratingCounts,
            authorDiversityPercent = authorDiversityPercent,
            completionPercent = completionPercent,
            streakMonths = streakMonths
        )
    }

    fun extractYear(publishedDate: String): Int? {
        val match = Regex("(\\d{4})").find(publishedDate)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    fun calculateMonthlyStreak(readBooks: List<Book>): Int {
        val finishedMonths = readBooks.filter { it.finishedAt > 0L }.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it.finishedAt }
            cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        }.distinct().sortedDescending()

        if (finishedMonths.isEmpty()) return 0

        var streak = 1
        var current = finishedMonths.first()
        for (i in 1 until finishedMonths.size) {
            if (finishedMonths[i] == current - 1) {
                streak++
                current = finishedMonths[i]
            } else {
                break
            }
        }
        return streak
    }
}
