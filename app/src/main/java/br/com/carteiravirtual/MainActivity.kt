package br.com.carteiravirtual

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.carteiravirtual.model.Wallet
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvReal: TextView
    private lateinit var tvDolar: TextView
    private lateinit var tvBitcoin: TextView
    private lateinit var btnConverter: Button

    private val wallet = Wallet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        tvReal = findViewById(R.id.tvReal)
        tvDolar = findViewById(R.id.tvDolar)
        tvBitcoin = findViewById(R.id.tvBitcoin)
        btnConverter = findViewById(R.id.btnConverter)

        // Atualizar display inicial
        updateDisplay()

        // Listener do botão
        btnConverter.setOnClickListener {
            val intent = Intent(this, ConverterActivity::class.java)
            intent.putExtra("real", wallet.real)
            intent.putExtra("dolar", wallet.dolar)
            intent.putExtra("bitcoin", wallet.bitcoin)
            startActivityForResult(intent, REQUEST_CONVERTER)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CONVERTER && resultCode == RESULT_OK && data != null) {
            // Atualizar wallet com os valores retornados
            wallet.real = data.getDoubleExtra("real", wallet.real)
            wallet.dolar = data.getDoubleExtra("dolar", wallet.dolar)
            wallet.bitcoin = data.getDoubleExtra("bitcoin", wallet.bitcoin)

            // Atualizar display
            updateDisplay()
        }
    }

    companion object {
        private const val REQUEST_CONVERTER = 1
    }

    private fun updateDisplay() {
        val dfMoney = DecimalFormat("#,##0.00")
        val dfBitcoin = DecimalFormat("0.0000")

        tvReal.text = "R$ ${dfMoney.format(wallet.real)}"
        tvDolar.text = "$ ${dfMoney.format(wallet.dolar)}"
        tvBitcoin.text = "${dfBitcoin.format(wallet.bitcoin)} BTC"
    }
}