package br.com.carteiravirtual.api

import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApi {

    // USD para BRL
    @GET("json/last/USD-BRL")
    suspend fun getUsdBrl(): CotacaoResponse

    // BTC para BRL
    @GET("json/last/BTC-BRL")
    suspend fun getBtcBrl(): CotacaoResponse
}