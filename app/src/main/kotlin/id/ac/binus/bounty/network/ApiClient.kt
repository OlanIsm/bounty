package id.ac.binus.bounty.network

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val api = Retrofit.Builder()
        .baseUrl("https://randomuser.me/")
        .client(OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(RandomUserApi::class.java)

    @JvmStatic fun getApi(): RandomUserApi = api
}
