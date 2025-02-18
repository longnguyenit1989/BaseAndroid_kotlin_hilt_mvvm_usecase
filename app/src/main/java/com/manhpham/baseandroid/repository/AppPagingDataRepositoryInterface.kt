package com.manhpham.baseandroid.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.rxjava3.observable
import com.manhpham.baseandroid.models.PagingUserResponse
import com.manhpham.baseandroid.ui.home.HomePagingSource
import com.manhpham.baseandroid.usecase.PagingDataSortType
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

interface AppPagingDataRepositoryInterface {
    fun getPagingData(type: PagingDataSortType): Observable<PagingData<PagingUserResponse>>
}

class AppPagingDataRepository @Inject constructor(
    private val appRemoteDataRefreshableRepositoryInterface: AppRemoteDataRefreshableRepositoryInterface
) : AppPagingDataRepositoryInterface {
    override fun getPagingData(type: PagingDataSortType): Observable<PagingData<PagingUserResponse>> {
        return Pager(
            config = PagingConfig(20, 10),
            pagingSourceFactory = {
                HomePagingSource(appRemoteDataRefreshableRepositoryInterface, type)
            }
        ).observable
    }
}
