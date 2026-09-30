package com.fastiptv.ui.livetv

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.fastiptv.data.session.SessionManager
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.EpgProgram
import com.fastiptv.domain.model.RecentItem
import com.fastiptv.domain.repository.IptvRepository
import com.fastiptv.player.PlayerState
import com.fastiptv.player.StreamPlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LiveTvViewModel @Inject constructor(
    private val repository: IptvRepository,
    private val streamPlayer: StreamPlayerController,
    private val sessionManager: SessionManager
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = streamPlayer.playerState

    val categories: StateFlow<List<Category>> = repository.observeLiveCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contentRegion: StateFlow<com.fastiptv.domain.model.ContentRegion> = sessionManager.contentRegionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.getCachedContentRegion())

    val pinnedGroups: StateFlow<Set<String>> = sessionManager.pinnedGroupsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), sessionManager.getCachedPinnedGroups())

    val recents: StateFlow<List<RecentItem>> = repository.observeRecents(limit = 20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedLanguageGroup = MutableStateFlow("RECENT")
    val selectedLanguageGroup: StateFlow<String> = _selectedLanguageGroup.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    private val _currentEpgProgram = MutableStateFlow<EpgProgram?>(null)
    val currentEpgProgram: StateFlow<EpgProgram?> = _currentEpgProgram.asStateFlow()

    private val _isBannerVisible = MutableStateFlow(true)
    val isBannerVisible: StateFlow<Boolean> = _isBannerVisible.asStateFlow()

    private val _isDrawerOpen = MutableStateFlow(false)
    val isDrawerOpen: StateFlow<Boolean> = _isDrawerOpen.asStateFlow()

    private val _switchNotice = MutableStateFlow<String?>(null)
    val switchNotice: StateFlow<String?> = _switchNotice.asStateFlow()

    // Active channels in the current surfing queue
    val channels: StateFlow<List<Channel>> = _selectedCategory
        .flatMapLatest { cat ->
            if (cat != null) {
                if (cat.id == "RECENT") {
                    repository.observeRecents(limit = 30).map { recentsList ->
                        recentsList.filter { it.type == "live" }.map { r ->
                            Channel(
                                id = r.streamId,
                                name = r.title,
                                logoUrl = r.iconUrl,
                                categoryId = "RECENT",
                                epgChannelId = null
                            )
                        }
                    }
                } else {
                    repository.observeChannelsByCategory(cat.id)
                }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var bannerHideJob: Job? = null
    private var epgJob: Job? = null
    private var hasStartedPlayback = false

    init {
        // Automatically start playing last watched or first available channel
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(categories, contentRegion, pinnedGroups) { cats, region, pinned ->
                Triple(cats, region, pinned)
            }.collect { (cats, region, pinned) ->
                if (_selectedCategory.value == null && cats.isNotEmpty()) {
                    val groups = com.fastiptv.ui.components.CategoryGroupHelper.buildGroups(cats, region, pinned)
                    val firstPriorityCat = groups.firstOrNull()?.categories?.firstOrNull()?.category
                    _selectedCategory.value = firstPriorityCat ?: cats.first()
                }
            }
        }

        viewModelScope.launch {
            channels.collect { chList ->
                if (!hasStartedPlayback && chList.isNotEmpty()) {
                    val target = chList.first()
                    tuneToChannel(target)
                }
            }
        }
    }

    fun getPlayer(): Player = streamPlayer.getPlayer()

    fun tuneToChannel(channel: Channel) {
        hasStartedPlayback = true
        _currentChannel.value = channel
        _switchNotice.value = "CH ${channel.id} • ${channel.name}"

        val format = sessionManager.getCachedStreamFormat()
        streamPlayer.playLiveStream(channel.id, format, channel.name)

        // Record recent
        viewModelScope.launch {
            repository.recordRecent(
                RecentItem(
                    streamId = channel.id,
                    type = "live",
                    title = channel.name,
                    iconUrl = channel.logoUrl,
                    lastWatched = System.currentTimeMillis()
                )
            )
        }

        fetchEpg(channel.id)
        showBanner(3500L)
    }

    fun nextChannel() {
        val list = channels.value
        if (list.isEmpty()) return
        val currentId = _currentChannel.value?.id
        val currentIndex = list.indexOfFirst { it.id == currentId }
        val nextIndex = if (currentIndex != -1 && currentIndex + 1 < list.size) {
            currentIndex + 1
        } else {
            0
        }
        val next = list[nextIndex]
        tuneToChannel(next)
    }

    fun previousChannel() {
        val list = channels.value
        if (list.isEmpty()) return
        val currentId = _currentChannel.value?.id
        val currentIndex = list.indexOfFirst { it.id == currentId }
        val prevIndex = if (currentIndex > 0) {
            currentIndex - 1
        } else {
            list.size - 1
        }
        val prev = list[prevIndex]
        tuneToChannel(prev)
    }

    fun selectLanguageGroup(group: String) {
        _selectedLanguageGroup.value = group
        val cats = categories.value

        val matchedCat = when (group) {
            "HINDI" -> cats.find { it.name.contains("HINDI", ignoreCase = true) }
            "GUJARATI" -> cats.find { it.name.contains("GUJARATI", ignoreCase = true) }
            "PUNJABI" -> cats.find { it.name.contains("PUNJABI", ignoreCase = true) }
            "SPORTS" -> cats.find { it.name.contains("SPORT", ignoreCase = true) || it.name.contains("CRICKET", ignoreCase = true) }
            "NEWS" -> cats.find { it.name.contains("NEWS", ignoreCase = true) || it.name.contains("SAMACHAAR", ignoreCase = true) }
            "DEVOTIONAL" -> cats.find { it.name.contains("BHAKTI", ignoreCase = true) || it.name.contains("DEVOTIONAL", ignoreCase = true) || it.name.contains("RELIGIOUS", ignoreCase = true) }
            else -> cats.firstOrNull()
        }

        if (matchedCat != null) {
            _selectedCategory.value = matchedCat
        }
    }

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun togglePinGroup(groupName: String) {
        viewModelScope.launch {
            sessionManager.togglePinGroup(groupName)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleBanner() {
        if (_isBannerVisible.value) {
            _isBannerVisible.value = false
            bannerHideJob?.cancel()
        } else {
            showBanner(4000L)
        }
    }

    fun showBanner(durationMs: Long = 3500L) {
        _isBannerVisible.value = true
        bannerHideJob?.cancel()
        bannerHideJob = viewModelScope.launch {
            delay(durationMs)
            _isBannerVisible.value = false
        }
    }

    fun openDrawer() {
        _isDrawerOpen.value = true
        _isBannerVisible.value = false
        bannerHideJob?.cancel()
    }

    fun closeDrawer() {
        _isDrawerOpen.value = false
        showBanner(3000L)
    }

    private fun fetchEpg(streamId: Int) {
        epgJob?.cancel()
        epgJob = viewModelScope.launch {
            repository.getShortEpg(streamId).onSuccess { list ->
                _currentEpgProgram.value = list.firstOrNull { it.isNowPlaying } ?: list.firstOrNull()
            }.onFailure {
                _currentEpgProgram.value = null
            }
        }
    }

    fun stopPlayer() {
        streamPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        bannerHideJob?.cancel()
        epgJob?.cancel()
        streamPlayer.stop()
    }
}
