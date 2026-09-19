package com.fastiptv.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.domain.model.Channel
import com.fastiptv.domain.model.Movie
import com.fastiptv.domain.repository.IptvRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FavoriteTab {
    CHANNELS,
    MOVIES
}

data class FavoritesUiState(
    val selectedTab: FavoriteTab = FavoriteTab.CHANNELS,
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(FavoriteTab.CHANNELS)

    val uiState: StateFlow<FavoritesUiState> = combine(
        _selectedTab,
        repository.observeFavorites(),
        repository.observeFavoriteMovies()
    ) { tab, channels, movies ->
        FavoritesUiState(
            selectedTab = tab,
            channels = channels,
            movies = movies,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = FavoritesUiState(isLoading = true)
    )

    fun selectTab(tab: FavoriteTab) {
        _selectedTab.value = tab
    }

    fun toggleChannelFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun toggleMovieFavorite(movie: Movie) {
        viewModelScope.launch {
            repository.toggleFavoriteMovie(movie)
        }
    }
}
