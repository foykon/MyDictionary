package com.example.mydictionary.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mydictionary.R
import com.example.mydictionary.databinding.FragmentHomeBinding
import com.example.mydictionary.ui.quiz.QuizFragment

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        setupQuizButtons()
    }

    private fun setupQuizButtons() {
        val quizCards = listOf(
            binding.quizCard1,
            binding.quizCard2,
            binding.quizCard3,
            binding.quizCard4,
            binding.quizCard5
        )

        quizCards.forEach { card ->
            card.setOnClickListener {
                startQuiz()
            }
        }
    }

    private fun startQuiz() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.flFragment, QuizFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 