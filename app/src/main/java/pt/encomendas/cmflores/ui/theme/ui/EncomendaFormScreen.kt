package pt.encomendas.cmflores.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Encomenda
import pt.encomendas.cmflores.data.LinhaEncomenda
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.util.Locale

private fun normalizarPesquisaArtigo(texto: String): String {
    return Normalizer
        .normalize(texto.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
}

private fun apenasDigitosData(texto: String): String {
    return texto.filter { it.isDigit() }.take(8)
}

private fun formatarDataParaGuardar(digitos: String): String {
    val numeros = apenasDigitosData(digitos)
    return buildString {
        numeros.forEachIndexed { indice, caracter ->
            if (indice == 2 || indice == 4) append("/")
            append(caracter)
        }
    }
}

private class DataVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val resultado = formatarDataParaGuardar(text.text)
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> (offset + 2).coerceAtMost(resultado.length)
                }
            }
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 5 -> (offset - 1).coerceAtMost(text.length)
                    else -> (offset - 2).coerceAtMost(text.length)
                }
            }
        }
        return TransformedText(AnnotatedString(resultado), offsetMapping)
    }
}

private val dataVisualTransformation = DataVisualTransformation()

data class LinhaEncomendaTemporaria(
    val artigo: Artigo,
    val quantidade: Double,
    val unidade: String,
    val precoUnitario: Double,
    val total: Double,
    val observacao: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendaFormScreen(
    onVoltar: () -> Unit,
    onGuardada: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val baseDados = DatabaseProvider.obterBaseDados(context)
    val coroutineScope = rememberCoroutineScope()

    var clientes by remember { mutableStateOf<List<Cliente>>(emptyList()) }
    var artigos by remember { mutableStateOf<List<Artigo>>(emptyList()) }
    var clienteSelecionado by remember { mutableStateOf<Cliente?>(null) }
    var linhas by remember { mutableStateOf<List<LinhaEncomendaTemporaria>>(emptyList()) }
    var artigoSelecionado by remember { mutableStateOf<Artigo?>(null) }
    var quantidadeTexto by remember { mutableStateOf("1") }
    var precoUnitarioTexto by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    var pesquisaArtigo by remember { mutableStateOf("") }
    var dataRecolha by remember { mutableStateOf("") }
    var dataEntrega by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("Pendente") }
    var mensagem by remember { mutableStateOf("") }

    var mostrarClientes by remember { mutableStateOf(false) }
    var mostrarArtigos by remember { mutableStateOf(false) }
    var mostrarQuantidade by remember { mutableStateOf(false) }
    var fotoAmpliada by remember { mutableStateOf<String?>(null) }

    var mostrarDialogConfirmacao by remember { mutableStateOf(false) }
    var mostrarDialogWhatsApp by remember { mutableStateOf(false) }
    var numeroEncomendaGerado by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        clientes = baseDados.clienteDao().obterTodos()
        artigos = baseDados.artigoDao().obterTodos()
    }

    val totalEncomenda = linhas.sumOf { it.total }

    fun adicionarLinha() {
        val artigo = artigoSelecionado ?: return
        val quantidadeAdicionar = quantidadeTexto.trim().replace(",", ".").toDoubleOrNull()
        val precoUnitario = precoUnitarioTexto.trim().replace(",", ".").toDoubleOrNull()

        if (quantidadeAdicionar == null || quantidadeAdicionar <= 0) {
            mensagem = "⚠️ Indique uma quantidade válida."
            return
        }
        if (precoUnitario == null || precoUnitario < 0) {
            mensagem = "⚠️ Indique um preço unitário válido."
            return
        }

        linhas = linhas + LinhaEncomendaTemporaria(
            artigo = artigo,
            quantidade = quantidadeAdicionar,
            unidade = artigo.unidade,
            precoUnitario = precoUnitario,
            total = quantidadeAdicionar * precoUnitario,
            observacao = observacao.trim()
        )

        // Reset e Volta à Pesquisa!
        artigoSelecionado = null
        quantidadeTexto = "1"
        precoUnitarioTexto = ""
        observacao = ""
        mostrarQuantidade = false
        pesquisaArtigo = ""
        mostrarArtigos = true // Abre logo a janela de artigos novamente
        mensagem = ""
    }

    fun removerLinha(linhaParaRemover: LinhaEncomendaTemporaria) {
        val indice = linhas.indexOfFirst { it === linhaParaRemover }
        if (indice >= 0) {
            linhas = linhas.toMutableList().also { it.removeAt(indice) }
        }
    }

    fun validarParaGuardar() {
        val camposEmFalta = mutableListOf<String>()

        if (clienteSelecionado == null) camposEmFalta.add("Cliente")
        if (dataRecolha.trim().length < 8) camposEmFalta.add("Data de recolha")
        if (dataEntrega.trim().length < 8) camposEmFalta.add("Data de entrega")
        if (estado.trim().isEmpty()) camposEmFalta.add("Estado")
        if (linhas.isEmpty()) camposEmFalta.add("Artigos (pelo menos 1)")

        if (camposEmFalta.isNotEmpty()) {
            mensagem = "⚠️ Faltam preencher os seguintes campos:\n- " + camposEmFalta.joinToString("\n- ")
            return
        }

        mostrarDialogConfirmacao = true
    }

    fun executarGravacaoNaBD() {
        val cliente = clienteSelecionado ?: return

        coroutineScope.launch {
            try {
                var numeroGerado = ""
                baseDados.withTransaction {
                    val encomendasExistentes = baseDados.encomendaDao().obterTodas()
                    val maiorNumero = encomendasExistentes
                        .mapNotNull { enc ->
                            val numero = enc.numero.trim()
                            if (numero.matches(Regex("^A\\d{9}$"))) numero.substring(1).toLongOrNull() else null
                        }.maxOrNull() ?: 0L

                    val proximoNumero = maiorNumero + 1L
                    numeroGerado = "A" + proximoNumero.toString().padStart(9, '0')

                    val id = baseDados.encomendaDao().inserir(
                        Encomenda(
                            numero = numeroGerado,
                            clienteId = cliente.id,
                            dataRecolha = formatarDataParaGuardar(dataRecolha),
                            dataEntrega = formatarDataParaGuardar(dataEntrega),
                            total = totalEncomenda,
                            estado = estado.trim()
                        )
                    )

                    linhas.forEach { linha ->
                        baseDados.linhaEncomendaDao().inserir(
                            LinhaEncomenda(
                                encomendaId = id,
                                artigoId = linha.artigo.id,
                                quantidade = linha.quantidade,
                                unidade = linha.unidade,
                                precoUnitario = linha.precoUnitario,
                                total = linha.total,
                                observacao = linha.observacao
                            )
                        )
                    }
                }
                numeroEncomendaGerado = numeroGerado
                mostrarDialogConfirmacao = false
                mostrarDialogWhatsApp = true
            } catch (e: Exception) {
                mensagem = "❌ Erro ao guardar encomenda: ${e.message}"
            }
        }
    }

    val artigosFiltrados = artigos.filter { artigo ->
        if (!artigo.ativo) false else {
            val pesquisa = normalizarPesquisaArtigo(pesquisaArtigo.trim())
            normalizarPesquisaArtigo(artigo.codigo).contains(pesquisa) ||
                    normalizarPesquisaArtigo(artigo.descricao).contains(pesquisa) ||
                    normalizarPesquisaArtigo(artigo.categoria).contains(pesquisa)
        }
    }

    if (mostrarDialogConfirmacao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogConfirmacao = false },
            title = { Text("Atenção") },
            text = { Text("Confirma Encomenda?", fontSize = 18.sp) },
            confirmButton = { Button(onClick = { executarGravacaoNaBD() }) { Text("SIM") } },
            dismissButton = { TextButton(onClick = { mostrarDialogConfirmacao = false }) { Text("NÃO") } }
        )
    }

    if (mostrarDialogWhatsApp) {
        AlertDialog(
            onDismissRequest = {
                mostrarDialogWhatsApp = false; onGuardada()
            },
            title = { Text("Enviar documento") },
            text = { Text("Enviar documento pelo WhatsApp?", fontSize = 18.sp) },
            confirmButton = {
                Button(onClick = {
                    gerarPdfEPartilharWhatsApp(context, numeroEncomendaGerado, clienteSelecionado!!, dataRecolha, linhas, totalEncomenda)
                    mostrarDialogWhatsApp = false
                    onGuardada()
                }) { Text("SIM") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogWhatsApp = false; onGuardada() }) { Text("NÃO") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova encomenda") },
                navigationIcon = { TextButton(onClick = onVoltar) { Text("← VOLTAR") } }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Número: será gerado automaticamente", fontWeight = FontWeight.Bold)

                Button(onClick = { mostrarClientes = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(clienteSelecionado?.nome ?: "SELECIONAR CLIENTE")
                }

                OutlinedTextField(
                    value = dataRecolha,
                    onValueChange = { dataRecolha = apenasDigitosData(it); mensagem = "" },
                    label = { Text("Data de recolha") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = dataVisualTransformation
                )

                OutlinedTextField(
                    value = dataEntrega,
                    onValueChange = { dataEntrega = apenasDigitosData(it); mensagem = "" },
                    label = { Text("Data de entrega") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = dataVisualTransformation
                )

                OutlinedTextField(
                    value = estado,
                    onValueChange = { estado = it; mensagem = "" },
                    label = { Text("Estado") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "🌸 ARTIGOS DA ENCOMENDA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (linhas.isEmpty()) Text("Ainda não foram adicionados artigos.")
                else {
                    linhas.forEach { linha ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = linha.artigo.codigo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(text = linha.artigo.descricao, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Quantidade: " + if (linha.quantidade % 1.0 == 0.0) linha.quantidade.toInt().toString() else linha.quantidade.toString() + " ${linha.unidade}")
                                    Text("Preço unitário: € %.2f".format(linha.precoUnitario))
                                    if (linha.observacao.isNotBlank()) Text("Observação: " + linha.observacao)
                                    Text("Total: € %.2f".format(linha.total), fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { removerLinha(linha) }) { Text("✕", fontSize = 22.sp) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                if (mensagem.isNotBlank()) Text(text = mensagem, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { pesquisaArtigo = ""; mensagem = ""; mostrarArtigos = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("➕ ADICIONAR ARTIGO")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "€ %.2f".format(totalEncomenda), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = { validarParaGuardar() }, modifier = Modifier.fillMaxWidth()) {
                        Text("GUARDAR ENCOMENDA")
                    }
                }
            }
        }
    }

    if (mostrarClientes) {
        AlertDialog(
            onDismissRequest = { mostrarClientes = false },
            title = { Text("Selecionar cliente") },
            text = {
                LazyColumn {
                    items(clientes, key = { it.id }) { cliente ->
                        TextButton(onClick = { clienteSelecionado = cliente; mostrarClientes = false }, modifier = Modifier.fillMaxWidth()) { Text(cliente.nome) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mostrarClientes = false }) { Text("CANCELAR") } }
        )
    }

    if (mostrarArtigos) {
        Dialog(onDismissRequest = { mostrarArtigos = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Selecionar artigo", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pesquisaArtigo,
                        onValueChange = { pesquisaArtigo = it.uppercase() }, // MAIÚSCULAS AQUI
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pesquisar artigo") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (artigosFiltrados.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) { Text("Nenhum artigo.") }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(artigosFiltrados, key = { it.id }) { artigo ->
                                ArtigoSelecaoCard(
                                    artigo = artigo,
                                    onSelecionar = {
                                        artigoSelecionado = artigo
                                        quantidadeTexto = "1"
                                        precoUnitarioTexto = String.format(Locale.getDefault(), "%.2f", artigo.precoComIva)
                                        observacao = ""
                                        mostrarArtigos = false
                                        mostrarQuantidade = true
                                    },
                                    onFotoClick = { fotoAmpliada = it }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { mostrarArtigos = false }, modifier = Modifier.fillMaxWidth()) { Text("FECHAR") }
                }
            }
        }
    }

    if (mostrarQuantidade && artigoSelecionado != null) {
        val artigo = artigoSelecionado!!
        AlertDialog(
            onDismissRequest = { mostrarQuantidade = false; artigoSelecionado = null },
            title = { Text("Adicionar artigo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(artigo.descricao, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = precoUnitarioTexto,
                        onValueChange = { precoUnitarioTexto = it; mensagem = "" },
                        label = { Text("Preço unitário (€)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { val v = quantidadeTexto.toIntOrNull() ?: 1; quantidadeTexto = (v - 1).coerceAtLeast(1).toString() }) { Text("−", style = MaterialTheme.typography.headlineMedium) }
                        OutlinedTextField(value = quantidadeTexto, onValueChange = { if (it.all { c -> c.isDigit() }) quantidadeTexto = it }, modifier = Modifier.width(110.dp), label = { Text("Qtd") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        TextButton(onClick = { val v = quantidadeTexto.toIntOrNull() ?: 0; quantidadeTexto = (v + 1).toString() }) { Text("+", style = MaterialTheme.typography.headlineMedium) }
                    }

                    OutlinedTextField(
                        value = observacao,
                        onValueChange = { observacao = it.uppercase(); mensagem = "" }, // MAIÚSCULAS AQUI
                        label = { Text("Observação") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4
                    )
                }
            },
            confirmButton = { Button(onClick = { adicionarLinha() }) { Text("ADICIONAR") } },
            dismissButton = { TextButton(onClick = { mostrarQuantidade = false; artigoSelecionado = null; mostrarArtigos = true }) { Text("CANCELAR") } } // Ao cancelar volta à lista!
        )
    }

    if (fotoAmpliada != null) {
        Dialog(onDismissRequest = { fotoAmpliada = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().clickable { fotoAmpliada = null }) {
                AsyncImage(model = if (fotoAmpliada!!.startsWith("/")) File(fotoAmpliada!!) else Uri.parse(fotoAmpliada!!), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }
    }
}

@Composable
private fun ArtigoSelecaoCard(artigo: Artigo, onSelecionar: () -> Unit, onFotoClick: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onSelecionar() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (artigo.foto.isNotBlank()) {
                AsyncImage(model = if (artigo.foto.startsWith("/")) File(artigo.foto) else Uri.parse(artigo.foto), contentDescription = null, modifier = Modifier.size(70.dp).clickable { onFotoClick(artigo.foto) }, contentScale = ContentScale.Crop)
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = artigo.codigo, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(text = artigo.descricao, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "€ %.2f".format(artigo.precoComIva), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun gerarPdfEPartilharWhatsApp(context: Context, numeroEnc: String, cliente: Cliente, dataDocumento: String, linhas: List<LinhaEncomendaTemporaria>, total: Double) {
    try {
        // LEITURA DOS DADOS DA LOJA VIA SHAREDPREFERENCES
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
                // IMPRESSÃO DOS DADOS REAIS DA LOJA
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
                val dataFormatada = if (dataDocumento.length == 8) "${dataDocumento.substring(4, 8)}/${dataDocumento.substring(2, 4)}/${dataDocumento.substring(0, 2)}" else dataDocumento
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
            var desc = linha.artigo.descricao
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
