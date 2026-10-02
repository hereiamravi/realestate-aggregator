package com.realestate.sample

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.realestate.sdk.models.Post

/**
 * Deep-link helper per DEVELOPMENT_TODO.md item I:
 * prefer Instagram app URI when possible, fallback to web URL.
 */
object InstagramDeepLink {

    /** App URI: instagram://media?id={instagram_media_id} */
    fun appUri(post: Post): Uri? {
        val mediaId = post.instagram_post_id?.takeIf { it.isNotBlank() } ?: return null
        return Uri.parse("instagram://media?id=$mediaId")
    }

    /** Web fallback: https://www.instagram.com/p/{shortcode}/ else original_url */
    fun webUri(post: Post): Uri? {
        val shortcode = post.shortcode?.takeIf { it.isNotBlank() }
        if (shortcode != null) return Uri.parse("https://www.instagram.com/p/$shortcode/")
        val original = post.original_url?.takeIf { it.isNotBlank() } ?: return null
        return Uri.parse(original)
    }

    fun openPost(context: Context, post: Post) {
        val app = appUri(post)
        val web = webUri(post)
        try {
            if (app != null) {
                context.startActivity(Intent(Intent.ACTION_VIEW, app))
                return
            }
            if (web != null) context.startActivity(Intent(Intent.ACTION_VIEW, web))
        } catch (ex: ActivityNotFoundException) {
            // App URI failed (Instagram not installed) -> try web fallback
            try {
                if (web != null && app != null) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, web))
                    return
                }
            } catch (ignored: ActivityNotFoundException) { }
            Toast.makeText(context, "Cannot open Instagram", Toast.LENGTH_SHORT).show()
        }
    }
}
