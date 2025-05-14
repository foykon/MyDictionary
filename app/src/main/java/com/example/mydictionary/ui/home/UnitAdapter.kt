package com.example.mydictionary.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mydictionary.R
import com.example.mydictionary.data.QuizUnit
import com.example.mydictionary.data.QuizType
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class UnitAdapter(
    private val units: List<QuizUnit>,
    private val onQuizClick: (QuizUnit, QuizType) -> Unit
) : RecyclerView.Adapter<UnitAdapter.UnitViewHolder>() {

    class UnitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val unitName: TextView = view.findViewById(R.id.unitName)
        val progressIndicator: LinearProgressIndicator = view.findViewById(R.id.progressIndicator)
        val lockIcon: ImageView = view.findViewById(R.id.lockIcon)
        val quizCards: List<MaterialCardView> = listOf(
            view.findViewById(R.id.quizCard1),
            view.findViewById(R.id.quizCard2),
            view.findViewById(R.id.quizCard3),
            view.findViewById(R.id.quizCard4)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UnitViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_unit, parent, false)
        return UnitViewHolder(view)
    }

    override fun onBindViewHolder(holder: UnitViewHolder, position: Int) {
        val unit = units[position]
        holder.unitName.text = unit.name
        
        // Update progress indicator
        val completedQuizzes = unit.quizzes.count { it.isCompleted(unit.id) }
        holder.progressIndicator.progress = (completedQuizzes * 100) / unit.quizzes.size

        // Show/hide lock icon
        holder.lockIcon.visibility = if (unit.isUnlocked) View.GONE else View.VISIBLE

        // Setup quiz cards
        unit.quizzes.forEachIndexed { index, quizType ->
            if (index < holder.quizCards.size) {
                val card = holder.quizCards[index]
                
                if (unit.isUnlocked) {
                    card.setOnClickListener { onQuizClick(unit, quizType) }
                    card.isEnabled = !quizType.isCompleted(unit.id)
                    card.alpha = if (quizType.isCompleted(unit.id)) 0.5f else 1.0f
                } else {
                    card.setOnClickListener(null)
                    card.isEnabled = false
                    card.alpha = 0.3f
                }
            }
        }
    }

    override fun getItemCount() = units.size
} 