package com.example.mydictionary.ui.dictionary

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mydictionary.databinding.FragmentDictionaryBinding
import com.example.mydictionary.databinding.ItemWordBinding
import com.example.mydictionary.data.Word
import com.bumptech.glide.Glide
import com.example.mydictionary.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.Toast

class DictionaryFragment : Fragment() {
    private var _binding: FragmentDictionaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DictionaryViewModel
    private lateinit var adapter: WordAdapter
    private var textToSpeech: TextToSpeech? = null
    private var currentPhotoPath: String? = null

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                showAddWordDialog(path)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDictionaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DictionaryViewModel::class.java]
        setupRecyclerView()
        observeWords()
        setupTextToSpeech()
        setupAddButton()
        
        // Veritabanı boşsa örnek verileri ekle
        viewModel.words.observe(viewLifecycleOwner) { words ->
            if (words.isEmpty()) {
                val sampleWords = listOf(
                    Word(
                        word = "Apple",
                        imageUrl = "https://images.unsplash.com/photo-1619546813926-a78fa6372cd2?w=500&q=80"
                    ),
                    Word(
                        word = "Banana",
                        imageUrl = "https://images.unsplash.com/photo-1543218024-57a70143c369?w=500&q=80"
                    ),
                    Word(
                        word = "Orange",
                        imageUrl = "https://images.unsplash.com/photo-1587735243615-c03f25aaff15?w=500&q=80"
                    ),
                    Word(
                        word = "Strawberry",
                        imageUrl = "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=500&q=80"
                    ),
                    Word(
                        word = "Grape",
                        imageUrl = "https://images.unsplash.com/photo-1590739225289-2f1e6b1f9a0b?w=500&q=80"
                    ),
                    Word(
                        word = "Watermelon",
                        imageUrl = "https://images.unsplash.com/photo-1563114773-84221bd62daa?w=500&q=80"
                    )
                )
                
                sampleWords.forEach { word ->
                    viewModel.insert(word)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = WordAdapter { word, showWord ->
            if (showWord) {
                speakWord(word.word)
            }
        }
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@DictionaryFragment.adapter
        }
    }

    private fun setupTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Dil desteği yoksa varsayılan dili kullan
                }
            }
        }
    }

    private fun speakWord(word: String) {
        textToSpeech?.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun observeWords() {
        viewModel.words.observe(viewLifecycleOwner) { words ->
            adapter.submitList(words)
        }
    }

    private fun setupAddButton() {
        binding.fabAddWord.setOnClickListener {
            checkCameraPermissionAndTakePhoto()
        }
    }

    private fun checkCameraPermissionAndTakePhoto() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile.also { file ->
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            currentPhotoPath = file.absolutePath
            takePictureLauncher.launch(photoURI)
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun showAddWordDialog(imagePath: String) {
        val input = EditText(context)
        input.isEnabled = false // Başlangıçta devre dışı bırak
        
        // ML Kit ile resmi analiz et
        val image = InputImage.fromFilePath(requireContext(), Uri.parse("file://$imagePath"))
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        
        labeler.process(image)
            .addOnSuccessListener { labels ->
                // En yüksek güven skoruna sahip 3 etiketi al
                val topLabels = labels.sortedByDescending { it.confidence }
                    .take(3)
                    .filter { it.confidence > 0.7f } // %70'den yüksek güven skoruna sahip etiketleri filtrele
                
                if (topLabels.isNotEmpty()) {
                    // Birden fazla yüksek güvenli tahmin varsa, kullanıcıya seçenek sun
                    if (topLabels.size > 1) {
                        val options = topLabels.map { it.text }.toTypedArray()
                        AlertDialog.Builder(requireContext())
                            .setTitle("Tahmin Edilen Kelimeler")
                            .setItems(options) { _, which ->
                                input.setText(options[which])
                                input.isEnabled = true
                            }
                            .setNeutralButton("Manuel Giriş") { _, _ ->
                                input.setText("")
                                input.isEnabled = true
                                input.hint = "Kelimeyi manuel girin"
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        // Tek yüksek güvenli tahmin varsa, doğrudan göster
                        input.setText(topLabels[0].text)
                        input.isEnabled = true
                    }
                } else {
                    // Yüksek güvenli tahmin yoksa manuel giriş iste
                    input.isEnabled = true
                    input.hint = "Kelimeyi manuel girin"
                }
            }
            .addOnFailureListener { e ->
                // Hata durumunda kullanıcıya bilgi ver
                input.isEnabled = true
                input.hint = "Kelimeyi manuel girin"
                Toast.makeText(context, "Görüntü analizi başarısız oldu", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveWord(word: String, imageUrl: String) {
        val word = Word(word = word, imageUrl = imageUrl)
        viewModel.insert(word)
    }

    private fun handleCameraResult(imageUrl: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_word, null)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Kelime Ekle")
            .setView(dialogView)
            .setPositiveButton("Ekle") { dialog, _ ->
                val wordInput = dialogView.findViewById<EditText>(R.id.wordInput)
                val word = wordInput?.text.toString()
                if (word.isNotBlank()) {
                    saveWord(word, imageUrl)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}

class WordAdapter(private val onWordClick: (Word, Boolean) -> Unit) : 
    androidx.recyclerview.widget.ListAdapter<Word, WordAdapter.WordViewHolder>(WordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WordViewHolder(private val binding: ItemWordBinding) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        
        init {
            binding.wordImage.setOnClickListener {
                val position = adapterPosition
                if (position != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                    val word = getItem(position)
                    binding.wordText.text = word.word
                    binding.wordText.visibility = View.VISIBLE
                    onWordClick(word, true)
                }
            }
        }

        fun bind(word: Word) {
            binding.wordText.text = word.word
            binding.wordText.visibility = View.GONE
            Glide.with(binding.root)
                .load(word.imageUrl)
                .into(binding.wordImage)
        }
    }
}

class WordDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Word>() {
    override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean {
        return oldItem == newItem
    }
} 