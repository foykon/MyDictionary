package com.example.mydictionary.quiz

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.mydictionary.R
import com.example.mydictionary.data.Word
import com.example.mydictionary.data.WordDatabase
import com.example.mydictionary.databinding.FragmentSoundQuizBinding
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.Locale

class SoundQuizFragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentSoundQuizBinding? = null
    private val binding get() = _binding!!
    private var score = 0
    private var currentQuestionIndex = 0
    private var selectedAnswerCard: MaterialCardView? = null
    private lateinit var textToSpeech: TextToSpeech
    private var questions: List<SoundQuizQuestion> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSoundQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textToSpeech = TextToSpeech(requireContext(), this)
        loadQuestions()
        setupAnswerCardListeners()
        setupSoundButton()
    }

    private fun loadQuestions() {
        lifecycleScope.launch {
            val database = WordDatabase.getDatabase(requireContext())
            val allWords = database.wordDao().getAllWords().value ?: emptyList()
            
            // En az 4 kelime varsa quiz'i başlat
            if (allWords.size >= 4) {
                // Rastgele 4 kelime seç
                val selectedWords = allWords.shuffled().take(4)
                
                // Her kelime için bir soru oluştur
                questions = selectedWords.map { word ->
                    SoundQuizQuestion(
                        textToSpeak = word.word,
                        correctImageUrl = word.imageUrl,
                        options = selectedWords.map { option -> option.imageUrl }
                    )
                }
                
                setupQuestion()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Türkçe dil desteği yoksa varsayılan dili kullan
            }
        }
    }

    private fun setupQuestion() {
        if (questions.isEmpty()) return
        
        val currentQuestion = questions[currentQuestionIndex]
        
        // Cevapları karıştır
        val shuffledOptions = currentQuestion.options.shuffled()
        
        // Resimleri yükle
        Glide.with(this)
            .load(shuffledOptions[0])
            .into(binding.answer1)
            
        Glide.with(this)
            .load(shuffledOptions[1])
            .into(binding.answer2)
            
        Glide.with(this)
            .load(shuffledOptions[2])
            .into(binding.answer3)
            
        Glide.with(this)
            .load(shuffledOptions[3])
            .into(binding.answer4)

        // Skor ve soru numarası güncelle
        binding.scoreText.text = "Puan: $score"
        binding.questionNumber.text = "Soru ${currentQuestionIndex + 1}/4"
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

    private fun setupSoundButton() {
        binding.soundButton.setOnClickListener {
            playCurrentSound()
        }
    }

    private fun playCurrentSound() {
        if (questions.isEmpty()) return
        val currentQuestion = questions[currentQuestionIndex]
        textToSpeech.speak(currentQuestion.textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun handleAnswerSelection(selectedCard: MaterialCardView) {
        if (questions.isEmpty()) return
        
        // Önceki seçimi temizle
        selectedAnswerCard?.isChecked = false
        
        // Yeni seçimi işaretle
        selectedCard.isChecked = true
        selectedAnswerCard = selectedCard

        // Cevabı kontrol et
        val selectedImageUrl = when (selectedCard) {
            binding.answer1Card -> binding.answer1.tag
            binding.answer2Card -> binding.answer2.tag
            binding.answer3Card -> binding.answer3.tag
            binding.answer4Card -> binding.answer4.tag
            else -> null
        }

        val currentQuestion = questions[currentQuestionIndex]
        if (selectedImageUrl == currentQuestion.correctImageUrl) {
            // Doğru cevap
            score += 10
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            // Kısa bir gecikme sonra sonraki soruya geç
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    setupQuestion()
                } else {
                    // Quiz bitti, sonuç ekranına git
                    showResults()
                }
            }, 1000)
        } else {
            // Yanlış cevap
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
        }
    }

    private fun showResults() {
        // Sonuç ekranına git
        // TODO: Implement results screen
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.stop()
        textToSpeech.shutdown()
        _binding = null
    }
}

data class SoundQuizQuestion(
    val textToSpeak: String,
    val correctImageUrl: String,
    val options: List<String>
) 