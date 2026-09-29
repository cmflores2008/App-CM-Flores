package pt.encomendas.cmflores.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportacaoCobol {

    // Estrutura para agrupar a encomenda, o cliente e as linhas numa única "caixa"
    data class DadosExportacao(
        val encomenda: Encomenda,
        val cliente: Cliente,
        val linhas: List<LinhaEncomenda>
    )

    // ---------------------------------------------------------
    // FERRAMENTAS DE FORMATAÇÃO COBOL
    // ---------------------------------------------------------

    private fun formatarAlfanumerico(texto: String, tamanho: Int): String {
        val limpo = texto.replace("À", "A").replace("Á", "A").replace("Ç", "C")
            .replace("É", "E").replace("Í", "I").replace("Ó", "O").replace("Ú", "U")
            .replace("ã", "a").replace("õ", "o")
        return limpo.take(tamanho).padEnd(tamanho, ' ')
    }

    private fun formatarNumerico(numero: String, tamanho: Int): String {
        return numero.take(tamanho).padStart(tamanho, '0')
    }

    private fun formatarOverpunch(valor: Double, inteiros: Int, decimais: Int): String {
        val multiplicador = Math.pow(10.0, decimais.toDouble())
        val valorInteiro = Math.round(valor * multiplicador)
        val valorStr = Math.abs(valorInteiro).toString()

        val isNegativo = valorInteiro < 0
        val ultimoDigito = valorStr.last()

        val charCobol = if (isNegativo) {
            when (ultimoDigito) {
                '1' -> 'J'; '2' -> 'K'; '3' -> 'L'; '4' -> 'M'; '5' -> 'N'
                '6' -> 'O'; '7' -> 'P'; '8' -> 'Q'; '9' -> 'R'; '0' -> '}'
                else -> ultimoDigito
            }
        } else {
            when (ultimoDigito) {
                '1' -> 'A'; '2' -> 'B'; '3' -> 'C'; '4' -> 'D'; '5' -> 'E'
                '6' -> 'F'; '7' -> 'G'; '8' -> 'H'; '9' -> 'I'; '0' -> '{'
                else -> ultimoDigito
            }
        }

        val baseConvertida = if (valorStr.length > 1) valorStr.dropLast(1) + charCobol else charCobol.toString()
        return baseConvertida.padStart(inteiros + decimais, '0')
    }

    // ---------------------------------------------------------
    // EXPORTAÇÃO EM LOTE
    // ---------------------------------------------------------
    fun exportarEmLote(
        context: Context,
        listaExportacao: List<DadosExportacao>,
        artigos: List<Artigo>
    ) {
        if (listaExportacao.isEmpty()) return

        try {
            val sbCbt = StringBuilder()
            val sbLin = StringBuilder()
            val dataHoje = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val horaHoje = SimpleDateFormat("HHmmssSS", Locale.getDefault()).format(Date())

            // Processar cada encomenda da lista
            listaExportacao.forEach { dados ->
                val encomenda = dados.encomenda
                val cliente = dados.cliente
                val linhas = dados.linhas

                val dataLimpa = encomenda.dataRecolha.replace("/", "")
                val dataCobol = if (dataLimpa.length == 8) dataLimpa.substring(4, 8) + dataLimpa.substring(2, 4) + dataLimpa.substring(0, 2) else "00000000"

                // --- 1. ESCREVER LINHA DO CABEÇALHO (.CBT) ---
                sbCbt.append(formatarAlfanumerico("C", 1))
                sbCbt.append(formatarAlfanumerico("E", 1))
                sbCbt.append(formatarAlfanumerico(encomenda.numero, 10))
                sbCbt.append(formatarAlfanumerico(" ", 1))
                sbCbt.append(formatarAlfanumerico("", 5))
                sbCbt.append(formatarAlfanumerico("", 110))
                sbCbt.append(formatarAlfanumerico(cliente.codigo, 15))
                sbCbt.append(formatarAlfanumerico(cliente.nome, 50))
                sbCbt.append(formatarAlfanumerico(cliente.morada, 50))
                sbCbt.append(formatarAlfanumerico(cliente.localidade, 50))
                sbCbt.append(formatarAlfanumerico(cliente.codigoPostal, 10))
                sbCbt.append(formatarAlfanumerico(cliente.localidade, 25))
                sbCbt.append(formatarAlfanumerico(cliente.numContribuinte, 15))
                sbCbt.append(formatarAlfanumerico(cliente.codVendedor, 5))
                sbCbt.append(formatarAlfanumerico(cliente.vendedor, 40))
                sbCbt.append(formatarAlfanumerico(cliente.codPagamento, 5))
                sbCbt.append(formatarAlfanumerico(cliente.formaPagamento, 40))
                sbCbt.append(formatarAlfanumerico("", 50))
                sbCbt.append(formatarAlfanumerico("", 20))
                sbCbt.append(formatarAlfanumerico("", 5))
                sbCbt.append(formatarAlfanumerico("", 40))
                sbCbt.append(formatarAlfanumerico("", 50))
                sbCbt.append(formatarAlfanumerico("", 50))
                sbCbt.append(formatarAlfanumerico("", 10))
                sbCbt.append(formatarAlfanumerico("", 80))
                sbCbt.append(formatarAlfanumerico("", 80))
                sbCbt.append(formatarAlfanumerico("ANDROID", 12))
                sbCbt.append(formatarAlfanumerico(cliente.telemovel, 50))
                sbCbt.append(formatarAlfanumerico("", 1))
                sbCbt.append(formatarAlfanumerico("", 1))
                sbCbt.append(formatarAlfanumerico("", 15))
                sbCbt.append(formatarAlfanumerico("", 15))
                sbCbt.append(formatarAlfanumerico("", 1))
                sbCbt.append(formatarAlfanumerico("", 15))
                sbCbt.append(formatarAlfanumerico("PT", 5))
                sbCbt.append(formatarAlfanumerico("PRT", 3))
                sbCbt.append(formatarAlfanumerico("", 5))
                sbCbt.append(formatarAlfanumerico("", 200))
                sbCbt.append(formatarAlfanumerico("", 2))
                sbCbt.append(formatarAlfanumerico("", 126))

                sbCbt.append(formatarNumerico("", 80))
                sbCbt.append(formatarNumerico(dataHoje, 8))
                sbCbt.append(formatarNumerico(horaHoje, 8))
                sbCbt.append(formatarNumerico(dataCobol, 8))
                sbCbt.append(formatarNumerico(dataCobol, 8))
                sbCbt.append(formatarNumerico(dataCobol, 8))
                sbCbt.append(formatarNumerico("0", 4))
                sbCbt.append(formatarNumerico("0", 4))
                sbCbt.append(formatarNumerico("0", 1))
                sbCbt.append(formatarNumerico("0", 1))
                sbCbt.append(formatarNumerico("0", 1))
                sbCbt.append(formatarNumerico("0", 1))
                sbCbt.append(formatarNumerico("0", 8))
                sbCbt.append(formatarNumerico("0", 4))
                sbCbt.append(formatarNumerico("0", 4))

                for (i in 1..4) {
                    sbCbt.append(formatarNumerico("0", 4))
                    sbCbt.append(formatarOverpunch(0.0, 8, 2))
                    sbCbt.append(formatarOverpunch(0.0, 8, 2))
                }

                sbCbt.append(formatarNumerico("0", 4))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))

                sbCbt.append(formatarOverpunch(encomenda.total, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarOverpunch(encomenda.total, 8, 2))
                sbCbt.append(formatarOverpunch(encomenda.total, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarNumerico("0", 8))
                sbCbt.append(formatarOverpunch(0.0, 8, 2))
                sbCbt.append(formatarOverpunch(0.0, 6, 2))
                sbCbt.append(formatarOverpunch(0.0, 6, 2))
                sbCbt.append(formatarNumerico("0", 3))
                sbCbt.append(formatarOverpunch(0.0, 6, 2))
                sbCbt.append(formatarAlfanumerico("", 32))
                sbCbt.append("\n")

                // --- 2. ESCREVER AS LINHAS DA ENCOMENDA (.LIN) ---
                var contadorLinha = 1
                linhas.forEach { linha ->
                    val artigo = artigos.find { it.id == linha.artigoId }

                    sbLin.append(formatarNumerico(dataCobol, 8))
                    sbLin.append(formatarAlfanumerico("C", 1))
                    sbLin.append(formatarAlfanumerico("E", 1))
                    sbLin.append(formatarAlfanumerico(encomenda.numero, 10))
                    sbLin.append(formatarNumerico(contadorLinha.toString(), 3))

                    sbLin.append(formatarAlfanumerico("", 5))
                    sbLin.append(formatarAlfanumerico("", 1))
                    sbLin.append(formatarAlfanumerico("", 10))
                    sbLin.append(formatarAlfanumerico(cliente.codigo, 15)) // CODENT
                    sbLin.append(formatarAlfanumerico(artigo?.codigo ?: "DESCONH", 15))
                    sbLin.append(formatarAlfanumerico(artigo?.descricao ?: "", 65))
                    sbLin.append(formatarAlfanumerico("", 5))
                    sbLin.append(formatarAlfanumerico(linha.unidade, 5))
                    sbLin.append(formatarAlfanumerico("M", 1))
                    sbLin.append(formatarAlfanumerico("S", 1))
                    sbLin.append(formatarAlfanumerico("", 1))
                    sbLin.append(formatarAlfanumerico(artigo?.categoria ?: "", 5))
                    sbLin.append(formatarAlfanumerico(linha.observacao, 40))
                    sbLin.append(formatarAlfanumerico("", 5))
                    sbLin.append(formatarAlfanumerico("", 64))

                    sbLin.append(formatarNumerico("0", 8))
                    sbLin.append(formatarNumerico("0", 3))

                    sbLin.append(formatarOverpunch(1.0, 5, 4))
                    sbLin.append(formatarOverpunch(linha.quantidade, 5, 4))
                    sbLin.append(formatarOverpunch(linha.quantidade, 5, 4))
                    sbLin.append(formatarOverpunch(0.0, 5, 4))

                    sbLin.append(formatarOverpunch(linha.precoUnitario, 8, 4))
                    sbLin.append(formatarOverpunch(linha.precoUnitario, 8, 4))

                    sbLin.append(formatarOverpunch(0.0, 2, 2))
                    sbLin.append(formatarOverpunch(0.0, 8, 2))

                    sbLin.append(formatarNumerico("0", 1))
                    sbLin.append(formatarOverpunch(0.0, 2, 2))

                    sbLin.append(formatarOverpunch(linha.total, 8, 2))
                    sbLin.append(formatarOverpunch(linha.total, 8, 2))

                    sbLin.append(formatarOverpunch(0.0, 5, 4))
                    sbLin.append(formatarOverpunch(0.0, 6, 4))

                    sbLin.append(formatarOverpunch(linha.precoUnitario, 6, 4))
                    sbLin.append(formatarOverpunch(linha.total, 6, 4))

                    sbLin.append(formatarNumerico("0", 1))
                    sbLin.append(formatarNumerico("0", 4))
                    sbLin.append(formatarAlfanumerico("", 40))
                    sbLin.append("\n")
                    contadorLinha++
                }
            }

            // --- 3. CRIAR E PARTILHAR OS DOIS FICHEIROS ---
            val pastaPdfs = File(context.cacheDir, "cobol_exports")
            if (!pastaPdfs.exists()) pastaPdfs.mkdirs()

            val sufixo = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileCbt = File(pastaPdfs, "FDOC_$sufixo.CBT")
            val fileLin = File(pastaPdfs, "FDOC_$sufixo.LIN")

            val fosCbt = FileOutputStream(fileCbt)
            fosCbt.write(sbCbt.toString().toByteArray(Charsets.ISO_8859_1))
            fosCbt.close()

            val fosLin = FileOutputStream(fileLin)
            fosLin.write(sbLin.toString().toByteArray(Charsets.ISO_8859_1))
            fosLin.close()

            val uriCbt = FileProvider.getUriForFile(context, "pt.encomendas.cmflores.fileprovider", fileCbt)
            val uriLin = FileProvider.getUriForFile(context, "pt.encomendas.cmflores.fileprovider", fileLin)

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uriCbt, uriLin))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Exportar Encomendas"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
