package pt.encomendas.cmflores.ui

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
import androidx.room.withTransaction
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Encomenda
import pt.encomendas.cmflores.data.LinhaEncomenda
import java.io.File
import java.text.Normalizer
import java.util.Locale

private fun normalizarPesquisaArtigo(texto: String): String {
    return Normalizer
        .normalize(
            texto.lowercase(Locale.getDefault()),
            Normalizer.Form.NFD
        )
        .replace(
            Regex("\\p{InCombiningDiacriticalMarks}+"),
            ""
        )
}

private fun apenasDigitosData(texto: String): String {
    return texto
        .filter { it.isDigit() }
        .take(8)
}

private fun formatarDataParaGuardar(digitos: String): String {
    val numeros = apenasDigitosData(digitos)

    return buildString {
        numeros.forEachIndexed { indice, caracter ->

            if (
                indice == 2 ||
                indice == 4
            ) {
                append("/")
            }

            append(caracter)
        }
    }
}

private class DataVisualTransformation : VisualTransformation {

    override fun filter(
        text: AnnotatedString
    ): TransformedText {

        val resultado =
            formatarDataParaGuardar(
                text.text
            )

        val offsetMapping =
            object : OffsetMapping {

                override fun originalToTransformed(
                    offset: Int
                ): Int {

                    return when {
                        offset <= 2 ->
                            offset

                        offset <= 4 ->
                            offset + 1

                        else ->
                            (offset + 2)
                                .coerceAtMost(
                                    resultado.length
                                )
                    }
                }

                override fun transformedToOriginal(
                    offset: Int
                ): Int {

                    return when {
                        offset <= 2 ->
                            offset

                        offset <= 5 ->
                            (offset - 1)
                                .coerceAtMost(
                                    text.length
                                )

                        else ->
                            (offset - 2)
                                .coerceAtMost(
                                    text.length
                                )
                    }
                }
            }

        return TransformedText(
            text = AnnotatedString(resultado),
            offsetMapping = offsetMapping
        )
    }
}

private val dataVisualTransformation =
    DataVisualTransformation()

