package com.example.kutuphaneapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book

class RecentBooksAdapter(private val onBookClick: (Book) -> Unit) :
    RecyclerView.Adapter<RecentBooksAdapter.ViewHolder>() {

    private var books: List<Book> = emptyList()

    fun submitList(newList: List<Book>) {
        books = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
        holder.itemView.setOnClickListener { onBookClick(book) }
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivCover: ImageView = view.findViewById(R.id.iv_book_cover)
        private val tvTitle: TextView = view.findViewById(R.id.tv_book_title)

        fun bind(book: Book) {
            tvTitle.text = book.title
            Glide.with(itemView.context)
                .load(book.coverUrl)
                .placeholder(R.drawable.ic_menu_book)
                .into(ivCover)
        }
    }
}