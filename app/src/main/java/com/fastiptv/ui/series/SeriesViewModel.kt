package com.fastiptv.ui.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastiptv.domain.model.Category
import com.fastiptv.domain.model.Series
import com.fastiptv.domain.model.SeriesDetail
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
class SeriesViewModel @Inject constructor(
    private val repository: IptvRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = repository.observeSeriesCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    val seriesList: StateFlow<List<Series>> = _selectedCategory
        .flatMapLatest { cat ->
            if (cat != null) {
                repository.observeSeriesByCategory(cat.id)
            } else {
                flowOf(emptyList<Series>())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries.asStateFlow()

    private val _seriesDetail = MutableStateFlow<SeriesDetail?>(null)
    val seriesDetail: StateFlow<SeriesDetail?> = _seriesDetail.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    init {
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

    fun openSeriesDetail(series: Series) {
        _selectedSeries.value = series
        viewModelScope.launch {
            _isLoadingDetail.value = true
            _seriesDetail.value = null
            repository.getSeriesInfo(series.id)
                .onSuccess { detail ->
                    _seriesDetail.value = detail
                }
                .onFailure {
                    _seriesDetail.value = null
                }
            _isLoadingDetail.value = false
        }
    }

    fun closeSeriesDetail() {
        _selectedSeries.value = null
        _seriesDetail.value = null
    }
}
