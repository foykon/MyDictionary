package com.example.mydictionary

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class LeaderboardItem(
    val rank: Int,
    val username: String,
    val score: Int
)

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
    
    private var items: List<LeaderboardItem> = emptyList()

    fun updateItems(newItems: List<LeaderboardItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUsername)
        private val tvScore: TextView = itemView.findViewById(R.id.tvScore)

        fun bind(item: LeaderboardItem) {
            tvRank.text = "#${item.rank}"
            tvUsername.text = item.username
            tvScore.text = item.score.toString()
        }
    }
} 