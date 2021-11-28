package com.nisaefendioglu.newsapp.data.repository
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.nisaefendioglu.newsapp.data.model.resourse.Article
import com.nisaefendioglu.newsapp.di.NetworkModule.FIRST_PAGE
import com.nisaefendioglu.newsapp.di.NetworkModule.ITEM_PER_PAGE
import com.nisaefendioglu.newsapp.data.remote.ApiService
import com.nisaefendioglu.newsapp.util.NetworkState


const val DEFAULT_SEARCH_TAG = "android"

class ArticleDataSource(
    private val apiServiceService: ApiService,
    private val compositeDisposable: CompositeDisposable
) : PageKeyedDataSource<Int, Article>() {

    private var page = FIRST_PAGE
    private var TAG = ArticleDataSource::class.java.simpleName

    val networkState: MutableLiveData<NetworkState> = MutableLiveData()

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Article>
    ) {

        networkState.postValue(NetworkState.LOADING)

        compositeDisposable.add(fetchNews(page, callback))
    }

    private fun fetchNews(
        page: Int,
        callback: LoadInitialCallback<Int, Article>
    ): Disposable {
        return apiServiceService.fetchNews(page)
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    callback.onResult(it.articles, null, page + 1)
                    networkState.postValue(NetworkState.LOADED)
                },
                {
                    networkState.postValue(NetworkState.ERROR)
                }
            )
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Article>) {
        networkState.postValue(NetworkState.LOADING)
        compositeDisposable.add(fetchMoreNews(params.key, callback))
    }

    private fun fetchMoreNews(
        page: Int,
        callback: LoadCallback<Int, Article>
    ): Disposable {
        return apiServiceService.fetchNews(page)
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    if ((it.totalResults / ITEM_PER_PAGE) >= page) {
                        callback.onResult(it.articles, page + 1)
                        networkState.postValue(NetworkState.LOADED)
                    } else {
                        networkState.postValue(NetworkState.ENDOFLIST)
                    }
                },
                {
                    networkState.postValue(NetworkState.ERROR)
                }
            )
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Article>) {

    }
}