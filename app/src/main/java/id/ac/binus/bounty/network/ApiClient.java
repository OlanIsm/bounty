package id.ac.binus.bounty.network;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private ApiClient() { }
    private static final RandomUserApi API = new Retrofit.Builder()
            .baseUrl("https://randomuser.me/")
            .client(new OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(RandomUserApi.class);

    public static RandomUserApi getApi() { return API; }
}
