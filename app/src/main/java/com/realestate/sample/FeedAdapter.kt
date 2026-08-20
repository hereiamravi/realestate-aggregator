package com.realestate.sample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.realestate.sdk.models.Post

class FeedAdapter(private val onClick: (Post) -> Unit) : RecyclerView.Adapter<FeedAdapter.VH>() {
    private var items: List<Post> = emptyList()

    fun submitList(list: List<Post>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        holder.bind(post)
        holder.itemView.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = items.size

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(android.R.id.text1)
        private val subtitle: TextView = view.findViewById(android.R.id.text2)

        fun bind(post: Post) {
            title.text = post.extracted?.property_type ?: post.caption?.take(50) ?: ""
            subtitle.text = post.caption ?: ""
        }
    }
}
