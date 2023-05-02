package com.example.cz4171_project;

import android.media.Image;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;

import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface NetworkUtil {
    @Multipart
    @POST("/")
    Call<String> uploadImage(@Part MultipartBody.Part part);

}