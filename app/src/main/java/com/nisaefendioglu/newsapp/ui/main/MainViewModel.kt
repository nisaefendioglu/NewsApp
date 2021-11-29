package com.nisaefendioglu.newsapp.ui.main
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nisaefendioglu.newsapp.data.model.NewsArticle
import com.nisaefendioglu.newsapp.data.model.NewsResponse
import com.nisaefendioglu.newsapp.di.CoroutinesDispatcherProvider
import com.nisaefendioglu.newsapp.network.repository.NewsRepository
import com.nisaefendioglu.newsapp.utils.Constants
import com.nisaefendioglu.newsapp.utils.NetworkHelper
import com.nisaefendioglu.newsapp.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

//görünüm, internet kontrolleri.

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NewsRepository,
    private val networkHelper: NetworkHelper,
    private val coroutinesDispatcherProvider: CoroutinesDispatcherProvider
) : ViewModel() {

    private val TAG = "MainViewModel"
    private val _errorToast = MutableLiveData<String>()
    val errorToast: LiveData<String>
        get() = _errorToast

    private val _newsResponse = MutableLiveData<NetworkResult<NewsResponse>>()
    val newsResponse: LiveData<NetworkResult<NewsResponse>>
        get() = _newsResponse

    private var feedResponse: NewsResponse? = null
    var feedNewsPage = 1

    var searchNewsPage = 1
    var searchResponse: NewsResponse? = null
    private var oldQuery: String = ""
    var newQuery: String = ""
    var totalPage = 1

    private val _isSearchActivated = MutableLiveData<Boolean>()
    val isSearchActivated: LiveData<Boolean>
        get() = _isSearchActivated

    init {
        fetchNews(Constants.CountryCode)
    }

    fun fetchNews(countryCode: String) {
        if (feedNewsPage <= totalPage) {
            if (networkHelper.isNetworkConnected()) {
                _newsResponse.postValue(NetworkResult.Loading())

                val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
                    onError(exception)
                }
                viewModelScope.launch(coroutinesDispatcherProvider.io + coroutineExceptionHandler) {
                    when (val response = repository.getNews(countryCode, feedNewsPage)) {
                        is NetworkResult.Success -> {
                            _newsResponse.postValue(handleFeedNewsResponse(response))
                        }
                        is NetworkResult.Error -> {
                            _newsResponse.postValue(
                                NetworkResult.Error(
                                    response.message ?: "Error"
                                )
                            )
                        }
                    }

                }
            } else {
                _errorToast.value = "No internet"
            }
        }
    }

    private fun handleFeedNewsResponse(response: NetworkResult<NewsResponse>): NetworkResult<NewsResponse> {
        response.data?.let { resultResponse ->
            if (feedResponse == null) {
                feedNewsPage = 2
                feedResponse = resultResponse
            } else {
                feedNewsPage++
                val oldArticles = feedResponse?.articles
                val newArticles = resultResponse.articles
                oldArticles?.addAll(newArticles)
            }
            feedResponse?.let {
                feedResponse = convertPublishedDate(it)
            }
            return NetworkResult.Success(feedResponse ?: resultResponse)
        }
        return NetworkResult.Error("No data found")
    }

      fun convertPublishedDate(currentResponse: NewsResponse): NewsResponse {
        currentResponse.let { response ->
            for (i in 0 until response.articles.size) {
                val publishedAt = response.articles[i].publishedAt
                publishedAt?.let {
                    val converted = formatDate(it)
                    response.articles[i].publishedAt = converted
                }
            }
        }
        return currentResponse
    }

    fun formatDate(strCurrentDate: String): String {
        var convertedDate = ""
        try {
            if (strCurrentDate.isNotEmpty() && strCurrentDate.contains("T")) {
                val local = Locale("US")
                var format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", local)
                val newDate: Date? = format.parse(strCurrentDate)

                format = SimpleDateFormat("MMM dd, yyyy hh:mm a", local)
                newDate?.let {
                    convertedDate = format.format(it)
                }
            } else {
                convertedDate = strCurrentDate
            }
        } catch (e: Exception) {
            e.message?.let {
                Log.e(TAG, it)
            }
            convertedDate = strCurrentDate
        }
        return convertedDate
    }

    fun hideErrorToast() {
        _errorToast.value = ""
    }

    fun saveNews(news: NewsArticle) {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            onError(exception)
        }
        viewModelScope.launch(coroutinesDispatcherProvider.io + coroutineExceptionHandler) {
            repository.saveNews(news)
        }
    }

    fun getFavoriteNews() = repository.getSavedNews()

    fun deleteNews(news: NewsArticle) {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            onError(exception)
        }
        viewModelScope.launch(coroutinesDispatcherProvider.io + coroutineExceptionHandler) {
            repository.deleteNews(news)
        }
    }

    private fun onError(throwable: Throwable) {
        _errorToast.value = throwable.message
    }
}