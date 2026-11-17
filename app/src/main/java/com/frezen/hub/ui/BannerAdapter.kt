// BannerAdapter.kt — ViewPager2 banner slideshow
package com.frezen.hub.ui

import android.content.Context
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.frezen.hub.R

data class BannerItem(
    val imageResId: Int,
    val title: String,
    val subtitle: String
)

class BannerAdapter(
    private val context: Context,
    private val items: List<BannerItem>
) : RecyclerView.Adapter<BannerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner:  ImageView = view.findViewById(R.id.iv_banner)
        val tvTitle:   TextView  = view.findViewById(R.id.tv_banner_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_banner_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_banner, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivBanner.setImageResource(item.imageResId)
        holder.tvTitle.text    = item.title
        holder.tvSubtitle.text = item.subtitle
    }

    override fun getItemCount() = items.size
}
