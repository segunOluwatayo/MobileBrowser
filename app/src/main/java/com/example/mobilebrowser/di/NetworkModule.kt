package com.example.mobilebrowser.di

import com.example.mobilebrowser.api.BookmarkApiService
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.data.util.UserDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(userDataStore: UserDataStore): Interceptor {
        return Interceptor { chain ->
            val accessToken = runBlocking { userDataStore.accessToken.first() }
            val request = if (accessToken.isNotBlank()) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://nimbus-browser-backend-production.up.railway.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHistoryApiService(retrofit: Retrofit): HistoryApiService {
        return retrofit.create(HistoryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBookmarkApiService(retrofit: Retrofit): BookmarkApiService {
        return retrofit.create(BookmarkApiService::class.java)
    }

}