package com.fastiptv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
class HomeViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeLiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<Channel>> = repository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<RecentItem>> = repository.observeRecents(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val channels: StateFlow<List<Channel>> = _selectedCategory
        .flatMapLatest { cat ->
            if (cat != null) {
                repository.observeChannelsByCategory(cat.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _focusedChannel = MutableStateFlow<Channel?>(null)
    val focusedChannel: StateFlow<Channel?> = _focusedChannel.asStateFlow()

    private val _focusedProgram = MutableStateFlow<EpgProgram?>(null)
    val focusedProgram: StateFlow<EpgProgram?> = _focusedProgram.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var epgFetchJob: Job? = null

    init {
        // Auto-select first category when loaded
        viewModelScope.launch {
            categories.collect { cats ->
                if (_selectedCategory.value == null && cats.isNotEmpty()) {
                    _selectedCategory.value = cats.first()
                }
            }
        }

        // Auto-focus first channel when channels change
        viewModelScope.launch {
            channels.collect { chList ->
                if (chList.isNotEmpty()) {
                    setFocusedChannel(chList.first())
                } else {
                    _focusedChannel.value = null
                    _focusedProgram.value = null
                }
            }
        }
    }

    fun selectCategoryById(categoryId: String?) {
        if (categoryId.isNullOrBlank()) return
        val currentCats = categories.value
        val found = currentCats.find { it.id == categoryId }
        if (found != null) {
            selectCategory(found)
        } else {
            viewModelScope.launch {
                categories.collect { cats ->
                    cats.find { it.id == categoryId }?.let { cat ->
                        selectCategory(cat)
                    }
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun setFocusedChannel(channel: Channel) {
        _focusedChannel.value = channel
        epgFetchJob?.cancel()
        epgFetchJob = viewModelScope.launch {
            delay(150)
            val result = repository.getShortEpg(channel.id)
            result.onSuccess { programs ->
                val nowSec = System.currentTimeMillis() / 1000
                val current = programs.firstOrNull { it.startTimestamp <= nowSec && it.endTimestamp >= nowSec }
                    ?: programs.firstOrNull()
                _focusedProgram.value = current
            }.onFailure {
                _focusedProgram.value = null
            }
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun getChannelsForCategory(categoryId: String): Flow<List<Channel>> {
        return repository.observeChannelsByCategory(categoryId)
    }
}
