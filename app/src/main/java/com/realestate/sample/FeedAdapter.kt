package com.realestate.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.realestate.sdk.models.Post
import kotlin.text.Regex

class FeedAdapter(
    private val onClick: (Post) -> Unit,
    private val onBookmarkClick: ((Post) -> Unit)? = null
) : RecyclerView.Adapter<FeedAdapter.VH>() {
    private var items: MutableList<Post> = mutableListOf()
    private var bookmarkedIds: Set<String> = emptySet()

    fun submitList(list: List<Post>) {
        items = list.toMutableList()
        notifyDataSetChanged()
    }

    fun addItems(list: List<Post>) {
        if (list.isEmpty()) return
        val startIndex = items.size
        items.addAll(list)
        notifyItemRangeInserted(startIndex, list.size)
    }

    fun setBookmarkedIds(ids: Set<String>) {
        bookmarkedIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        holder.bind(post, bookmarkedIds.contains(post.id), onBookmarkClick)
        holder.itemView.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        private val caption: TextView = view.findViewById(R.id.caption)
        private val propertyInfo: TextView = view.findViewById(R.id.propertyInfo)
        private val bookmarkButton: ImageButton = view.findViewById(R.id.bookmarkButton)

        fun bind(post: Post, isBookmarked: Boolean = false, onBookmarkClick: ((Post) -> Unit)? = null) {
            // Load thumbnail from first media item
            post.media?.firstOrNull()?.let { mediaItem ->
                thumbnail.load(mediaItem.url)
            }

            // Set caption
            caption.text = post.caption

            // Set property info (property type and price)
            val propertyType = post.extracted?.property_type
            val priceValue = post.extracted?.price?.value
            val currency = post.extracted?.price?.currency
            
            val propertyInfoText = buildString {
                append(propertyType ?: "Property")
                append(" | ")
                append(if (priceValue != null) {
                    val formatted = if (currency != null) "${currency} " else ""
                    formatted + priceValue.toString().replace(Regex("\\B(?=(\\d{3})+(?!\\d))"), ",")
                } else {
                    "Price not available"
                })
            }
            propertyInfo.text = propertyInfoText

            // Bookmark state: use alpha to indicate state (no extra drawable needed)
            bookmarkButton.alpha = if (isBookmarked) 1.0f else 0.4f
            bookmarkButton.contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark"
            bookmarkButton.setOnClickListener { onBookmarkClick?.invoke(post) }
        }
    }
}
