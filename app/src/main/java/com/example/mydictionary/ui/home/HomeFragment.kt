package com.example.mydictionary.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mydictionary.R
import com.example.mydictionary.databinding.FragmentHomeBinding
import com.example.mydictionary.ui.quiz.QuizFragment
import com.example.mydictionary.ui.quiz.Quiz2Fragment

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
        binding.quizCard1.setOnClickListener {
            startQuiz(1)
        }
        
        binding.quizCard2.setOnClickListener {
            startQuiz(2)
        }
    }

    private fun startQuiz(quizType: Int) {
        val fragment = when (quizType) {
            1 -> QuizFragment()
            2 -> Quiz2Fragment()
            else -> return
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