private data class LinhaEncomendaTemporaria(
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

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val baseDados =
        DatabaseProvider.obterBaseDados(context)

    val coroutineScope =
        rememberCoroutineScope()

    var clientes by remember {
        mutableStateOf<List<Cliente>>(emptyList())
    }

    var artigos by remember {
        mutableStateOf<List<Artigo>>(emptyList())
    }

    var clienteSelecionado by remember {
        mutableStateOf<Cliente?>(null)
    }

    var linhas by remember {
        mutableStateOf<List<LinhaEncomendaTemporaria>>(
            emptyList()
        )
    }

    var artigoSelecionado by remember {
        mutableStateOf<Artigo?>(null)
    }

    var quantidadeTexto by remember {
        mutableStateOf("1")
    }

    var precoUnitarioTexto by remember {
        mutableStateOf("")
    }

    var observacao by remember {
        mutableStateOf("")
    }

    var pesquisaArtigo by remember {
        mutableStateOf("")
    }

    var dataRecolha by remember {
        mutableStateOf("")
    }

    var dataEntrega by remember {
        mutableStateOf("")
    }

    var estado by remember {
        mutableStateOf("Pendente")
    }

    var mensagem by remember {
        mutableStateOf("")
    }

    var mostrarClientes by remember {
        mutableStateOf(false)
    }

    var mostrarArtigos by remember {
        mutableStateOf(false)
    }

    var mostrarQuantidade by remember {
        mutableStateOf(false)
    }

    var fotoAmpliada by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        clientes =
            baseDados
                .clienteDao()
                .obterTodos()

        artigos =
            baseDados
                .artigoDao()
                .obterTodos()
    }

    val totalEncomenda =
        linhas.sumOf { linha ->
            linha.total
        }

    fun adicionarLinha() {

        val artigo =
            artigoSelecionado
                ?: return

        val quantidadeAdicionar =
            quantidadeTexto
                .trim()
                .replace(",", ".")
                .toDoubleOrNull()

        val precoUnitario =
            precoUnitarioTexto
                .trim()
                .replace(",", ".")
                .toDoubleOrNull()

        if (
            quantidadeAdicionar == null ||
            quantidadeAdicionar <= 0
        ) {
            mensagem =
                "⚠️ Indique uma quantidade válida."
            return
        }

        if (
            precoUnitario == null ||
            precoUnitario < 0
        ) {
            mensagem =
                "⚠️ Indique um preço unitário válido."
            return
        }

        val observacaoLimpa =
            observacao.trim()

        linhas =
            linhas +
                    LinhaEncomendaTemporaria(
                        artigo = artigo,
                        quantidade = quantidadeAdicionar,
                        unidade = artigo.unidade,
                        precoUnitario = precoUnitario,
                        total =
                            quantidadeAdicionar *
                                    precoUnitario,
                        observacao = observacaoLimpa
                    )

        artigoSelecionado = null
        quantidadeTexto = "1"
        precoUnitarioTexto = ""
        observacao = ""
        mostrarQuantidade = false
        mensagem = ""
    }

    fun removerLinha(
        linhaParaRemover:
        LinhaEncomendaTemporaria
    ) {

        val indice =
            linhas.indexOfFirst {
                it === linhaParaRemover
            }

        if (indice >= 0) {

            linhas =
                linhas
                    .toMutableList()
                    .also {
                        it.removeAt(indice)
                    }
        }
    }

    fun guardarEncomenda() {

        val cliente =
            clienteSelecionado

        if (cliente == null) {

            mensagem =
                "⚠️ Selecione um cliente."

            return
        }

        if (linhas.isEmpty()) {

            mensagem =
                "⚠️ Adicione pelo menos um artigo à encomenda."

            return
        }

        coroutineScope.launch {

            try {

                var numeroGerado = ""

                baseDados.withTransaction {

                    /*
                     * Procurar o maior número AXXXXXXXXX
                     * já existente.
                     *
                     * Os números antigos, caso existam,
                     * são ignorados.
                     */
                    val encomendasExistentes =
                        baseDados
                            .encomendaDao()
                            .obterTodas()

                    val maiorNumero =
                        encomendasExistentes
                            .mapNotNull { encomenda ->

                                val numero =
                                    encomenda.numero.trim()

                                if (
                                    numero.matches(
                                        Regex(
                                            "^A\\d{9}$"
                                        )
                                    )
                                ) {

                                    numero
                                        .substring(1)
                                        .toLongOrNull()

                                } else {

                                    null
                                }
                            }
                            .maxOrNull()
                            ?: 0L

                    val proximoNumero =
                        maiorNumero + 1L

                    numeroGerado =
                        "A" +
                                proximoNumero
                                    .toString()
                                    .padStart(
                                        9,
                                        '0'
                                    )

                    /*
                     * Criar a encomenda já com o número
                     * comercial definitivo.
                     */
                    val id =
                        baseDados
                            .encomendaDao()
                            .inserir(
                                Encomenda(
                                    numero =
                                        numeroGerado,
                                    clienteId =
                                        cliente.id,
                                    dataRecolha =
                                        formatarDataParaGuardar(
                                            dataRecolha
                                        ),
                                    dataEntrega =
                                        formatarDataParaGuardar(
                                            dataEntrega
                                        ),
                                    total =
                                        totalEncomenda,
                                    estado =
                                        estado
                                            .trim()
                                            .ifBlank {
                                                "Pendente"
                                            }
                                )
                            )

                    /*
                     * Gravar todas as linhas dentro da mesma
                     * transação.
                     */
                    linhas.forEach { linha ->

                        baseDados
                            .linhaEncomendaDao()
                            .inserir(
                                LinhaEncomenda(
                                    encomendaId = id,
                                    artigoId =
                                        linha.artigo.id,
                                    quantidade =
                                        linha.quantidade,
                                    unidade =
                                        linha.unidade,
                                    precoUnitario =
                                        linha.precoUnitario,
                                    total =
                                        linha.total,
                                    observacao =
                                        linha.observacao
                                )
                            )
                    }
                }

                onGuardada()

            } catch (e: Exception) {

                mensagem =
                    "❌ Erro ao guardar encomenda: " +
                            "${e.message}"
            }
        }
    }

    val artigosFiltrados =
        artigos.filter { artigo ->

            if (!artigo.ativo) {

                false

            } else {

                val pesquisa =
                    normalizarPesquisaArtigo(
                        pesquisaArtigo.trim()
                    )

                normalizarPesquisaArtigo(
                    artigo.codigo
                ).contains(pesquisa) ||
                        normalizarPesquisaArtigo(
                            artigo.descricao
                        ).contains(pesquisa) ||
                        normalizarPesquisaArtigo(
                            artigo.categoria
                        ).contains(pesquisa)
            }
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Nova encomenda")
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVoltar
                    ) {
                        Text("← VOLTAR")
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            Column(

                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text =
                        "Número: será gerado automaticamente",
                    fontWeight =
                        FontWeight.Bold
                )

                Button(

                    onClick = {
                        mostrarClientes = true
                    },

                    modifier =
                        Modifier.fillMaxWidth()

                ) {

                    Text(
                        clienteSelecionado?.nome
                            ?: "SELECIONAR CLIENTE"
                    )
                }

                OutlinedTextField(

                    value = dataRecolha,

                    onValueChange = {
                        dataRecolha =
                            apenasDigitosData(it)
                    },

                    label = {
                        Text("Data de recolha")
                    },

                    placeholder = {
                        Text("dd/mm/aaaa")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    visualTransformation =
                        dataVisualTransformation
                )

                OutlinedTextField(

                    value = dataEntrega,

                    onValueChange = {
                        dataEntrega =
                            apenasDigitosData(it)
                    },

                    label = {
                        Text("Data de entrega")
                    },

                    placeholder = {
                        Text("dd/mm/aaaa")
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    singleLine = true,

                    visualTransformation =
                        dataVisualTransformation
                )

                OutlinedTextField(

                    value = estado,

                    onValueChange = {
                        estado = it
                    },

                    label = {
                        Text("Estado")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(
                    text =
                        "🌸 ARTIGOS DA ENCOMENDA",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )

                if (linhas.isEmpty()) {

                    Text(
                        "Ainda não foram adicionados artigos."
                    )

                } else {

                    linhas.forEach { linha ->

                        Card(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Column(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),

                                verticalArrangement =
                                    Arrangement.spacedBy(4.dp)
                            ) {

                                Row(

                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    horizontalArrangement =
                                        Arrangement.SpaceBetween,

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Column(

                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text =
                                                linha.artigo.codigo,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodySmall,

                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            text =
                                                linha.artigo.descricao,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .titleMedium,

                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        if (
                                            linha.artigo.categoria
                                                .isNotBlank()
                                        ) {

                                            Text(
                                                "Categoria: " +
                                                        linha.artigo
                                                            .categoria
                                            )
                                        }

                                        Text(

                                            text =
                                                "Quantidade: " +

                                                        if (
                                                            linha.quantidade %
                                                            1.0 == 0.0
                                                        ) {

                                                            linha.quantidade
                                                                .toInt()
                                                                .toString()

                                                        } else {

                                                            linha.quantidade
                                                                .toString()
                                                        } +

                                                        if (
                                                            linha.unidade
                                                                .isNotBlank()
                                                        ) {

                                                            " ${linha.unidade}"

                                                        } else {

                                                            ""
                                                        }
                                        )

                                        Text(

                                            "Preço unitário: € %.2f"
                                                .format(
                                                    linha.precoUnitario
                                                )
                                        )

                                        if (
                                            linha.observacao
                                                .isNotBlank()
                                        ) {

                                            Text(
                                                "Observação: " +
                                                        linha.observacao
                                            )
                                        }

                                        Text(

                                            "Total: € %.2f"
                                                .format(
                                                    linha.total
                                                ),

                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }

                                    TextButton(

                                        onClick = {
                                            removerLinha(linha)
                                        }

                                    ) {

                                        Text(
                                            "✕",
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                if (mensagem.isNotBlank()) {

                    Text(

                        text = mensagem,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp
                        )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Button(

                        onClick = {

                            pesquisaArtigo = ""
                            mensagem = ""
                            mostrarArtigos = true
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "➕ ADICIONAR ARTIGO"
                        )
                    }

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.SpaceBetween,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(

                            text =
                                "TOTAL DA ENCOMENDA",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(

                            text =
                                "€ %.2f"
                                    .format(
                                        totalEncomenda
                                    ),

                            style =
                                MaterialTheme
                                    .typography
                                    .titleLarge,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Button(

                        onClick = {
                            guardarEncomenda()
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text(
                            "GUARDAR ENCOMENDA"
                        )
                    }
                }
            }
        }
    }

    if (mostrarClientes) {

        AlertDialog(

            onDismissRequest = {
                mostrarClientes = false
            },

            title = {
                Text("Selecionar cliente")
            },

            text = {

                LazyColumn {

                    items(

                        clientes,

                        key = {
                                cliente ->
                            cliente.id
                        }

                    ) { cliente ->

                        TextButton(

                            onClick = {

                                clienteSelecionado =
                                    cliente

                                mostrarClientes =
                                    false
                            },

                            modifier =
                                Modifier.fillMaxWidth()

                        ) {

                            Text(
                                cliente.nome
                            )
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(

                    onClick = {
                        mostrarClientes = false
                    }

                ) {

                    Text("CANCELAR")
                }
            }
        )
    }

    if (mostrarArtigos) {

        Dialog(

            onDismissRequest = {
                mostrarArtigos = false
            },

            properties =
                DialogProperties(
                    usePlatformDefaultWidth =
                        false
                )

        ) {

            Card(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)

            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                ) {

                    Text(

                        text =
                            "Selecionar artigo",

                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    OutlinedTextField(

                        value =
                            pesquisaArtigo,

                        onValueChange = {
                            pesquisaArtigo = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text("Pesquisar artigo")
                        },

                        placeholder = {
                            Text(
                                "Código, descrição ou categoria"
                            )
                        },

                        singleLine = true
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(

                        text =
                            "${artigosFiltrados.size} artigo(s)",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    if (
                        artigosFiltrados.isEmpty()
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(
                                        1f,
                                        fill = false
                                    ),

                            contentAlignment =
                                Alignment.Center

                        ) {

                            Text(
                                "Nenhum artigo ativo encontrado."
                            )
                        }

                    } else {

                        LazyColumn(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .weight(
                                        1f,
                                        fill = false
                                    ),

                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            items(

                                artigosFiltrados,

                                key = {
                                        artigo ->
                                    artigo.id
                                }

                            ) { artigo ->

                                ArtigoSelecaoCard(

                                    artigo =
                                        artigo,

                                    onSelecionar = {

                                        artigoSelecionado =
                                            artigo

                                        quantidadeTexto =
                                            "1"

                                        precoUnitarioTexto =
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                artigo.precoComIva
                                            )

                                        observacao = ""

                                        mostrarArtigos =
                                            false

                                        mostrarQuantidade =
                                            true
                                    },

                                    onFotoClick = {
                                            caminho ->
                                        fotoAmpliada =
                                            caminho
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    TextButton(

                        onClick = {
                            mostrarArtigos = false
                        },

                        modifier =
                            Modifier.fillMaxWidth()

                    ) {

                        Text("CANCELAR")
                    }
                }
            }
        }
    }

    if (
        mostrarQuantidade &&
        artigoSelecionado != null
    ) {

        val artigo =
            artigoSelecionado!!

        AlertDialog(

            onDismissRequest = {

                mostrarQuantidade =
                    false

                artigoSelecionado =
                    null

                quantidadeTexto =
                    "1"

                precoUnitarioTexto =
                    ""

                observacao =
                    ""
            },

            title = {
                Text("Adicionar artigo")
            },

            text = {

                Column(

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        artigo.descricao,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        artigo.categoria.isNotBlank()
                    ) {

                        Text(
                            "Categoria: " +
                                    artigo.categoria
                        )
                    }

                    OutlinedTextField(

                        value =
                            precoUnitarioTexto,

                        onValueChange = {

                            precoUnitarioTexto =
                                it

                            mensagem = ""
                        },

                        label = {
                            Text(
                                "Preço unitário (€)"
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        singleLine = true
                    )

                    Text(

                        text =

                            if (
                                artigo.unidade.isNotBlank()
                            ) {

                                "Unidade: ${artigo.unidade}"

                            } else {

                                "Unidade não definida"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        TextButton(

                            onClick = {

                                val valorAtual =
                                    quantidadeTexto
                                        .toIntOrNull()
                                        ?: 1

                                quantidadeTexto =
                                    (valorAtual - 1)
                                        .coerceAtLeast(1)
                                        .toString()
                            }

                        ) {

                            Text(
                                "−",

                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium
                            )
                        }

                        OutlinedTextField(

                            value =
                                quantidadeTexto,

                            onValueChange = { novoValor ->

                                if (
                                    novoValor.all {
                                        it.isDigit()
                                    }
                                ) {

                                    quantidadeTexto =
                                        novoValor
                                }

                                mensagem = ""
                            },

                            modifier =
                                Modifier.width(110.dp),

                            label = {
                                Text("Quantidade")
                            },

                            singleLine = true,

                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Number
                                )
                        )

                        TextButton(

                            onClick = {

                                val valorAtual =
                                    quantidadeTexto
                                        .toIntOrNull()
                                        ?: 0

                                quantidadeTexto =
                                    (
                                            valorAtual + 1
                                            ).toString()
                            }

                        ) {

                            Text(
                                "+",

                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium
                            )
                        }
                    }

                    OutlinedTextField(

                        value =
                            observacao,

                        onValueChange = {

                            observacao = it
                            mensagem = ""
                        },

                        label = {
                            Text("Observação")
                        },

                        placeholder = {
                            Text(
                                "Ex.: branca, vermelha, tamanho..."
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        minLines = 2,
                        maxLines = 4
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {
                        adicionarLinha()
                    }
                ) {

                    Text(
                        "ADICIONAR À ENCOMENDA"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        mostrarQuantidade =
                            false

                        artigoSelecionado =
                            null

                        quantidadeTexto =
                            "1"

                        precoUnitarioTexto =
                            ""

                        observacao =
                            ""
                    }

                ) {

                    Text("CANCELAR")
                }
            }
        )
    }

    if (fotoAmpliada != null) {

        Dialog(

            onDismissRequest = {
                fotoAmpliada = null
            },

            properties =
                DialogProperties(
                    usePlatformDefaultWidth =
                        false
                )

        ) {

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable {
                            fotoAmpliada = null
                        }

            ) {

                AsyncImage(

                    model =
                        if (
                            fotoAmpliada!!
                                .startsWith("/")
                        ) {

                            File(
                                fotoAmpliada!!
                            )

                        } else {

                            Uri.parse(
                                fotoAmpliada!!
                            )
                        },

                    contentDescription =
                        "Fotografia ampliada",

                    modifier =
                        Modifier.fillMaxSize(),

                    contentScale =
                        ContentScale.Fit
                )

                TextButton(

                    onClick = {
                        fotoAmpliada = null
                    },

                    modifier =
                        Modifier
                            .align(
                                Alignment.TopEnd
                            )
                            .padding(12.dp)

                ) {

                    Text(

                        text = "✕",

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtigoSelecaoCard(
    artigo: Artigo,
    onSelecionar: () -> Unit,
    onFotoClick: (String) -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onSelecionar()
                }

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (artigo.foto.isNotBlank()) {

                AsyncImage(

                    model =
                        if (
                            artigo.foto.startsWith("/")
                        ) {

                            File(
                                artigo.foto
                            )

                        } else {

                            Uri.parse(
                                artigo.foto
                            )
                        },

                    contentDescription =
                        "Fotografia de ${artigo.descricao}",

                    modifier =
                        Modifier
                            .size(70.dp)
                            .clickable {
                                onFotoClick(
                                    artigo.foto
                                )
                            },

                    contentScale =
                        ContentScale.Crop
                )

                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )
            }

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        artigo.codigo,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        artigo.descricao,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )

                if (
                    artigo.categoria.isNotBlank()
                ) {

                    Text(
                        "Categoria: " +
                                artigo.categoria
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "€ %.2f"
                            .format(
                                artigo.precoComIva
                            ) +
                                if (
                                    artigo.unidade
                                        .isNotBlank()
                                ) {
                                    " / ${artigo.unidade}"
                                } else {
                                    ""
                                },

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}
