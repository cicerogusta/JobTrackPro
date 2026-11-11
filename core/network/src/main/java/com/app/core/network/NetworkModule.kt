package com.app.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun client(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).build()
    @Provides @Singleton
    fun jobsApi(client: OkHttpClient): JobsApi = Retrofit.Builder()
        .baseUrl("https://www.arbeitnow.com/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(JobsApi::class.java)
}
