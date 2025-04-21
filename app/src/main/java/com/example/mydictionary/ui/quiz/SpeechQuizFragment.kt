package com.example.mydictionary.ui.quiz

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.mydictionary.databinding.FragmentSpeechQuizBinding
import com.example.mydictionary.data.Word
import com.example.mydictionary.ui.dictionary.DictionaryViewModel
import java.util.Locale

class SpeechQuizFragment : Fragment() {

    private var _binding: FragmentSpeechQuizBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DictionaryViewModel
    private lateinit var speechLauncher: ActivityResultLauncher<Intent>
    private lateinit var currentWord: Word

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpeechQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel'den kelimeleri al
        viewModel = ViewModelProvider(requireActivity())[DictionaryViewModel::class.java]

        // Speech kayıt launcher
        speechLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val matches = result.data!!
                    .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val heard = matches?.firstOrNull().orEmpty()
                binding.tvRecognized.text = heard

                if (heard.equals(currentWord.word, ignoreCase = true)) {
                    Toast.makeText(context, "Doğru! ${currentWord.word}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Yanlış. Doğru kelime: ${currentWord.word}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Butona tıklandığında konuşmayı başlat
        binding.btnSpeak.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Lütfen kelimeyi söyle")
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Cihaz bu işlemi desteklemiyor", Toast.LENGTH_SHORT).show()
            }
        }

        // Rastgele bir kelime seç ve resmi göster
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.isNotEmpty()) {
                currentWord = words.shuffled().first()
                Glide.with(this)
                    .load(currentWord.imageUrl)
                    .into(binding.speechQuizImage)
            } else {
                Toast.makeText(context, "Önce en az bir kelime ekleyin", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 