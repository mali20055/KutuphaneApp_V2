package com.example.kutuphaneapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book

class SearchResultAdapter(private val onAddClick: (Book) -> Unit) :
    RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    private var books: List<Book> = emptyList()

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        private val tvPublisherYear: TextView = view.findViewById(R.id.tvPublisherYear)
        private val tvPageCount: TextView = view.findViewById(R.id.tvPageCount)
        private val ivBookCover: ImageView = view.findViewById(R.id.ivBookCover)
        private val btnAddLibrary: Button = view.findViewById(R.id.btnAddLibrary)

        fun bind(book: Book) {
            tvTitle.text = book.title
            tvAuthor.text = book.author
            
            val publisherYear = buildString {
                if (book.publisher.isNotEmpty()) append(book.publisher)
                if (book.publisher.isNotEmpty() && book.publishedDate.isNotEmpty()) append(" • ")
                if (book.publishedDate.isNotEmpty()) append(book.publishedDate.take(4))
            }
            tvPublisherYear.text = publisherYear

            if (book.pageCount > 0) {
                tvPageCount.visibility = View.VISIBLE
                tvPageCount.text = "${book.pageCount} sayfa"
            } else {
                tvPageCount.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.color.input_background)
                .error(R.color.input_background)
                .into(ivBookCover)

            btnAddLibrary.setOnClickListener {
                onAddClick(book)
            }
        }
    }
}
