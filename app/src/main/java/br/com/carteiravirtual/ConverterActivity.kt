package br.com.carteiravirtual

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import br.com.carteiravirtual.api.RetrofitClient
import br.com.carteiravirtual.model.Wallet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat

class ConverterActivity : AppCompatActivity() {

    private lateinit var btnVoltar: Button
    private lateinit var spinnerOrigem: Spinner
    private lateinit var spinnerDestino: Spinner
    private lateinit var etValor: EditText
    private lateinit var tvResultado: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnConsultarCotacao: Button
    private lateinit var btnConfirmarCompra: Button

    private lateinit var wallet: Wallet
    private val moedas = arrayOf("Real (BRL)", "Dólar (USD)", "Bitcoin (BTC)")

    // Armazena os dados da última cotação consultada
    private var ultimaCotacao: DadosCotacao? = null

    data class DadosCotacao(
        val origem: Int,
        val destino: Int,
        val valorOrigem: Double,
        val valorDestino: Double,
        val taxa: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter)


        btnVoltar = findViewById(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            finish()  // Fecha a activity e volta para a MainActivity
        }

        // Receber wallet da MainActivity
        wallet = Wallet(
            real = intent.getDoubleExtra("real", 100000.0),
            dolar = intent.getDoubleExtra("dolar", 50000.0),
            bitcoin = intent.getDoubleExtra("bitcoin", 0.5)
        )

        // Inicializar views
        spinnerOrigem = findViewById(R.id.spinnerOrigem)
        spinnerDestino = findViewById(R.id.spinnerDestino)
        etValor = findViewById(R.id.etValor)
        tvResultado = findViewById(R.id.tvResultado)
        progressBar = findViewById(R.id.progressBar)
        btnConsultarCotacao = findViewById(R.id.btnConsultarCotacao)
        btnConfirmarCompra = findViewById(R.id.btnConfirmarCompra)

        // Configurar spinners
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moedas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrigem.adapter = adapter
        spinnerDestino.adapter = adapter

        // Definir seleção inicial diferente
        spinnerOrigem.setSelection(0)  // Real
        spinnerDestino.setSelection(1) // Dólar

        // Listeners dos botões
        btnConsultarCotacao.setOnClickListener {
            consultarCotacao()
        }

