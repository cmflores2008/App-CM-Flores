package pt.encomendas.cmflores.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.LinhaEncomenda
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

data class LinhaRelatorio(
    val artigo: Artigo?,
    val observacao: String,
    val quantidadeTotal: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatorioDatasScreen(
    dataInicio: String,
    dataFim: String,
    onVoltar: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.obterBaseDados(context) }

    var linhasAgrupadas by remember { mutableStateOf<List<LinhaRelatorio>>(emptyList()) }
    var aCarregar by remember { mutableStateOf(true) }

    LaunchedEffect(dataInicio, dataFim) {
        aCarregar = true
        try {
            // Usamos o calendário real para ler as datas corretamente, independentemente do mês ou ano
            val formatoData = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateInicioStr = formatoData.parse(dataInicio)
            val dateFimStr = formatoData.parse(dataFim)

            // 1. Vamos buscar TODAS as encomendas à base de dados
            val todasEncomendas = db.encomendaDao().obterTodas()

            // 2. Filtramos em Kotlin, usando lógica de calendário real
            val encomendasNoPeriodo = todasEncomendas.filter { enc ->
                try {
                    val dataEnc = formatoData.parse(enc.dataRecolha)
                    if (dataEnc != null && dateInicioStr != null && dateFimStr != null) {
                        // Verifica se a data da encomenda é igual/superior ao início e igual/inferior ao fim
                        !dataEnc.before(dateInicioStr) && !dataEnc.after(dateFimStr)
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false // Ignora encomendas com datas mal formatadas
                }
            }

            // 3. Vamos buscar as linhas apenas das encomendas que passaram no filtro do calendário
            val linhasDoPeriodo = mutableListOf<LinhaEncomenda>()
            encomendasNoPeriodo.forEach { enc ->
                linhasDoPeriodo.addAll(db.linhaEncomendaDao().obterPorEncomenda(enc.id))
            }

            // 4. Agrupa os artigos pelos mesmos moldes (Código + Observação)
            val agrupado = linhasDoPeriodo.groupBy { Pair(it.artigoId, it.observacao.trim().lowercase()) }

            val resultado = agrupado.map { (chave, listaLinhas) ->
                val artigoId = chave.first
                val observacaoDisplay = listaLinhas.first().observacao.trim().uppercase()
                val qtdeTotal = listaLinhas.sumOf { it.quantidade }
                val artigo = db.artigoDao().obterPorId(artigoId)

                LinhaRelatorio(artigo = artigo, observacao = observacaoDisplay, quantidadeTotal = qtdeTotal)
            }.sortedWith(compareBy({ it.artigo?.codigo ?: "ZZZ" }, { it.observacao }))

            linhasAgrupadas = resultado
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            aCarregar = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Totais por Data") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Text("⬅️") }
                },
                actions = {
                    if (linhasAgrupadas.isNotEmpty()) {
                        IconButton(onClick = {
                            gerarPdfRelatorio(context, dataInicio, dataFim, linhasAgrupadas)
                        }) {
                            Text("📲", fontSize = 24.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Text(text = "Período: $dataInicio a $dataFim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (aCarregar) {
                CircularProgressIndicator()
            } else if (linhasAgrupadas.isEmpty()) {
                Text("Não existem artigos encomendados para estas datas.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(linhasAgrupadas) { linha ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = linha.artigo?.codigo ?: "Desconhecido", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(text = linha.artigo?.descricao ?: "Não encontrado", style = MaterialTheme.typography.bodyLarge)
                                    if (linha.observacao.isNotEmpty()) {
                                        Text(text = "Obs: ${linha.observacao}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                val qtdeFormatada = if (linha.quantidadeTotal % 1.0 == 0.0) linha.quantidadeTotal.toInt().toString() else linha.quantidadeTotal.toString()
                                Text(text = qtdeFormatada, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// GERADOR DE PDF - RELATÓRIO AINDA MAIS COMPACTO
// ------------------------------------------------------------------
private fun gerarPdfRelatorio(context: Context, dataInicio: String, dataFim: String, linhas: List<LinhaRelatorio>) {
    try {
        val prefs = context.getSharedPreferences("cmflores_prefs", Context.MODE_PRIVATE)
        val lojaNome = prefs.getString("loja_nome", "CM FLORES")?.takeIf { it.isNotBlank() } ?: "CM FLORES"

        val pdfDocument = android.graphics.pdf.PdfDocument()

        // Definição das posições das colunas
        val margemEsq = 40f
        val xDesc = 120f
        val xObs = 330f
        val margemDir = 550f

        var pageNum = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = android.graphics.Paint()
        val ptLocale = Locale("pt", "PT")

        fun desenharCabecalho(): Float {
            var y = 50f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 16f
            paint.textAlign = android.graphics.Paint.Align.LEFT
            canvas.drawText(lojaNome.uppercase(), margemEsq, y, paint)

            y += 30f
            paint.textSize = 12f
            canvas.drawText("RELATÓRIO DE TOTAIS POR DATA", margemEsq, y, paint)
            y += 15f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            canvas.drawText("Período de recolha: $dataInicio a $dataFim", margemEsq, y, paint)

            y += 30f
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 10f

            // Títulos das Colunas
            canvas.drawText("CÓDIGO", margemEsq, y, paint)
            canvas.drawText("DESCRIÇÃO", xDesc, y, paint)
            canvas.drawText("OBSERVAÇÃO", xObs, y, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas.drawText("QUANTIDADE", margemDir, y, paint)

            paint.textAlign = android.graphics.Paint.Align.LEFT
            y += 10f
            canvas.drawLine(margemEsq, y, margemDir, y, paint)
            return y + 20f
        }

        var yPosition = desenharCabecalho()

        linhas.forEach { linha ->
            if (yPosition > 780f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = desenharCabecalho()
            }

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 10f

            // Código da flor
            paint.textAlign = android.graphics.Paint.Align.LEFT
            val codigo = linha.artigo?.codigo ?: ""
            canvas.drawText(if (codigo.length > 12) codigo.substring(0, 10) + "..." else codigo, margemEsq, yPosition, paint)

            // Descrição da flor (Coluna Fixa)
            var desc = linha.artigo?.descricao ?: ""
            if (desc.length > 35) desc = desc.substring(0, 32) + "..."
            canvas.drawText(desc, xDesc, yPosition, paint)

            // Observação (Coluna Fixa)
            var obs = linha.observacao
            if (obs.length > 35) obs = obs.substring(0, 32) + "..."
            canvas.drawText(obs, xObs, yPosition, paint)

            // Quantidade Total Agrupada
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            val qtdStr = String.format(ptLocale, "%.4f", linha.quantidadeTotal).replace(",0000", "")
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            canvas.drawText(qtdStr, margemDir, yPosition, paint)

            // Avanço vertical mínimo
            yPosition += 12f
        }

        pdfDocument.finishPage(page)

        val pastaPdfs = File(context.cacheDir, "pdfs")
        if (!pastaPdfs.exists()) pastaPdfs.mkdirs()
        val file = File(pastaPdfs, "Relatorio_${dataInicio.replace("/","")}_a_${dataFim.replace("/","")}.pdf")
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()

        val uri = FileProvider.getUriForFile(context, "pt.encomendas.cmflores.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { context.startActivity(Intent.createChooser(intent, "Enviar Relatório")) }
        catch (e: Exception) {
            intent.setPackage("com.whatsapp")
            try { context.startActivity(Intent.createChooser(intent, "Enviar Relatório")) } catch (e2: Exception) { }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
