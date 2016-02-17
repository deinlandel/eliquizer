package com.deinlandel.eliquizer;

import com.deinlandel.eliquizer.entity.RecipeFlavor;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.List;

/**
 * @author Deinlandel
 */
public interface RestApi {
    @FormUrlEncoded
    @POST("recipe/getFlavors/")
    Call<List<RecipeFlavor>> getFlavors(@Field("id") String id);
}
