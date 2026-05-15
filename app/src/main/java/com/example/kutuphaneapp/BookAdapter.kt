package com.example.kutuphaneapp

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

// Kitap listesini RecyclerView'da görüntülemek için kullanılan Adapter sınıfı
class BookAdapter(private val onBookClick: (Book) -> Unit) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    // Kitap listesini tutan değişken
    private var bookList: List<Book> = emptyList()

    // ViewHolder sınıfı: Görünümleri tanımlar ve bağlar
    inner class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        val tvPublisherYear: TextView = view.findViewById(R.id.tv_publisher_year)
        val chipStatus: Chip = view.findViewById(R.id.chip_status)
        val chipGroupTags: ChipGroup = view.findViewById(R.id.chip_group_tags)
        val ratingBar: RatingBar = view.findViewById(R.id.rating_bar)

        init {
            // Tıklama olayını tanımla
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onBookClick(bookList[adapterPosition]) // Tıklanan kitabı ilet
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        // item_book.xml tasarımını bağla
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]

        // Kitap başlığı, yazarı ayarla
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = book.author

        // Yayınevi ve yıl formatı
        val year = if (book.publishedDate.length >= 4) book.publishedDate.take(4) else "" // Sadece yıl al
        val publisherText = when {
            book.publisher.isNotEmpty() && year.isNotEmpty() ->
                "${book.publisher.take(20)} • $year"
            book.publisher.isNotEmpty() ->
                book.publisher.take(20)
            year.isNotEmpty() -> year
            else -> ""
        }
        holder.tvPublisherYear.text = publisherText

        holder.ratingBar.rating = book.rating

        // Glide ile kapak görselini yükle
        if (book.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(book.coverUrl)
                .placeholder(R.color.background)
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_menu_book) // Görsel yoksa varsayılan ikon
        }

        // Kitap durumuna göre chip rengini ve metnini ayarla
        when (book.status) {
            "reading" -> {
                holder.chipStatus.text = "Okuyorum"
                holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E3F2FD"))
                holder.chipStatus.setTextColor(Color.parseColor("#1565C0"))
            }
            "to_read" -> {
                holder.chipStatus.text = "Okuyacağım"
                holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFF8E1"))
                holder.chipStatus.setTextColor(Color.parseColor("#F57F17"))
            }
            "read" -> {
                holder.chipStatus.text = "Okudum"
                holder.chipStatus.chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                holder.chipStatus.setTextColor(Color.parseColor("#2E7D32"))
            }
        }

        // Etiketleri temizle ve yeniden ekle
        holder.chipGroupTags.removeAllViews()
        book.tags.forEach { tag ->
            val chip = Chip(holder.itemView.context)
            chip.text = tag
            chip.textSize = 10f
            holder.chipGroupTags.addView(chip)
        }
    }

    override fun getItemCount(): Int = bookList.size

    // Listeyi güncellemek için kullanılan fonksiyon
    fun updateList(newList: List<Book>) {
        bookList = newList
        notifyDataSetChanged()
    }
}
