package com.example.mvmrapp;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("loadXML/getXMLFiles")
    Call<ResponseBody> uploadXMLFiles(
            @Part("flag_nmap") RequestBody flag_nmap,
            @Part("flag_nessus") RequestBody flag_nessus,
            @Part("flag_openvas") RequestBody flag_openvas,
            @Part("flag_owaspzapzap") RequestBody flag_owaspzapzap,
            @Part MultipartBody.Part nmap,
            @Part MultipartBody.Part nessus,
            @Part MultipartBody.Part openvas,
            @Part MultipartBody.Part owaspzapzap
    );
}