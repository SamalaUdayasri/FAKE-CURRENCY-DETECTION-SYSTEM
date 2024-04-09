package com.fake.currency.app.Retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {

    private static Retrofit retrofitclient = null;
    public static Retrofit getClient()
    {
        if(retrofitclient==null)
        {
            retrofitclient = new Retrofit.Builder()
                    .baseUrl("http://16.171.44.237")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofitclient;
    }
}
