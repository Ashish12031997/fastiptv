package com.fastiptv.ui.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class EpgTimeSlot(
    val formattedTime: String,
    val startEpochSec: Long,
    val endEpochSec: Long
)

@HiltViewModel
class EpgGridViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _timeSlots = MutableStateFlow<List<EpgTimeSlot>>(emptyList())
    val timeSlots: StateFlow<List<EpgTimeSlot>> = _timeSlots.asStateFlow()

    private val _programsMap = MutableStateFlow<Map<Int, List<EpgProgram>>>(emptyMap())
    val programsMap: StateFlow<Map<Int, List<EpgProgram>>> = _programsMap.asStateFlow()

    private val _selectedProgram = MutableStateFlow<Pair<Channel, EpgProgram>?>(null)
    val selectedProgram: StateFlow<Pair<Channel, EpgProgram>?> = _selectedProgram.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        generateTimeSlots()
        loadCategoriesAndChannels()
    }

    private fun generateTimeSlots() {
        val calendar = Calendar.getInstance()
        // Round down to current half hour
        val minute = calendar.get(Calendar.MINUTE)
        val roundedMinute = if (minute >= 30) 30 else 0
        calendar.set(Calendar.MINUTE, roundedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val slots = mutableListOf<EpgTimeSlot>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        for (i in 0 until 8) { // 4 hours window (8 x 30m)
            val startSec = calendar.timeInMillis / 1000
            val formatted = timeFormat.format(calendar.time)
            calendar.add(Calendar.MINUTE, 30)
            val endSec = calendar.timeInMillis / 1000
            slots.add(EpgTimeSlot(formatted, startSec, endSec))
        }

        _timeSlots.value = slots
    }

    private fun loadCategoriesAndChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.observeLiveCategories().collect { catList ->
                _categories.value = catList
                if (_selectedCategory.value == null && catList.isNotEmpty()) {
                    selectCategory(catList.first())
                }
            }
        }
    }

    private var channelsJob: Job? = null

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
        channelsJob?.cancel()
        channelsJob = viewModelScope.launch {
            _isLoading.value = true
            repository.observeChannelsByCategory(category.id).collect { channelList ->
                _channels.value = channelList
                _isLoading.value = false
                fetchProgramsForChannels(channelList.take(25)) // Load top 25 for fast response
            }
        }
    }

    private fun fetchProgramsForChannels(channels: List<Channel>) {
        viewModelScope.launch {
            val currentMap = _programsMap.value.toMutableMap()
            val channelsToFetch = channels.filter { !currentMap.containsKey(it.id) }
            if (channelsToFetch.isEmpty()) return@launch

            // Fetch in chunks of 5 concurrently for rapid response without overwhelming connection pool
            channelsToFetch.chunked(5).forEach { chunk ->
                coroutineScope {
                    chunk.map { channel ->
                        async {
                            val result = repository.getShortEpg(channel.id)
                            channel.id to result.getOrDefault(emptyList())
                        }
                    }.awaitAll().forEach { (id, list) ->
                        currentMap[id] = list
                    }
                }
                _programsMap.value = currentMap.toMap()
            }
        }
    }

    fun selectProgram(channel: Channel, program: EpgProgram) {
        _selectedProgram.value = Pair(channel, program)
    }

    fun dismissProgramDetail() {
        _selectedProgram.value = null
    }
}
