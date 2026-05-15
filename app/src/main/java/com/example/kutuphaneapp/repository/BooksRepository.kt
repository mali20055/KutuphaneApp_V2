package com.example.kutuphaneapp.repository

import android.net.Uri
import com.example.kutuphaneapp.BuildConfig
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.util.StorageUtil
import com.example.kutuphaneapp.model.OpenLibraryDoc
import com.example.kutuphaneapp.model.VolumeItem
import com.example.kutuphaneapp.network.OpenLibraryRetrofitInstance
import com.example.kutuphaneapp.network.RetrofitInstance
import com.example.kutuphaneapp.util.QueryAnalyzer
import com.example.kutuphaneapp.util.SearchQuery
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BooksRepository {

    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    // ─────────────────────────────────────────────────────────────────────────
    // Firestore İşlemleri
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Kullanıcının kitaplarını anlık (Real-time) dinler ve Flow olarak döner.
     */
    fun getUserBooksFlow(): Flow<List<Book>> = callbackFlow {
        val uid = userId ?: run {
            trySend(emptyList())
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid).collection("books")
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val books = snapshot?.toObjects(Book::class.java) ?: emptyList()
                trySend(books)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Kullanıcının okuma hedefini getirir. Varsayılan: 24
     */
    suspend fun getReadingGoal(): Int {
        val uid = userId ?: return 24
        return try {
            val doc = db.collection("users").document(uid).get().await()
            doc.getLong("readingGoal")?.toInt() ?: 24
        } catch (e: Exception) { 24 }
    }

    /**
     * Kitapları tek seferlik çeker (İstatistik hesaplamaları vb. için).
     */
    suspend fun getUserBooksOnce(): List<Book> {
        val uid = userId ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(uid).collection("books").get().await()
            snapshot.toObjects(Book::class.java)
        } catch (e: Exception) { emptyList() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ana giriş noktası (Dış API Aramaları)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun searchBooks(query: String): List<Book> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        val searchQuery = QueryAnalyzer.analyze(trimmedQuery)

        when (searchQuery) {
            is SearchQuery.Isbn          -> searchByIsbn(searchQuery.value)
            is SearchQuery.Author        -> searchByAuthor(trimmedQuery, searchQuery)
            is SearchQuery.Title         -> searchByTitle(trimmedQuery, searchQuery)
            is SearchQuery.TitleAndAuthor -> searchByTitleAndAuthor(trimmedQuery, searchQuery)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ISBN Araması — her iki API paralel, ilk geçerli sonuç döner
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun searchByIsbn(isbn: String): List<Book> = coroutineScope {
        val googleDeferred = async {
            try {
                val response = RetrofitInstance.api.searchBooks(
                    query = "isbn:$isbn",
                    apiKey = apiKey,
                    maxResults = 5
                )
                response.items?.map { mapGoogleBook(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val olDeferred = async {
            try {
                val bibkey = "ISBN:$isbn"
                val olDataMap = OpenLibraryRetrofitInstance.api.getBookByIsbn(bibkey)
                val olData = olDataMap[bibkey]
                if (olData != null) {
                    listOf(
                        Book(
                            id = "OL_$isbn",
                            title = olData.title ?: "Bilinmiyor",
                            author = olData.authors?.joinToString(", ") { it.name ?: "" }
                                ?: "Bilinmiyor",
                            coverUrl = olData.cover?.large
                                ?: olData.cover?.medium
                                ?: olData.cover?.small ?: "",
                            isbn = isbn,
                            publisher = olData.publishers?.firstOrNull()?.name ?: "",
                            publishedDate = olData.publishDate ?: "",
                            description = ""
                        )
                    )
                } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val merged = googleDeferred.await() + olDeferred.await()
        val deduped = smartDeduplicate(merged)
        // ISBN aramasında sadece en alakalı tek sonuç yeter
        if (deduped.isNotEmpty()) listOf(deduped.first()) else emptyList()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Başlık Araması — paralel çağrı + füzyon + relevance sıralaması
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun searchByTitle(
        rawQuery: String,
        searchQuery: SearchQuery.Title
    ): List<Book> = coroutineScope {

        val googleQuery = QueryAnalyzer.buildGoogleQuery(searchQuery) // intitle:...

        val googleDeferred = async {
            try {
                val response = RetrofitInstance.api.searchBooks(googleQuery, apiKey)
                response.items?.map { mapGoogleBook(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val olDeferred = async {
            try {
                val response = OpenLibraryRetrofitInstance.api.searchByTitle(rawQuery)
                response.docs?.map { mapOpenLibraryDoc(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        fusionAndRank(rawQuery, googleDeferred.await(), olDeferred.await())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Yazar Araması — paralel çağrı + füzyon
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun searchByAuthor(
        rawQuery: String,
        searchQuery: SearchQuery.Author
    ): List<Book> = coroutineScope {

        val googleQuery = QueryAnalyzer.buildGoogleQuery(searchQuery) // inauthor:...

        val googleDeferred = async {
            try {
                val response = RetrofitInstance.api.searchBooks(googleQuery, apiKey)
                response.items?.map { mapGoogleBook(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        val olDeferred = async {
            try {
                val response = OpenLibraryRetrofitInstance.api.searchByAuthor(rawQuery)
                response.docs?.map { mapOpenLibraryDoc(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        fusionAndRank(rawQuery, googleDeferred.await(), olDeferred.await())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Başlık + Yazar Araması
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun searchByTitleAndAuthor(
        rawQuery: String,
        searchQuery: SearchQuery.TitleAndAuthor
    ): List<Book> = coroutineScope {

        val googleQuery = QueryAnalyzer.buildGoogleQuery(searchQuery) // intitle:... inauthor:...

        val googleDeferred = async {
            try {
                val response = RetrofitInstance.api.searchBooks(googleQuery, apiKey)
                response.items?.map { mapGoogleBook(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        // OL için başlık araması yap
        val olDeferred = async {
            try {
                val response = OpenLibraryRetrofitInstance.api.searchByTitle(searchQuery.title)
                response.docs?.map { mapOpenLibraryDoc(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        fusionAndRank(rawQuery, googleDeferred.await(), olDeferred.await())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Füzyon & Sıralama
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Birden fazla kaynak listesini alır; tekilleştirir ve relevance skoruna göre sıralar.
     */
    private fun fusionAndRank(query: String, vararg resultSets: List<Book>): List<Book> {
        val all = resultSets.flatMap { it }
        val deduped = smartDeduplicate(all)
        return deduped
            .map { book -> Pair(book, scoreBook(query, book)) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Relevance Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sorguya göre bir kitabın ne kadar alakalı olduğunu puanlar.
     * Daha yüksek puan = listenin üstünde görünür.
     */
    private fun scoreBook(query: String, book: Book): Float {
        var score = 0f
        val q = query.lowercase().trim()
        val title = book.title.lowercase().trim()
        val author = book.author.lowercase().trim()

        // ── Başlık eşleşmeleri ──────────────────────────────────────────────
        if (title == q) score += 100f            // Tam eşleşme
        else if (title.contains(q)) score += 60f // Başlıkta birebir geçiyor
        else if (q.contains(title) && title.length > 3) score += 35f

        // Kelime kelime eşleşme (kısa stopword'ler hariç, >= 3 karakter)
        val queryWords = q.split(Regex("\\s+")).filter { it.length >= 3 }
        if (queryWords.isNotEmpty()) {
            val matchedInTitle = queryWords.count { title.contains(it) }
            score += (matchedInTitle.toFloat() / queryWords.size) * 40f

            val matchedInAuthor = queryWords.count { author.contains(it) }
            score += (matchedInAuthor.toFloat() / queryWords.size) * 15f
        }

        // ── Veri kalitesi bonusları ──────────────────────────────────────────
        if (book.coverUrl.isNotEmpty()) score += 8f
        if (book.pageCount > 0) score += 5f
        if (book.isbn.isNotEmpty()) score += 7f
        if (book.description.isNotEmpty()) score += 5f
        if (book.publisher.isNotEmpty()) score += 3f
        if (book.publishedDate.isNotEmpty()) score += 3f

        return score
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Akıllı Tekilleştirme
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Listeden tekrarlayan kitapları temizler.
     * Öncelik sırası: ISBN eşleşmesi > başlık benzerliği
     * Duplikat bulunursa verisi daha zengin olan saklanır.
     */
    private fun smartDeduplicate(books: List<Book>): List<Book> {
        val result = mutableListOf<Book>()

        for (book in books) {
            val existingIndex = result.indexOfFirst { existing ->
                // ISBN ile birebir eşleşme
                (book.isbn.isNotEmpty() && existing.isbn.isNotEmpty()
                        && book.isbn == existing.isbn)
                    // veya başlıklar çok benzer
                    || isSimilarTitle(book.title, existing.title)
            }

            if (existingIndex == -1) {
                result.add(book)
            } else {
                // Mevcut kitabı daha zengin veriyle güncelle (merge)
                result[existingIndex] = mergeBooks(result[existingIndex], book)
            }
        }

        return result
    }

    /**
     * İki başlığın aynı kitaba ait olup olmadığını kontrol eder.
     * Birebir eşleşme veya biri diğerinin prefix'i ise aynı kabul edilir.
     * Örn: "Lord of the Rings" ve "The Lord of the Rings" → aynı
     */
    private fun isSimilarTitle(title1: String, title2: String): Boolean {
        val t1 = title1.lowercase().trim().removePrefix("the ").removePrefix("a ")
        val t2 = title2.lowercase().trim().removePrefix("the ").removePrefix("a ")
        if (t1.isEmpty() || t2.isEmpty()) return false
        if (t1 == t2) return true
        // Uzunsa prefix kontrolü (kısa başlıkları yanlış eşleştirmesin)
        if (t1.length > 8 && t2.startsWith(t1)) return true
        if (t2.length > 8 && t1.startsWith(t2)) return true
        return false
    }

    /**
     * İki kitap kaydını birleştirir; eksik alanları secondary'den doldurur.
     */
    private fun mergeBooks(primary: Book, secondary: Book): Book {
        return primary.copy(
            coverUrl = if (primary.coverUrl.isEmpty()) secondary.coverUrl else primary.coverUrl,
            isbn = if (primary.isbn.isEmpty()) secondary.isbn else primary.isbn,
            pageCount = if (primary.pageCount == 0) secondary.pageCount else primary.pageCount,
            publisher = if (primary.publisher.isEmpty()) secondary.publisher else primary.publisher,
            publishedDate = if (primary.publishedDate.isEmpty()) secondary.publishedDate else primary.publishedDate,
            description = if (primary.description.isEmpty()) secondary.description else primary.description
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mapper'lar
    // ─────────────────────────────────────────────────────────────────────────

    private fun mapGoogleBook(item: VolumeItem): Book {
        val info = item.volumeInfo
        return Book(
            id = "GB_${item.id}",
            title = info.title ?: "Bilinmiyor",
            author = info.authors?.joinToString(", ") ?: "Bilinmiyor",
            // bestAvailableUrl() → http→https dönüşümü + zoom= regex temizliği güvenli şekilde yapılır
            coverUrl = info.imageLinks?.bestAvailableUrl() ?: "",
            isbn = info.industryIdentifiers
                ?.firstOrNull { it.type == "ISBN_13" }?.identifier
                ?: info.industryIdentifiers
                    ?.firstOrNull { it.type == "ISBN_10" }?.identifier ?: "",
            pageCount = info.pageCount ?: 0,
            publisher = info.publisher ?: "",
            publishedDate = info.publishedDate ?: "",
            description = info.description ?: ""
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Kitap Güncelleme / Silme / Kapak Yükleme
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun updateBook(book: Book): Result<Unit> {
        val uid = userId ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))
        return try {
            db.collection("users").document(uid).collection("books").document(book.id).set(book).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteBook(bookId: String): Result<Unit> {
        val uid = userId ?: return Result.failure(Exception("Kullanıcı oturumu bulunamadı"))
        return try {
            db.collection("users").document(uid).collection("books").document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun uploadCoverImage(uid: String, bookId: String, uri: Uri): Result<String> {
        return try {
            val url = StorageUtil.uploadBookCover(uid, bookId, uri)
            Result.success(url)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun mapOpenLibraryDoc(doc: OpenLibraryDoc): Book {
        // Yayın yılı: önce firstPublishYear, sonra publishYear listesindeki en küçük değer
        val publishedDate = (doc.firstPublishYear ?: doc.publishYear?.minOrNull())
            ?.toString() ?: ""

        // ISBN: önce 13 haneli, sonra ilk bulduğu
        val isbn = doc.isbn?.firstOrNull { it.length == 13 }
            ?: doc.isbn?.firstOrNull() ?: ""

        // Kapak: -M.jpg (Medium) formatı — L boyutu bazı kitaplarda 404 döner, M daha güvenilir
        val coverUrl = if (doc.coverI != null)
            "https://covers.openlibrary.org/b/id/${doc.coverI}-M.jpg"
        else ""

        return Book(
            id = "OL_${isbn.ifEmpty { doc.title?.hashCode().toString() }}",
            title = doc.title ?: "Bilinmiyor",
            author = doc.authorName?.joinToString(", ") ?: "Bilinmiyor",
            coverUrl = coverUrl,
            isbn = isbn,
            pageCount = doc.pageCount ?: 0,
            publisher = doc.publisher?.firstOrNull() ?: "",
            publishedDate = publishedDate,
            description = ""
        )
    }
}
