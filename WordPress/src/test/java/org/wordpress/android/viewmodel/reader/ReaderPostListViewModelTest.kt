package org.wordpress.android.viewmodel.reader

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel

@RunWith(MockitoJUnitRunner::class)
class ReaderPostListViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var newsManager: NewsManager
    @Mock private lateinit var observer: Observer<NewsItem>
    @Mock private lateinit var item: NewsItem
    @Mock private lateinit var initialTag: ReaderTag
    @Mock private lateinit var otherTag: ReaderTag
    @Mock private lateinit var newsTracker: NewsTracker
    @Mock private lateinit var newsTrackerHelper: NewsTrackerHelper

    private lateinit var viewModel: ReaderPostListViewModel
    private val liveData = MutableLiveData<NewsItem>()

    @Before
    fun setUp() {
        whenever(newsManager.newsItemSource()).thenReturn(liveData)
        viewModel = ReaderPostListViewModel(newsManager, newsTracker, newsTrackerHelper)
        val observable = viewModel.getNewsDataSource()
        observable.observeForever(observer)
    }

    @Test
    fun verifyPullInvokedInOnStart() {
        viewModel.start(initialTag)
        verify(newsManager).pull(false)
    }

    @Test
    fun verifyViewModelPropagatesNewsItems() {
        viewModel.start(initialTag)
        liveData.postValue(item)
        liveData.postValue(null)
        liveData.postValue(item)

        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun verifyViewModelPropagatesDismissToNewsManager() {
        viewModel.onNewsCardDismissed(item)
        verify(newsManager).dismiss(item)
    }

    @Test
    fun emitNullOnInitialTagChanged() {
        viewModel.start(initialTag)
        liveData.postValue(item)
        viewModel.onTagChanged(otherTag)
        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
    }

    @Test
    fun verifyNewsItemAvailableOnlyForInitialReaderTag() {
        viewModel.start(initialTag)
        liveData.postValue(item)
        viewModel.onTagChanged(otherTag)
        liveData.postValue(item) // should not be propagated to the UI
        liveData.postValue(item) // should not be propagated to the UI
        liveData.postValue(item) // should not be propagated to the UI
        liveData.postValue(item) // should not be propagated to the UI
        viewModel.onTagChanged(initialTag)

        val inOrder = inOrder(observer)
        inOrder.verify(observer).onChanged(item)
        inOrder.verify(observer).onChanged(null)
        inOrder.verify(observer).onChanged(item)
    }

    @Test
    fun verifyViewModelPropagatesDismissToNewsTracker() {
        viewModel.onNewsCardDismissed(item)
        verify(newsTracker).trackNewsCardDismissed(argThat { this == READER }, any())
    }

    @Test
    fun verifyViewModelPropagatesCardShownToNewsTracker() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(true)
        viewModel.onNewsCardShown(item)
        verify(newsTracker).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper).itemTracked(any())
    }

    @Test
    fun verifyViewModelDoesNotPropagatesCardShownToNewsTracker() {
        whenever(newsTrackerHelper.shouldTrackNewsCardShown(any())).thenReturn(false)
        viewModel.onNewsCardShown(item)
        verify(newsTracker, times(0)).trackNewsCardShown(argThat { this == READER }, any())
        verify(newsTrackerHelper, times(0)).itemTracked(any())
    }

    @Test
    fun verifyViewModelPropagatesExtendedInfoRequestedToNewsTracker() {
        viewModel.onNewsCardExtendedInfoRequested(item)
        verify(newsTracker).trackNewsCardExtendedInfoRequested(argThat { this == READER }, any())
    }
}
