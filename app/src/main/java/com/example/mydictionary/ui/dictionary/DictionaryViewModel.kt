package com.example.mydictionary.ui.dictionary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.mydictionary.data.Word
import com.example.mydictionary.data.WordDao
import com.example.mydictionary.data.WordDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {
    private val wordDao: WordDao
    val words: LiveData<List<Word>>

    init {
        val database = WordDatabase.getDatabase(application)
        wordDao = database.wordDao()
        words = wordDao.getAllWords()
    }

    fun insert(word: Word) = viewModelScope.launch(Dispatchers.IO) {
        wordDao.insert(word)
    }

    fun delete(word: Word) = viewModelScope.launch(Dispatchers.IO) {
        wordDao.delete(word)
    }

    fun update(word: Word) = viewModelScope.launch(Dispatchers.IO) {
        wordDao.update(word)
    }

    fun deleteAllWords() {
        viewModelScope.launch {
            wordDao.deleteAllWords()
        }
    }
} 