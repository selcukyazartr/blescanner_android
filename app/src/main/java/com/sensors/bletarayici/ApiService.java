package com.sensors.bletarayici;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    String ENDPOINT = "http://IP_ADRESI/PATH/";
    @Headers("Content-Type: application/json")
    @POST("CRUD_METHOD_ADI")
    Call<myBlueToothDevice> getMyJSON(@Body String sonuc);

}
