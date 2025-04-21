package com.example.mydictionary.ui.quiz

import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import com.example.mydictionary.databinding.FragmentQuiz3Binding
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import com.google.android.material.card.MaterialCardView
import java.util.Locale

class Quiz3Fragment : Fragment(), TextToSpeech.OnInitListener {
    private var _binding: FragmentQuiz3Binding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DictionaryViewModel
    private var currentQuestionIndex = 0
    private var score = 0
    private lateinit var questions: List<Quiz3Question>
    private var selectedAnswerCard: MaterialCardView? = null
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuiz3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]
        textToSpeech = TextToSpeech(requireContext(), this)
        setupQuiz()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, "Türkçe dil desteği bulunamadı!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "TextToSpeech başlatılamadı!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupQuiz() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.size >= 5) {
                // Rastgele 5 kelime seç
                val randomWords = words.shuffled().take(5)
                questions = randomWords.map { word ->
                    // Her soru için 3 yanlış resim seç
                    val wrongImages = (words - word).shuffled().take(3)
                    val allImages = (wrongImages + word).shuffled()
                    Quiz3Question(word, allImages)
                }
                showQuestion(0)
            } else {
                Toast.makeText(context, "Lütfen en az 5 kelime ekleyin!", Toast.LENGTH_LONG).show()
            }
        }

        setupAnswerCardListeners()
        setupPlaySoundButton()
    }

    private fun setupPlaySoundButton() {
        binding.playSoundCard.setOnClickListener {
            playCurrentWordSound()
        }
    }

    private fun playCurrentWordSound() {
        val currentQuestion = questions[currentQuestionIndex]
        val word = currentQuestion.correctWord.word
        textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun showQuestion(index: Int) {
        val question = questions[index]
        binding.scoreText.text = "Puan: $score"
        
        // Kartları sıfırla
        resetAllCards()

        // Resimleri göster
        val answerImages = listOf(
            binding.answer1,
            binding.answer2,
            binding.answer3,
            binding.answer4
        )

        // Resimleri ve kelimeleri birlikte karıştır
        val shuffledPairs = question.allImages.mapIndexed { index, word -> 
            Pair(word, answerImages[index])
        }.shuffled()
        
        // Tüm resimleri göster
        shuffledPairs.forEach { (word, imageView) ->
            Glide.with(this)
                .load(word.imageUrl)
                .into(imageView)
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
            card.isEnabled = true
            card.alpha = 1.0f
            card.strokeWidth = 0
            card.strokeColor = 0
            card.setCardBackgroundColor(android.graphics.Color.parseColor(color))
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
        if (!selectedCard.isEnabled) return

        // Tüm kartları geçici olarak devre dışı bırak
        disableAllCards()
        selectedAnswerCard = selectedCard

        // Seçilen resmin kelimesini bul
        val selectedWord = when (selectedCard) {
            binding.answer1Card -> questions[currentQuestionIndex].allImages[0]
            binding.answer2Card -> questions[currentQuestionIndex].allImages[1]
            binding.answer3Card -> questions[currentQuestionIndex].allImages[2]
            binding.answer4Card -> questions[currentQuestionIndex].allImages[3]
            else -> null
        }

        // Doğru cevabı kontrol et
        if (selectedWord?.id == questions[currentQuestionIndex].correctWord.id) {
            // Doğru cevap animasyonu ve geri bildirimi
            score += 10
            selectedCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
            selectedCard.strokeWidth = 4
            selectedCard.strokeColor = resources.getColor(android.R.color.holo_green_dark, null)
            val bounceAnimation = AnimationUtils.loadAnimation(context, R.anim.bounce)
            selectedCard.startAnimation(bounceAnimation)
            
            Toast.makeText(context, "Doğru! +10 puan", Toast.LENGTH_SHORT).show()
            
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    showFinalResult()
                }
            }, 1000)
        } else {
            // Yanlış cevap animasyonu ve geri bildirimi
            selectedCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
            selectedCard.strokeWidth = 4
            selectedCard.strokeColor = resources.getColor(android.R.color.holo_red_dark, null)
            val shakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake)
            selectedCard.startAnimation(shakeAnimation)
            
            // Doğru cevabı göster
            val correctImageIndex = questions[currentQuestionIndex].allImages.indexOf(questions[currentQuestionIndex].correctWord)
            val correctCard = when (correctImageIndex) {
                0 -> binding.answer1Card
                1 -> binding.answer2Card
                2 -> binding.answer3Card
                3 -> binding.answer4Card
                else -> null
            }
            
            correctCard?.let {
                it.setCardBackgroundColor(resources.getColor(android.R.color.holo_green_light, null))
                it.strokeWidth = 4
                it.strokeColor = resources.getColor(android.R.color.holo_green_dark, null)
            }
            
            Toast.makeText(context, "Tekrar dene!", Toast.LENGTH_SHORT).show()
            
            selectedCard.postDelayed({
                if (currentQuestionIndex < questions.size - 1) {
                    currentQuestionIndex++
                    showQuestion(currentQuestionIndex)
                } else {
                    showFinalResult()
                }
            }, 1500)
        }
    }

    private fun disableAllCards() {
        val answerCards = listOf(
            binding.answer1Card,
            binding.answer2Card,
            binding.answer3Card,
            binding.answer4Card
        )

        answerCards.forEach { card ->
            card.isEnabled = false
            card.alpha = 0.7f
        }
    }

    private fun showFinalResult() {
        val message = when (score) {
            50 -> "Mükemmel! Kelime haznen çok iyi!"
            in 40..49 -> "Harika! Neredeyse mükemmel!"
            in 30..39 -> "İyi iş! Pratik yapmaya devam et!"
            in 20..29 -> "Fena değil! Daha iyisini yapabilirsin!"
            else -> "Öğrenmeye devam et! Pratik yapmak mükemmelleştirir!"
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Quiz Tamamlandı!")
            .setMessage("Puanın: $score/50\n\n$message")
            .setPositiveButton("Tekrar Dene") { _, _ ->
                // Quiz'i sıfırla
                score = 0
                currentQuestionIndex = 0
                setupQuiz()
            }
            .setNegativeButton("Ana Sayfaya Dön") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        textToSpeech.stop()
        textToSpeech.shutdown()
        _binding = null
    }
}

data class Quiz3Question(
    val correctWord: Word,
    val allImages: List<Word>
) 