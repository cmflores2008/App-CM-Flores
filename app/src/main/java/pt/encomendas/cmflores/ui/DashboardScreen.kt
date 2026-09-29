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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Estrutura de apoio para conseguirmos mostrar o nome do cliente no Dashboard
data class EncomendaDisplay(
    val encomenda: Encomenda,
    val nomeCliente: String
)

// Estrutura para lermos os artigos rapidamente no Dashboard e gerarmos o PDF
data class LinhaPdfDashboard(
    val quantidade: Double,
    val descricaoArtigo: String,
    val observacao: String,
    val unidade: String,
    val precoUnitario: Double,
    val total: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onVoltar: () -> Unit,
    onNavigateToEncomendas: () -> Unit,
    onNavigateToClientes: () -> Unit,
    onNavigateToArtigos: () -> Unit,
    onNavigateToImportacao: () -> Unit,
    onNavigateToPesquisaDatas: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.obterBaseDados(context) }
    val coroutineScope = rememberCoroutineScope()

    var qtdPendentes by remember { mutableStateOf(0) }
    var totalEurosPendentes by remember { mutableStateOf(0.0) }
    var qtdSemFoto by remember { mutableStateOf(0) }

    var mostrarDialogDatas by remember { mutableStateOf(false) }
    var mostrarDialogPendentes by remember { mutableStateOf(false) }

    var listaPendentes by remember { mutableStateOf<List<EncomendaDisplay>>(emptyList()) }
    var proximasEntregas by remember { mutableStateOf<List<EncomendaDisplay>>(emptyList()) }

    LaunchedEffect(Unit) {
        qtdSemFoto = db.artigoDao().contarSemFoto()

        val todas = db.encomendaDao().obterTodas()
        val apenasPendentes = todas.filter { it.estado == "Pendente" }

        val mapeadas = apenasPendentes.map { enc ->
            val cliente = db.clienteDao().obterPorId(enc.clienteId)
            EncomendaDisplay(
                encomenda = enc,
                nomeCliente = cliente?.nome ?: "Cliente Desconhecido"
            )
        }

        listaPendentes = mapeadas
        qtdPendentes = mapeadas.size
        totalEurosPendentes = mapeadas.sumOf { it.encomenda.total }

        proximasEntregas = mapeadas.sortedBy { it.encomenda.dataEntrega }.take(5)
    }

    fun partilharEncomendaViaWhatsApp(encomenda: Encomenda, nomeCliente: String) {
        coroutineScope.launch {
            try {
                val linhasBD = db.linhaEncomendaDao().obterPorEncomenda(encomenda.id)
                val linhasPdf = linhasBD.map { linha ->
                    val artigo = db.artigoDao().obterPorId(linha.artigoId)
                    LinhaPdfDashboard(
                        quantidade = linha.quantidade,
                        descricaoArtigo = artigo?.descricao ?: "Artigo não encontrado",
                        observacao = linha.observacao,
                        unidade = linha.unidade,
                        precoUnitario = linha.precoUnitario,
                        total = linha.total
                    )
                }

                gerarPdfEPartilharWhatsAppDashboard(
                    context = context,
                    numeroEnc = encomenda.numero,
                    nomeCliente = nomeCliente,
                    dataRecolha = encomenda.dataRecolha,
                    dataEntrega = encomenda.dataEntrega,
                    linhas = linhasPdf,
                    total = encomenda.total
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (mostrarDialogDatas) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDialogDatas = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val dataInicio = format.format(Date(start))
                        val dataFim = format.format(Date(end))
                        onNavigateToPesquisaDatas(dataInicio, dataFim)
                    }
                    mostrarDialogDatas = false
                }) {
                    Text("Pesquisar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogDatas = false }) { Text("Cancelar") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f),
                title = { Text(text = "Selecione o intervalo de datas", modifier = Modifier.padding(16.dp)) },
                headline = { Text(text = "Datas de Recolha", modifier = Modifier.padding(horizontal = 16.dp)) }
            )
        }
    }

    if (mostrarDialogPendentes) {
        AlertDialog(
            onDismissRequest = { mostrarDialogPendentes = false },
            title = { Text("Listagem de Pendentes", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(listaPendentes) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Enc. ${item.encomenda.numero}", fontWeight = FontWeight.Bold)
                                Text(text = "Cliente: ${item.nomeCliente}")
                                Text(
                                    text = "Total: ${String.format(Locale.getDefault(), "%.2f €", item.encomenda.total)}",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { partilharEncomendaViaWhatsApp(item.encomenda, item.nomeCliente) }) {
                                Text("📲", fontSize = 28.sp)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostrarDialogPendentes = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Painel Principal - CM Flores") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Text("⬅️")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text("Resumo do Dia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResumoCard(
                        titulo = "Pendentes (Ver)",
                        valor = qtdPendentes.toString(),
                        subValor = String.format(Locale.getDefault(), "%.2f €", totalEurosPendentes),
                        icone = "🛒",
                        modifier = Modifier
                            .weight(1f)
                            .clickable { mostrarDialogPendentes = true }
                    )
                    ResumoCard(
                        titulo = "Sem Foto",
                        valor = qtdSemFoto.toString(),
                        subValor = "",
                        icone = "⚠️",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("Ações Rápidas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNavigateToEncomendas,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text("➕", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Criar Nova Encomenda", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onNavigateToClientes,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text("👥")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clientes")
                        }
                        OutlinedButton(
                            onClick = onNavigateToArtigos,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text("🌹")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Artigos")
                        }
                    }
                    OutlinedButton(
                        onClick = onNavigateToImportacao,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔄")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Importar Catálogo (Excel)")
                    }
                }
            }

            item {
                Text("Relatórios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { mostrarDialogDatas = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("📅", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Totais por Data de Recolha", style = MaterialTheme.typography.titleMedium)
                }
            }

            item {
                Text("Próximas Entregas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (proximasEntregas.isEmpty()) {
                            Text(
                                text = "Não existem entregas pendentes de momento.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            proximasEntregas.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Enc. ${item.encomenda.numero} - ${item.nomeCliente}", fontWeight = FontWeight.Bold)
                                        Text("Data Entrega: ${item.encomenda.dataEntrega}", color = MaterialTheme.colorScheme.secondary)
                                    }
                                    IconButton(onClick = { partilharEncomendaViaWhatsApp(item.encomenda, item.nomeCliente) }) {
                                        Text("📲", fontSize = 28.sp)
                                    }
                                }

                                if (index < proximasEntregas.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onVoltar,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("⬅️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voltar ao Menu Inicial", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun ResumoCard(titulo: String, valor: String, subValor: String, icone: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icone, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (subValor.isNotEmpty()) {
                Text(subValor, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ------------------------------------------------------------------
// NOVO GERADOR DE PDF - FORMATO PROFISSIONAL CLÁSSICO COM COLUNAS FIXAS
// ------------------------------------------------------------------
private fun gerarPdfEPartilharWhatsAppDashboard(
    context: Context,
    numeroEnc: String,
    nomeCliente: String,
    dataRecolha: String,
    dataEntrega: String,
    linhas: List<LinhaPdfDashboard>,
    total: Double
) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        // Definição exata das Colunas
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
            var y = 50f
            if (isPrimeiraPagina) {
                paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                paint.textSize = 10f
                paint.textAlign = android.graphics.Paint.Align.LEFT

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
                canvas.drawText(nomeCliente.uppercase(), margemEsq, y, paint)

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

            // Títulos das Colunas Ajustados
            paint.textAlign = android.graphics.Paint.Align.LEFT
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

            // Descrição (Limitada para não invadir a Observação)
            paint.textAlign = android.graphics.Paint.Align.LEFT
            var desc = linha.descricaoArtigo
            if (desc.length > 35) desc = desc.substring(0, 32) + "..."
            canvas.drawText(desc, margemEsq, yPosition, paint)

            // Observação (Sempre na coluna fixa)
            var obs = linha.observacao
            if (obs.length > 30) obs = obs.substring(0, 27) + "..."
            canvas.drawText(obs, xObs, yPosition, paint)

            // Valores Agrupados à Direita
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

        try {
            context.startActivity(Intent.createChooser(intent, "Enviar Encomenda"))
        } catch (e: Exception) {
            intent.setPackage("com.whatsapp")
            try {
                context.startActivity(Intent.createChooser(intent, "Enviar Encomenda"))
            } catch (e2: Exception) {
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
