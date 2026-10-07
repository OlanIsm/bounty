package id.ac.binus.bounty.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface RandomUserApi {
    @GET("api/")
    Call<RandomUserResponse> getUsers(@Query("results") int results);
}
