package pt.encomendas.cmflores.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.ImportacaoClientesScreen
import java.text.Normalizer

private fun normalizarPesquisa(texto: String): String {

    return Normalizer
        .normalize(
            texto.lowercase(),
            Normalizer.Form.NFD
        )
        .replace(
            Regex("\\p{InCombiningDiacriticalMarks}+"),
            ""
        )
}

@androidx.compose.material3.ExperimentalMaterial3Api
@androidx.compose.runtime.Composable
fun ClientesScreen(
    onVoltar: () -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    val clienteDao =
        DatabaseProvider
            .obterBaseDados(context)
            .clienteDao()

    val coroutineScope = rememberCoroutineScope()

    var clientes by remember {
        mutableStateOf<List<Cliente>>(emptyList())
    }

    var pesquisa by remember {
        mutableStateOf("")
    }

    var mostrarFormulario by remember {
        mutableStateOf(false)
    }

    var mostrarImportacao by remember {
        mutableStateOf(false)
    }

    var clienteSelecionado by remember {
        mutableStateOf<Cliente?>(null)
    }

    var mostrarConfirmacaoEliminar by remember {
        mutableStateOf(false)
    }

    var mensagem by remember {
        mutableStateOf("")
    }

    var codigo by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var morada by remember { mutableStateOf("") }
    var codigoPostal by remember { mutableStateOf("") }
    var localidade by remember { mutableStateOf("") }
    var numContribuinte by remember { mutableStateOf("") }
    var contacto by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var telemovel by remember { mutableStateOf("") }
    var codPagamento by remember { mutableStateOf("") }
    var formaPagamento by remember { mutableStateOf("") }
    var codVendedor by remember { mutableStateOf("") }
    var vendedor by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        clientes = clienteDao.obterTodos()
    }

    val pesquisaNormalizada =
        normalizarPesquisa(
            pesquisa.trim()
        )

    val clientesFiltrados =
        clientes.filter { cliente ->

            normalizarPesquisa(
                cliente.codigo
            ).contains(
                pesquisaNormalizada
            ) ||

                    normalizarPesquisa(
                        cliente.nome
                    ).contains(
                        pesquisaNormalizada
                    ) ||

                    normalizarPesquisa(
                        cliente.numContribuinte
                    ).contains(
                        pesquisaNormalizada
                    )
        }

    fun recarregarClientes() {
        coroutineScope.launch {
            clientes = clienteDao.obterTodos()
        }
    }

    fun novoCliente() {

        clienteSelecionado = null

        codigo = ""
        nome = ""
        morada = ""
        codigoPostal = ""
        localidade = ""
        numContribuinte = ""
        contacto = ""
        telefone = ""
        email = ""
        telemovel = ""
        codPagamento = ""
        formaPagamento = ""
        codVendedor = ""
        vendedor = ""

        mensagem = ""
        mostrarFormulario = true
    }

    fun editarCliente(cliente: Cliente) {

        clienteSelecionado = cliente

        codigo = cliente.codigo
        nome = cliente.nome
        morada = cliente.morada
        codigoPostal = cliente.codigoPostal
        localidade = cliente.localidade
        numContribuinte = cliente.numContribuinte
        contacto = cliente.contacto
        telefone = cliente.telefone
        email = cliente.email
        telemovel = cliente.telemovel
        codPagamento = cliente.codPagamento
        formaPagamento = cliente.formaPagamento
        codVendedor = cliente.codVendedor
        vendedor = cliente.vendedor

        mensagem = ""
        mostrarFormulario = true
    }

    fun guardarCliente() {

        if (nome.isBlank()) {

            mensagem =
                "⚠️ O nome do cliente é obrigatório."

            return
        }

        if (codigo.isNotBlank()) {

            val codigoDuplicado =
                clientes.any { cliente ->

                    cliente.codigo.equals(
                        codigo.trim(),
                        ignoreCase = true
                    ) &&
                            cliente.id !=
                            (clienteSelecionado?.id ?: 0)
                }

            if (codigoDuplicado) {

                mensagem =
                    "⚠️ Já existe um cliente com o código \"$codigo\"."

                return
            }
        }

        val cliente = Cliente(

            id =
                clienteSelecionado?.id
                    ?: 0,

            nome =
                nome.trim(),

            codigo =
                codigo.trim(),

            morada =
                morada.trim(),

            codigoPostal =
                codigoPostal.trim(),

            localidade =
                localidade.trim(),

            numContribuinte =
                numContribuinte.trim(),

            contacto =
                contacto.trim(),

            telefone =
                telefone.trim(),

            email =
                email.trim(),

            telemovel =
                telemovel.trim(),

            codPagamento =
                codPagamento.trim(),

            formaPagamento =
                formaPagamento.trim(),

            codVendedor =
                codVendedor.trim(),

            vendedor =
                vendedor.trim()
        )

        coroutineScope.launch {

            try {

                if (cliente.id == 0L) {

                    clienteDao.inserir(cliente)

                } else {

                    clienteDao.atualizar(cliente)
                }

                clientes =
                    clienteDao.obterTodos()

                mensagem = ""

                mostrarFormulario = false

                clienteSelecionado = null

            } catch (e: Exception) {

                mensagem =
                    "❌ Erro ao guardar cliente: ${e.message}"
            }
        }
    }

    if (mostrarImportacao) {

        ImportacaoClientesScreen(

            onVoltar = {

                mostrarImportacao = false
                recarregarClientes()
            },

            onImportacaoConcluida = {

                recarregarClientes()
            }
        )

        return
    }

    if (mostrarFormulario) {

        Scaffold(

            topBar = {

                TopAppBar(

                    title = {

                        Text(
                            if (clienteSelecionado == null)
                                "Novo cliente"
                            else
                                "Editar cliente"
                        )
                    },

                    navigationIcon = {

                        TextButton(

                            onClick = {

                                mostrarFormulario = false
                                clienteSelecionado = null
                                mensagem = ""
                            }
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
                        .padding(16.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                // ─────────────────────────────
                // DADOS DO CLIENTE
                // ─────────────────────────────

                Text(
                    text = "👤 DADOS DO CLIENTE",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(

                    value = codigo,

                    onValueChange = {
                        codigo = it
                    },

                    label = {
                        Text("Código")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = nome,

                    onValueChange = {
                        nome = it
                    },

                    label = {
                        Text("Nome *")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = numContribuinte,

                    onValueChange = {
                        numContribuinte = it
                    },

                    label = {
                        Text("N.º Contribuinte")
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // ─────────────────────────────
                // MORADA
                // ─────────────────────────────

                Text(
                    text = "🏠 MORADA",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(

                    value = morada,

                    onValueChange = {
                        morada = it
                    },

                    label = {
                        Text("Morada")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = codigoPostal,

                    onValueChange = {
                        codigoPostal = it
                    },

                    label = {
                        Text("Código Postal")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = localidade,

                    onValueChange = {
                        localidade = it
                    },

                    label = {
                        Text("Localidade")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // ─────────────────────────────
                // CONTACTOS
                // ─────────────────────────────

                Text(
                    text = "📞 CONTACTOS",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(

                    value = contacto,

                    onValueChange = {
                        contacto = it
                    },

                    label = {
                        Text("Contacto")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = telefone,

                    onValueChange = {
                        telefone = it
                    },

                    label = {
                        Text("Telefone")
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Phone
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = telemovel,

                    onValueChange = {
                        telemovel = it
                    },

                    label = {
                        Text("Telemóvel")
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Phone
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = email,

                    onValueChange = {
                        email = it
                    },

                    label = {
                        Text("Email")
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Email
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // ─────────────────────────────
                // COMERCIAL
                // ─────────────────────────────

                Text(
                    text = "💼 COMERCIAL",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(

                    value = codPagamento,

                    onValueChange = {
                        codPagamento = it
                    },

                    label = {
                        Text("Cód. Pagamento")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = formaPagamento,

                    onValueChange = {
                        formaPagamento = it
                    },

                    label = {
                        Text("Forma de Pagamento")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = codVendedor,

                    onValueChange = {
                        codVendedor = it
                    },

                    label = {
                        Text("Cód. Vendedor")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = vendedor,

                    onValueChange = {
                        vendedor = it
                    },

                    label = {
                        Text("Vendedor")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                if (mensagem.isNotBlank()) {

                    Text(
                        text = mensagem,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Button(

                    onClick = {
                        guardarCliente()
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("GUARDAR")
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }
        }

        return
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Clientes")
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
                    .padding(16.dp)
        ) {

            Button(

                onClick = {
                    novoCliente()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("+ NOVO CLIENTE")
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Button(

                onClick = {

                    mensagem = ""
                    mostrarImportacao = true
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text("📥 IMPORTAR CLIENTES")
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            OutlinedTextField(

                value = pesquisa,

                onValueChange = {
                    pesquisa = it
                },

                label = {
                    Text(
                        "Pesquisar por código, nome ou NIF"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (clientesFiltrados.isEmpty()) {

                Text(

                    text =
                        if (pesquisa.isBlank())
                            "Não existem clientes registados."
                        else
                            "Nenhum cliente encontrado.",

                    style =
                        MaterialTheme.typography.bodyLarge
                )

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(

                        items =
                            clientesFiltrados,

                        key = {
                            it.id
                        }

                    ) { cliente ->

                        ClienteCard(

                            cliente = cliente,

                            onEditar = {
                                editarCliente(cliente)
                            },

                            onEliminar = {

                                clienteSelecionado =
                                    cliente

                                mostrarConfirmacaoEliminar =
                                    true
                            }
                        )
                    }
                }
            }
        }
    }

    if (mostrarConfirmacaoEliminar) {

        AlertDialog(

            onDismissRequest = {

                mostrarConfirmacaoEliminar =
                    false
            },

            title = {
                Text("Eliminar cliente")
            },

            text = {

                Text(
                    "Tem a certeza que pretende eliminar o cliente \"${clienteSelecionado?.nome ?: ""}\"?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        val cliente =
                            clienteSelecionado
                                ?: return@TextButton

                        mostrarConfirmacaoEliminar =
                            false

                        coroutineScope.launch {

                            try {

                                clienteDao.eliminar(cliente)

                                clientes =
                                    clienteDao.obterTodos()

                                clienteSelecionado = null

                            } catch (e: Exception) {

                                mensagem =
                                    "❌ Erro ao eliminar cliente: ${e.message}"
                            }
                        }
                    }
                ) {

                    Text(
                        "ELIMINAR",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        mostrarConfirmacaoEliminar =
                            false
                    }
                ) {

                    Text("CANCELAR")
                }
            }
        )
    }
}

@androidx.compose.runtime.Composable
private fun ClienteCard(

    cliente: Cliente,

    onEditar: () -> Unit,

    onEliminar: () -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth()

    ) {

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            Text(

                text =
                    cliente.nome,

                style =
                    MaterialTheme.typography.titleMedium,

                modifier =
                    Modifier.fillMaxWidth()
            )

            if (cliente.codigo.isNotBlank()) {

                Text(
                    text =
                        "Código: ${cliente.codigo}"
                )
            }

            if (cliente.numContribuinte.isNotBlank()) {

                Text(
                    text =
                        "NIF: ${cliente.numContribuinte}"
                )
            }

            if (cliente.morada.isNotBlank()) {

                Text(
                    text =
                        cliente.morada
                )
            }

            if (cliente.codigoPostal.isNotBlank()) {

                Text(
                    text =
                        cliente.codigoPostal
                )
            }

            if (cliente.localidade.isNotBlank()) {

                Text(
                    text =
                        cliente.localidade
                )
            }

            if (cliente.telefone.isNotBlank()) {

                Text(
                    text =
                        "Telefone: ${cliente.telefone}"
                )
            }

            if (cliente.telemovel.isNotBlank()) {

                Text(
                    text =
                        "Telemóvel: ${cliente.telemovel}"
                )
            }

            if (cliente.email.isNotBlank()) {

                Text(
                    text =
                        "Email: ${cliente.email}"
                )
            }

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            HorizontalDivider()

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.End,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TextButton(
                    onClick = onEditar
                ) {

                    Text("EDITAR")
                }

                TextButton(
                    onClick = onEliminar
                ) {

                    Text(
                        "ELIMINAR",
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}


