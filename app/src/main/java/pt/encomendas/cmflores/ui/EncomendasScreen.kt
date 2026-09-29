package pt.encomendas.cmflores.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Encomenda
import pt.encomendas.cmflores.data.Cliente
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

data class EncomendaComClienteLista(
    val encomenda: Encomenda,
    val cliente: Cliente?
)

data class LinhaPdfLista(
    val quantidade: Double,
    val descricaoArtigo: String,
    val observacao: String,
    val unidade: String,
    val precoUnitario: Double,
    val total: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendasScreen(
    onVoltar: () -> Unit,
    onNovaEncomenda: () -> Unit,
    onAbrirEncomenda: (Long) -> Unit
) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.obterBaseDados(context) }
    val coroutineScope = rememberCoroutineScope()

    var listaEncomendas by remember { mutableStateOf<List<EncomendaComClienteLista>>(emptyList()) }

    LaunchedEffect(Unit) {
        val encomendasBD = db.encomendaDao().obterTodas()
        val clientesBD = db.clienteDao().obterTodos()

        // Mapear cada encomenda com o cliente correspondente e organizar pelas mais recentes
        listaEncomendas = encomendasBD.map { enc ->
            EncomendaComClienteLista(
                encomenda = enc,
                cliente = clientesBD.find { it.id == enc.clienteId }
            )
        }.sortedByDescending { it.encomenda.id }
    }

    fun partilharEncomendaViaWhatsAppLista(item: EncomendaComClienteLista) {
        val clienteAEnviar = item.cliente
        if (clienteAEnviar == null) return

        coroutineScope.launch {
            try {
                val linhasBD = db.linhaEncomendaDao().obterPorEncomenda(item.encomenda.id)
                val linhasPdf = linhasBD.map { linha ->
                    val artigo = db.artigoDao().obterPorId(linha.artigoId)
                    LinhaPdfLista(
                        quantidade = linha.quantidade,
                        descricaoArtigo = artigo?.descricao ?: "Artigo não encontrado",
                        observacao = linha.observacao,
                        unidade = linha.unidade,
                        precoUnitario = linha.precoUnitario,
                        total = linha.total
                    )
                }

                gerarPdfEPartilharWhatsAppLista(
                    context = context,
                    numeroEnc = item.encomenda.numero,
                    cliente = clienteAEnviar,
                    dataRecolha = item.encomenda.dataRecolha,
                    linhas = linhasPdf,
                    total = item.encomenda.total
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listagem de Encomendas") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) { Text("⬅️") }
                },
                actions = {
                    Button(onClick = onNovaEncomenda, modifier = Modifier.padding(end = 8.dp)) {
                        Text("➕ NOVA")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (listaEncomendas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("Não existem encomendas registadas.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listaEncomendas, key = { it.encomenda.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onAbrirEncomenda(item.encomenda.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.encomenda.numero, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "Cliente: ${item.cliente?.nome ?: "Desconhecido"}")
                                Text(text = "Data Recolha: ${item.encomenda.dataRecolha}", color = MaterialTheme.colorScheme.secondary)
                                Text(text = "Estado: ${item.encomenda.estado}")
                                Text(
                                    text = "Total: ${String.format(Locale.getDefault(), "%.2f €", item.encomenda.total)}",
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold
                                )
                            }
                            // Botão WhatsApp
                            IconButton(
                                onClick = { partilharEncomendaViaWhatsAppLista(item) },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("📲", fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------
// GERADOR DE PDF PROFISSIONAL PARA A LISTAGEM DE ENCOMENDAS
// ------------------------------------------------------------------
private fun gerarPdfEPartilharWhatsAppLista(
    context: Context,
    numeroEnc: String,
    cliente: Cliente,
    dataRecolha: String,
    linhas: List<LinhaPdfLista>,
    total: Double
) {
    try {
        val prefs = context.getSharedPreferences("cmflores_prefs", Context.MODE_PRIVATE)
        val lojaNome = prefs.getString("loja_nome", "CM FLORES")?.takeIf { it.isNotBlank() } ?: "CM FLORES"
        val lojaMorada = prefs.getString("loja_morada", "") ?: ""
        val lojaNif = prefs.getString("loja_nif", "") ?: ""
        val lojaContactos = prefs.getString("loja_contactos", "") ?: ""

        val pdfDocument = android.graphics.pdf.PdfDocument()
        val margemEsq = 40f
        val xObs = 220f
        val xUnd = 390f
        val xQtd = 440f
        val xPUnit = 495f
        val margemDir = 550f

        var pageNum = 1
        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = android.graphics.Paint()
        val ptLocale = Locale("pt", "PT")

        fun desenharCabecalho(isPrimeiraPagina: Boolean, subtotalAnterior: Double = 0.0): Float {
            var y = 40f
            if (isPrimeiraPagina) {
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 16f
                canvas.drawText(lojaNome.uppercase(), margemEsq, y, paint)

                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                paint.textSize = 9f
                y += 14f
                if (lojaMorada.isNotBlank()) { canvas.drawText(lojaMorada, margemEsq, y, paint); y += 14f }
                val infoExtra = listOfNotNull(if(lojaNif.isNotBlank()) "NIF: $lojaNif" else null, lojaContactos.ifBlank { null }).joinToString(" | ")
                if (infoExtra.isNotBlank()) { canvas.drawText(infoExtra, margemEsq, y, paint); y += 14f }

                y += 15f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 10f
                canvas.drawText("DATA", margemEsq, y, paint)
                canvas.drawText("DOCUMENTO", 150f, y, paint)
                canvas.drawText("NÚMERO", 350f, y, paint)

                y += 15f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                val dataFormatada = if (dataRecolha.length == 8) "${dataRecolha.substring(4, 8)}/${dataRecolha.substring(2, 4)}/${dataRecolha.substring(0, 2)}" else dataRecolha
                canvas.drawText(dataFormatada, margemEsq, y, paint)
                canvas.drawText("DOCUMENTO INTERNO", 150f, y, paint)
                canvas.drawText(numeroEnc, 350f, y, paint)

                y += 30f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText("1a VIA", margemEsq, y, paint)

                y += 20f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
                canvas.drawText("Exmo (s) Senhor (es)", margemEsq, y, paint)

                y += 15f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                canvas.drawText(cliente.nome.uppercase(), margemEsq, y, paint)
                y += 40f
            } else {
                y += 20f
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 10f
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                canvas.drawText("TRANSPORTE", 450f, y, paint)
                canvas.drawText(String.format(ptLocale, "%.2f", subtotalAnterior), margemDir, y, paint)
                paint.textAlign = android.graphics.Paint.Align.LEFT
                y += 30f
            }

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("Descrição", margemEsq, y, paint)
            canvas.drawText("Observação", xObs, y, paint)
            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText("Und.", xUnd, y, paint)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas.drawText("Quant.", xQtd, y, paint)
            canvas.drawText("P. Unit", xPUnit, y, paint)
            canvas.drawText("Valor", margemDir, y, paint)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            y += 10f
            canvas.drawLine(margemEsq, y, margemDir, y, paint)
            return y + 20f
        }

        var yPosition = desenharCabecalho(true)
        var subtotal = 0.0

        linhas.forEach { linha ->
            if (yPosition > 700f) {
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 10f
                paint.textAlign = android.graphics.Paint.Align.RIGHT
                canvas.drawText("A TRANSPORTAR", 450f, yPosition + 20f, paint)
                canvas.drawText(String.format(ptLocale, "%.2f", subtotal), margemDir, yPosition + 20f, paint)
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = desenharCabecalho(false, subtotal)
            }

            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
            paint.textSize = 9f
            paint.textAlign = android.graphics.Paint.Align.LEFT
            var desc = linha.descricaoArtigo
            if (desc.length > 35) desc = desc.substring(0, 32) + "..."
            canvas.drawText(desc, margemEsq, yPosition, paint)

            var obs = linha.observacao
            if (obs.length > 30) obs = obs.substring(0, 27) + "..."
            canvas.drawText(obs, xObs, yPosition, paint)

            paint.textAlign = android.graphics.Paint.Align.CENTER
            canvas.drawText(if (linha.unidade.isNotBlank()) linha.unidade else "Un", xUnd, yPosition, paint)

            paint.textAlign = android.graphics.Paint.Align.RIGHT
            val qtdStr = String.format(ptLocale, "%.4f", linha.quantidade).replace(",0000", "")
            canvas.drawText(qtdStr, xQtd, yPosition, paint)
            canvas.drawText(String.format(ptLocale, "%.2f", linha.precoUnitario), xPUnit, yPosition, paint)
            canvas.drawText(String.format(ptLocale, "%.2f", linha.total), margemDir, yPosition, paint)

            subtotal += linha.total
            yPosition += 15f
        }

        if (yPosition > 680f) {
            paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            canvas.drawText("A TRANSPORTAR", 450f, yPosition + 20f, paint)
            canvas.drawText(String.format(ptLocale, "%.2f", subtotal), margemDir, yPosition + 20f, paint)
            pdfDocument.finishPage(page)
            pageNum++
            pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            yPosition = desenharCabecalho(false, subtotal)
        }

        yPosition += 10f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawLine(margemEsq, yPosition, margemDir, yPosition, paint)
        yPosition += 25f

        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        paint.textSize = 9f
        val xResumoTexto = 400f

        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("Total Iliquido", xResumoTexto, yPosition, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText(String.format(ptLocale, "%.2f", total), margemDir, yPosition, paint)

        yPosition += 15f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("Descontos", xResumoTexto, yPosition, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("0,00", margemDir, yPosition, paint)

        yPosition += 15f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("Total Líquido", xResumoTexto, yPosition, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText(String.format(ptLocale, "%.2f", total), margemDir, yPosition, paint)

        yPosition += 15f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("I. V. A.", xResumoTexto, yPosition, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText("0,00", margemDir, yPosition, paint)

        yPosition += 25f
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        paint.textSize = 12f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText("TOTAL", xResumoTexto, yPosition, paint)
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText(String.format(ptLocale, "%.2f €", total), margemDir, yPosition, paint)

        pdfDocument.finishPage(page)

        val pastaPdfs = File(context.cacheDir, "pdfs")
        if (!pastaPdfs.exists()) pastaPdfs.mkdirs()
        val file = File(pastaPdfs, "Encomenda_$numeroEnc.pdf")
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
        try { context.startActivity(Intent.createChooser(intent, "Enviar Encomenda")) }
        catch (e: Exception) {
            intent.setPackage("com.whatsapp")
            try { context.startActivity(Intent.createChooser(intent, "Enviar Encomenda")) } catch (e2: Exception) { }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
