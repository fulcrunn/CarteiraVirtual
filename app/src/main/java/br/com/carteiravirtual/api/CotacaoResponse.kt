package br.com.carteiravirtual.api

import com.google.gson.annotations.SerializedName

data class CotacaoResponse(
    @SerializedName("USDBRL")
    val usdBrl: Cotacao?,

    @SerializedName("BTCBRL")
    val btcBrl: Cotacao?,

    @SerializedName("USDBTC")
    val usdBtc: Cotacao?
)

data class Cotacao(
    @SerializedName("bid")
    val bid: String,  // Valor de compra

    @SerializedName("ask")
    val ask: String   // Valor de venda
)
