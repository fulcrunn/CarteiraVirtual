package br.com.carteiravirtual.model

data class Wallet(
    var real: Double = 100000.0,      // 100 mil reais
    var dolar: Double = 50000.0,       // 50 mil dólares
    var bitcoin: Double = 0.5          // 0.5 BTC
)
