// GameCardAdapter.kt — Adapter untuk grid supported games
// Kalau game tidak ter-install: tampilkan warning icon + tap → Play Store
// Kalau sudah install: tampilkan normal + tap → launch overlay + game

package com.frezen.hub.ui

import android.content.Context
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.frezen.hub.R
import com.frezen.hub.model.SupportedGame
import com.frezen.hub.utils.GameManager

class GameCardAdapter(
    private val context: Context,
    private var games: List<SupportedGame>,
    private val onGameClick: (SupportedGame) -> Unit
) : RecyclerView.Adapter<GameCardAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivGame:    ImageView  = view.findViewById(R.id.iv_game_icon)
        val ivWarning: ImageView  = view.findViewById(R.id.iv_warning)
        val tvName:    TextView   = view.findViewById(R.id.tv_game_name)
        val cardRoot:  View       = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_game_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val game = games[position]

        holder.ivGame.setImageResource(game.iconResId)
        holder.tvName.text = game.name

        // Show warning overlay kalau tidak ter-install
        holder.ivWarning.visibility = if (game.isInstalled) View.GONE else View.VISIBLE

        // Grayscale kalau tidak ter-install, normal kalau sudah
        if (game.isInstalled) {
            holder.ivGame.colorFilter = null
            holder.ivGame.alpha = 1f
        } else {
            // Grayscale filter
            val matrix = android.graphics.ColorMatrix()
            matrix.setSaturation(0f)
            holder.ivGame.colorFilter =
                android.graphics.ColorMatrixColorFilter(matrix)
            holder.ivGame.alpha = 0.7f
        }

        holder.cardRoot.setOnClickListener {
            onGameClick(game)
        }
    }

    override fun getItemCount() = games.size

    fun updateGames(newGames: List<SupportedGame>) {
        games = newGames
        notifyDataSetChanged()
    }
}
