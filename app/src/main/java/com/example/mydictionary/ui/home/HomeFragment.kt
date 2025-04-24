package com.example.mydictionary.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mydictionary.R
import com.example.mydictionary.data.QuizUnit
import com.example.mydictionary.data.QuizType
import com.example.mydictionary.databinding.FragmentHomeBinding
import com.example.mydictionary.ui.quiz.QuizFragment
import com.example.mydictionary.ui.quiz.Quiz2Fragment
import com.example.mydictionary.ui.quiz.Quiz3Fragment
import com.example.mydictionary.ui.quiz.SpeechQuizFragment

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var unitAdapter: UnitAdapter
    private val units = mutableListOf<QuizUnit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUnits()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // Her geri dönüşte üniteleri yeniden yükle
        units.clear()
        setupUnits()
        unitAdapter.notifyDataSetChanged()
    }

    private fun setupUnits() {
        // Load completed units and quizzes from SharedPreferences
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val completedUnits = prefs.getStringSet("completed_units", setOf()) ?: setOf()
        val completedQuizzes = prefs.getStringSet("completed_quizzes", setOf()) ?: setOf()

        // Reset all quiz types
        QuizType.values().forEach { it.resetAll() }

        // Create all 5 units
        for (i in 1..5) {
            val unitQuizzes = listOf(
                QuizType.IMAGE_TO_WORD,
                QuizType.WORD_TO_IMAGE,
                QuizType.SOUND_QUIZ,
                QuizType.SPEECH_QUIZ
            ).apply {
                // Mark completed quizzes for this unit
                forEach { quizType ->
                    if (completedQuizzes.contains("${i}_${quizType.name}")) {
                        quizType.markAsCompleted(i)
                    }
                }
            }

            // Check if all quizzes in the unit are completed
            val allQuizzesCompleted = unitQuizzes.all { it.isCompleted(i) }
            val isCompleted = allQuizzesCompleted || completedUnits.contains(i.toString())
            val isUnlocked = i == 1 || completedUnits.contains((i - 1).toString()) || (i > 1 && units[i-2].isCompleted)

            units.add(QuizUnit(
                id = i,
                name = "Unit $i",
                quizzes = unitQuizzes,
                isCompleted = isCompleted,
                isUnlocked = isUnlocked
            ))
        }
    }

    private fun setupRecyclerView() {
        unitAdapter = UnitAdapter(units) { unit, quizType ->
            if (unit.isUnlocked) {
                startQuiz(unit.id, quizType)
            }
        }
        binding.unitsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = unitAdapter
        }
    }

    private fun startQuiz(unitId: Int, quizType: QuizType) {
        val fragment = when (quizType) {
            QuizType.IMAGE_TO_WORD -> QuizFragment()
            QuizType.WORD_TO_IMAGE -> Quiz2Fragment()
            QuizType.SOUND_QUIZ -> Quiz3Fragment()
            QuizType.SPEECH_QUIZ -> SpeechQuizFragment()
        }.apply {
            arguments = Bundle().apply {
                putInt("unitId", unitId)
            }
        }
        
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 