package com.fastiptv.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeVodCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val movies: StateFlow<List<Movie>> = _selectedCategory
        .flatMapLatest { cat ->
            if (cat != null) {
                repository.observeMoviesByCategory(cat.id)
            } else {
                flowOf(emptyList<Movie>())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Auto-select first category when loaded
        viewModelScope.launch {
            categories.collect { cats ->
                if (_selectedCategory.value == null && cats.isNotEmpty()) {
                    _selectedCategory.value = cats.first()
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun refreshMovies() {
        val cat = _selectedCategory.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncMovies(cat.id)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
