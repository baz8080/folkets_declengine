package com.mbcdev.folkets.declension;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

import java.util.List;

/**
 * A retrofit service for Saldo web services
 *
 * Created by barry on 12/03/2017.
 */
public interface SaldoService {

    @GET("ws/saldo-ws/gen/json/{paradigm}/_")
    Call<List<SaldoNounDeclension>> getNounDeclensions(@Path("paradigm") String paradigm);
}