        btnConfirmarCompra.setOnClickListener {
            confirmarCompra()
        }
    }

    private fun consultarCotacao() {
        // Validar entrada
        val valorStr = etValor.text.toString()
        if (valorStr.isEmpty()) {
            Toast.makeText(this, "Digite um valor para converter", Toast.LENGTH_SHORT).show()
            return
        }

        val valor = valorStr.toDoubleOrNull()
        if (valor == null || valor <= 0) {
            Toast.makeText(this, "Valor inválido", Toast.LENGTH_SHORT).show()
            return
        }

        val moedaOrigem = spinnerOrigem.selectedItemPosition
        val moedaDestino = spinnerDestino.selectedItemPosition

        // Validar moedas diferentes
        if (moedaOrigem == moedaDestino) {
            Toast.makeText(this, "Selecione moedas diferentes", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar saldo suficiente
        if (!verificarSaldo(moedaOrigem, valor)) {
            Toast.makeText(this, "Saldo insuficiente na moeda origem", Toast.LENGTH_LONG).show()
            return
        }

        // Consultar cotação na API
        buscarCotacao(moedaOrigem, moedaDestino, valor)
    }

    private fun verificarSaldo(moeda: Int, valor: Double): Boolean {
        return when (moeda) {
            0 -> wallet.real >= valor      // Real
            1 -> wallet.dolar >= valor     // Dólar
            2 -> wallet.bitcoin >= valor   // Bitcoin
            else -> false
        }
    }

    private fun buscarCotacao(origem: Int, destino: Int, valor: Double) {
        // Mostrar progress bar
        progressBar.visibility = View.VISIBLE
        tvResultado.visibility = View.GONE
        btnConfirmarCompra.visibility = View.GONE
        btnConsultarCotacao.isEnabled = false

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val taxaConversao = withContext(Dispatchers.IO) {
                    obterTaxaConversao(origem, destino)
                }

                if (taxaConversao != null) {
                    val valorConvertido = valor * taxaConversao

                    // Armazenar dados da cotação
                    ultimaCotacao = DadosCotacao(
                        origem = origem,
                        destino = destino,
                        valorOrigem = valor,
                        valorDestino = valorConvertido,
                        taxa = taxaConversao
                    )

                    // Mostrar resultado da cotação
                    exibirCotacao(origem, destino, valor, valorConvertido, taxaConversao)

                    // Habilitar botão de confirmar compra
                    btnConfirmarCompra.isEnabled = true
                    btnConfirmarCompra.visibility = View.VISIBLE

                } else {
                    Toast.makeText(
                        this@ConverterActivity,
                        "Erro ao obter cotação. Tente novamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@ConverterActivity,
                    "Erro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressBar.visibility = View.GONE
                btnConsultarCotacao.isEnabled = true
            }
        }
    }

    private fun confirmarCompra() {
        val cotacao = ultimaCotacao

        if (cotacao == null) {
            Toast.makeText(this, "Consulte a cotação primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        btnConfirmarCompra.isEnabled = false


        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // simula um delay de 1.5 segundos
                delay(2000)

                // Verificar saldo novamente (segurança)
                if (!verificarSaldo(cotacao.origem, cotacao.valorOrigem)) {
                    Toast.makeText(this@ConverterActivity, "Saldo insuficiente", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Atualizar wallet
                atualizarWallet(
                    cotacao.origem,
                    cotacao.destino,
                    cotacao.valorOrigem,
                    cotacao.valorDestino
                )

                // Mostrar confirmação
                exibirConfirmacao(
                    cotacao.origem,
                    cotacao.destino,
                    cotacao.valorOrigem,
                    cotacao.valorDestino
                )

                // Retornar wallet atualizada para MainActivity
                setResult(RESULT_OK, intent.apply {
                    putExtra("real", wallet.real)
                    putExtra("dolar", wallet.dolar)
                    putExtra("bitcoin", wallet.bitcoin)
                })

                // Limpar campos
                etValor.setText("")
                ultimaCotacao = null
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun obterTaxaConversao(origem: Int, destino: Int): Double? {
        return try {
            when {
                // Real -> Dólar
                origem == 0 && destino == 1 -> {
                    val response = RetrofitClient.api.getUsdBrl()
                    1.0 / response.usdBrl?.bid?.toDouble()!!
                }
                // Real -> Bitcoin
                origem == 0 && destino == 2 -> {
                    val response = RetrofitClient.api.getBtcBrl()
                    1.0 / response.btcBrl?.bid?.toDouble()!!
                }
                // Dólar -> Real
                origem == 1 && destino == 0 -> {
                    val response = RetrofitClient.api.getUsdBrl()
                    response.usdBrl?.bid?.toDouble()
                }
                // Dólar -> Bitcoin (conversão via Real)
                origem == 1 && destino == 2 -> {
                    // Dólar -> Real
                    val responseUsdBrl = RetrofitClient.api.getUsdBrl()
                    val dolarParaReal = responseUsdBrl.usdBrl?.bid?.toDouble()!!

                    // Real -> Bitcoin
                    val responseBtcBrl = RetrofitClient.api.getBtcBrl()
                    val realParaBitcoin = 1.0 / responseBtcBrl.btcBrl?.bid?.toDouble()!!

                    // Dólar -> Real -> Bitcoin
                    dolarParaReal * realParaBitcoin
                }
                // Bitcoin -> Real
                origem == 2 && destino == 0 -> {
                    val response = RetrofitClient.api.getBtcBrl()
                    response.btcBrl?.bid?.toDouble()
                }
                // Bitcoin -> Dólar (conversão via Real)
                origem == 2 && destino == 1 -> {
                    // Bitcoin -> Real
                    val responseBtcBrl = RetrofitClient.api.getBtcBrl()
                    val bitcoinParaReal = responseBtcBrl.btcBrl?.bid?.toDouble()!!

                    // Real -> Dólar
                    val responseUsdBrl = RetrofitClient.api.getUsdBrl()
                    val realParaDolar = 1.0 / responseUsdBrl.usdBrl?.bid?.toDouble()!!

                    // Bitcoin -> Real -> Dólar
                    bitcoinParaReal * realParaDolar
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun atualizarWallet(origem: Int, destino: Int, valorOrigem: Double, valorDestino: Double) {
        // Debitar da origem
        when (origem) {
            0 -> wallet.real -= valorOrigem
            1 -> wallet.dolar -= valorOrigem
            2 -> wallet.bitcoin -= valorOrigem
        }

        // Creditar no destino
        when (destino) {
            0 -> wallet.real += valorDestino
            1 -> wallet.dolar += valorDestino
            2 -> wallet.bitcoin += valorDestino
        }
    }

    private fun exibirCotacao(origem: Int, destino: Int, valorOrigem: Double, valorDestino: Double, taxa: Double) {
        val dfMoney = DecimalFormat("#,##0.00")
        val dfBitcoin = DecimalFormat("0.0000")
        val dfTaxa = DecimalFormat("#,##0.0000")

        val nomeOrigem = moedas[origem].substringBefore(" ")
        val nomeDestino = moedas[destino].substringBefore(" ")

        val valorOrigemFormatado = if (origem == 2) dfBitcoin.format(valorOrigem) else dfMoney.format(valorOrigem)
        val valorDestinoFormatado = if (destino == 2) dfBitcoin.format(valorDestino) else dfMoney.format(valorDestino)

        val simboloOrigem = when(origem) {
            0 -> "R$ "
            1 -> "$ "
            2 -> ""
            else -> ""
        }

        val simboloDestino = when(destino) {
            0 -> "R$ "
            1 -> "$ "
            2 -> ""
            else -> ""
        }

        val sufixoOrigem = if (origem == 2) " BTC" else ""
        val sufixoDestino = if (destino == 2) " BTC" else ""

        tvResultado.text = "📊 COTAÇÃO CONSULTADA\n\n" +
                "Você vai converter:\n" +
                "$simboloOrigem$valorOrigemFormatado$sufixoOrigem ($nomeOrigem)\n\n" +
                "Você vai receber:\n" +
                "$simboloDestino$valorDestinoFormatado$sufixoDestino ($nomeDestino)\n\n" +
                "Taxa: ${dfTaxa.format(taxa)}"

        tvResultado.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
        tvResultado.visibility = View.VISIBLE
    }

    private fun exibirConfirmacao(origem: Int, destino: Int, valorOrigem: Double, valorDestino: Double) {
        val dfMoney = DecimalFormat("#,##0.00")
        val dfBitcoin = DecimalFormat("0.0000")

        val nomeOrigem = moedas[origem].substringBefore(" ")
        val nomeDestino = moedas[destino].substringBefore(" ")

        val valorOrigemFormatado = if (origem == 2) dfBitcoin.format(valorOrigem) else dfMoney.format(valorOrigem)
        val valorDestinoFormatado = if (destino == 2) dfBitcoin.format(valorDestino) else dfMoney.format(valorDestino)

        val simboloOrigem = when(origem) {
            0 -> "R$ "
            1 -> "$ "
            2 -> ""
            else -> ""
        }

        val simboloDestino = when(destino) {
            0 -> "R$ "
            1 -> "$ "
            2 -> ""
            else -> ""
        }

        val sufixoOrigem = if (origem == 2) " BTC" else ""
        val sufixoDestino = if (destino == 2) " BTC" else ""

        tvResultado.text = "✅ COMPRA CONFIRMADA!\n\n" +
                "Convertido:\n" +
                "$simboloOrigem$valorOrigemFormatado$sufixoOrigem ($nomeOrigem)\n\n" +
                "Recebido:\n" +
                "$simboloDestino$valorDestinoFormatado$sufixoDestino ($nomeDestino)\n\n" +
                "Transação efetivada com sucesso!"

        tvResultado.setTextColor(resources.getColor(android.R.color.holo_green_dark))
        tvResultado.visibility = View.VISIBLE

        btnConfirmarCompra.visibility = View.GONE

        Toast.makeText(this, "Conversão realizada com sucesso!", Toast.LENGTH_LONG).show()
    }
}