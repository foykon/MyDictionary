package com.example.mydictionary.ui.quiz

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mydictionary.R
import com.example.mydictionary.data.Word
import com.example.mydictionary.data.QuizType
import com.example.mydictionary.databinding.FragmentQuiz2Binding
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import com.google.android.material.card.MaterialCardView

class Quiz2Fragment : Fragment() {
    private var _binding: FragmentQuiz2Binding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DictionaryViewModel
    private var currentQuestionIndex = 0
    private var score = 0
    private lateinit var questions: List<Quiz2Question>
    private var selectedAnswerCard: MaterialCardView? = null
    private var currentUnitId: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuiz2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
        currentUnitId = arguments?.getInt("unitId", 1) ?: 1
        setupQuiz()
    }

    private fun setupQuiz() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.size >= 5) {
                // Randomly select 5 words
                val randomWords = words.shuffled().take(5)
                questions = randomWords.map { word ->
                    // Select 3 wrong images for each question
                    val wrongImages = (words - word).shuffled().take(3)
                    val allImages = (wrongImages + word).shuffled()
                    Quiz2Question(word, allImages)
                }
                showQuestion(0)
            } else {
                Toast.makeText(context, "Please add at least 5 words!", Toast.LENGTH_LONG).show()
            }
        }

        setupAnswerCardListeners()
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.scoreText.text = "Score: $score"
        
        // Reset cards
        resetAllCards()

        // Show word
        binding.wordText.text = question.correctWord.word

        // Show images
        val answerImages = listOf(
            binding.answer1,
            binding.answer2,
            binding.answer3,
            binding.answer4
        )

        // Shuffle and show images
        val shuffledPairs = question.allImages.mapIndexed { index, word -> 
            Pair(word, answerImages[index])
        }.shuffled()
        
        // Show all images
        shuffledPairs.forEach { (word, imageView) ->
            Glide.with(this)
                .load(word.imageUrl)
                .into(imageView)
            imageView.tag = word.imageUrl
        }
    }

    private fun resetAllCards() {
        val answerCards = listOf(
            binding.answer1Card to "#FFE066",
            binding.answer2Card to "#98FB98",
            binding.answer3Card to "#87CEEB",
            binding.answer4Card to "#DDA0DD"
        )

        answerCards.forEach { (card, color) ->
            card.setCardBackgroundColor(android.graphics.Color.parseColor(color))
            card.isChecked = false
        }
        selectedAnswerCard = null
    }

    private fun setupAnswerCardListeners() {
        val answerCards = listOf(
            binding.answer1Card,
            binding.answer2Card,
            binding.answer3Card,
            binding.answer4Card
        )

        answerCards.forEach { card ->
            card.setOnClickListener {
                handleAnswerSelection(card)
            }
        }
    }

    private fun handleAnswerSelection(selectedCard: MaterialCardView) {
        // Clear previous selection
        selectedAnswerCard?.isChecked = false
        
        // Mark new selection
        selectedCard.isChecked = true
        selectedAnswerCard = selectedCard

        // Check answer
        val selectedImageUrl = when (selectedCard) {
            binding.answer1Card -> binding.answer1.tag
            binding.answer2Card -> binding.answer2.tag
            binding.answer3Card -> binding.answer3.tag
            binding.answer4Card -> binding.answer4.tag
            else -> null
        }

        val currentQuestion = questions[currentQuestionIndex]
        if (selectedImageUrl == currentQuestion.correctWord.imageUrl) {
            // Correct answer
            score += 10
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            // Move to next question after delay
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    // Quiz complete
                    markQuizAsCompleted()
                    showFinalResult()
                }
            }, 1000)
        } else {
            // Wrong answer
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
        }
    }

    private fun markQuizAsCompleted() {
        QuizType.WORD_TO_IMAGE.markAsCompleted(currentUnitId)
        
        // Save completion status with unit ID
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val completedQuizzes = prefs.getStringSet("completed_quizzes", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        completedQuizzes.add("${currentUnitId}_${QuizType.WORD_TO_IMAGE.name}")
        prefs.edit().putStringSet("completed_quizzes", completedQuizzes).apply()

        // Check if all quizzes in the unit are completed
        val allQuizzesCompleted = QuizType.values().all { quizType ->
            completedQuizzes.contains("${currentUnitId}_${quizType.name}")
        }

        if (allQuizzesCompleted) {
            // Mark unit as completed
            val completedUnits = prefs.getStringSet("completed_units", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
            completedUnits.add(currentUnitId.toString())
            prefs.edit().putStringSet("completed_units", completedUnits).apply()
        }
    }

    private fun showFinalResult() {
        // Update total score
        val prefs = requireContext().getSharedPreferences("quiz_prefs", Context.MODE_PRIVATE)
        val prev = prefs.getInt("total_score", 0)
        prefs.edit().putInt("total_score", prev + score).apply()
        
        val message = when (score) {
            50 -> "Perfect! You're a vocabulary master!"
            in 40..49 -> "Great job! Almost perfect!"
            in 30..39 -> "Good work! Keep practicing!"
            in 20..29 -> "Nice try! You can do better!"
            else -> "Keep learning! Practice makes perfect!"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz Complete!")
            .setMessage("Your score: $score/50\n\n$message")
            .setPositiveButton("Try Again") { _, _ ->
                // Reset quiz
                score = 0
                currentQuestionIndex = 0
                setupQuiz()
            }
            .setNegativeButton("Back to Home") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Quiz2Question(
    val correctWord: Word,
    val allImages: List<Word>
) 