package com.example.baseandroid.networking

import android.util.Log
import com.example.baseandroid.data.remote.ApiClient
import com.example.baseandroid.repository.AppLocalDataRepositoryInterface
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import kotlin.concurrent.withLock

enum class RefreshTokenState {
    NOT_NEED_REFRESH,
    IS_REFRESHING,
    REFRESH_SUCCESS,
    REFRESH_ERROR
}

class RefreshTokenValidator {

    companion object {
        @Volatile private var INSTANCE: RefreshTokenValidator? = null
        fun getInstance(): RefreshTokenValidator =  INSTANCE ?: synchronized(this) {
            INSTANCE ?: RefreshTokenValidator().also { INSTANCE = it }
        }
    }

    var refreshTokenState: RefreshTokenState = RefreshTokenState.NOT_NEED_REFRESH
    var lastFailedDate: Long? = null

}

class RefreshTokenAuthenticator @Inject constructor(private val appLocalDataRepositoryInterface: AppLocalDataRepositoryInterface, private val apiClient: ApiClient): Authenticator {

    init {
        Timber.d(this.hashCode().toString())
    }

    private val lock: ReentrantLock = ReentrantLock(true)

    override fun authenticate(route: Route?, response: Response): Request? {
        lock.withLock {
            val isRefreshTokenRequest = response.request.url.toString().endsWith("refreshToken")
            if (response.code == 401 && !isRefreshTokenRequest && checkRepeatRefreshToken()) {
                if (RefreshTokenValidator.getInstance().refreshTokenState != RefreshTokenState.IS_REFRESHING) {
                    RefreshTokenValidator.getInstance().refreshTokenState = RefreshTokenState.IS_REFRESHING
                    refreshToken()
                }
                return newRequest(response)
            }
            return null
        }
    }

    private fun checkRepeatRefreshToken(): Boolean {
        val timeDiff = System.currentTimeMillis() - (RefreshTokenValidator.getInstance().lastFailedDate ?: System.currentTimeMillis())
        return if (RefreshTokenValidator.getInstance().lastFailedDate != null) {
            timeDiff > 30000
        } else {
            true
        }
    }

    private fun newRequest(response: Response): Request? {
        when (RefreshTokenValidator.getInstance().refreshTokenState) {
            RefreshTokenState.IS_REFRESHING -> {
                Thread.sleep(1000)
                return newRequest(response)
            }
            RefreshTokenState.REFRESH_SUCCESS -> {
                val currentAccessToken = appLocalDataRepositoryInterface.getToken()
                return if (currentAccessToken.isNotEmpty()) {
                    response
                        .request
                        .newBuilder()
                        .apply {
                            if (currentAccessToken.isNotEmpty()) {
                                removeHeader("authorization")
                                addHeader("authorization", "Bearer $currentAccessToken")
                            }
                        }
                        .build()
                } else {
                    null
                }
            }
            else -> return null
        }
    }

    private fun refreshToken() {
        val refreshToken = appLocalDataRepositoryInterface.getRefreshToken()
        if (refreshToken.isNotEmpty()) {
            apiClient.refresh(refreshToken).execute().let {
                if (it.isSuccessful && it.code() == 200) {
                    appLocalDataRepositoryInterface.setToken(it.body()?.token ?: "")
                    RefreshTokenValidator.getInstance().refreshTokenState = RefreshTokenState.REFRESH_SUCCESS
                    RefreshTokenValidator.getInstance().lastFailedDate = null
                } else {
                    RefreshTokenValidator.getInstance().refreshTokenState = RefreshTokenState.REFRESH_ERROR
                    RefreshTokenValidator.getInstance().lastFailedDate = System.currentTimeMillis()
                }
            }
        } else {
            RefreshTokenValidator.getInstance().refreshTokenState = RefreshTokenState.REFRESH_ERROR
        }
    }

}