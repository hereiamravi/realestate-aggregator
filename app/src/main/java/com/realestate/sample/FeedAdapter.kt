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
        private val priceBadge: TextView = view.findViewById(R.id.priceBadge)
        private val propertyTypeTag: TextView = view.findViewById(R.id.propertyTypeTag)
        private val locationText: TextView = view.findViewById(R.id.locationText)
        private val bookmarkButton: ImageButton = view.findViewById(R.id.bookmarkButton)

        fun bind(post: Post, isBookmarked: Boolean = false, onBookmarkClick: ((Post) -> Unit)? = null) {
            // Hero Thumbnail
            post.media?.firstOrNull()?.let { mediaItem ->
                thumbnail.load(mediaItem.url) {
                    crossfade(true)
                }
            }

            // Caption
            caption.text = post.caption ?: "No description available"

            // Price Badge
            val rawPrice = post.extracted?.price?.raw
            val priceVal = post.extracted?.price?.value
            val currency = post.extracted?.price?.currency ?: "INR"

            if (!rawPrice.isNullOrBlank()) {
                priceBadge.text = rawPrice
            } else if (priceVal != null && priceVal > 0) {
                val symbol = if (currency == "INR") "₹" else "$"
                val num = priceVal.toLong()
                priceBadge.text = if (num >= 10000000) {
                    "$symbol${String.format("%.2f", num / 10000000.0)} Cr"
                } else if (num >= 100000) {
                    "$symbol${String.format("%.2f", num / 100000.0)} Lakhs"
                } else {
                    "$symbol$num"
                }
            } else {
                priceBadge.text = "Price on Request"
            }

            // Property Type Tag
            val propType = post.extracted?.property_type?.uppercase() ?: "PROPERTY"
            propertyTypeTag.text = propType

            // Location
            val loc = post.extracted?.location ?: "Prime Location"
            locationText.text = "📍 $loc"

            // Bookmark button
            bookmarkButton.alpha = if (isBookmarked) 1.0f else 0.4f
            bookmarkButton.contentDescription = if (isBookmarked) "Bookmarked" else "Bookmark"
            bookmarkButton.setOnClickListener { onBookmarkClick?.invoke(post) }
        }
    }
}
