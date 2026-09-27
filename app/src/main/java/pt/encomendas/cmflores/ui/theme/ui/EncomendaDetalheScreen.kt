package pt.encomendas.cmflores.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Encomenda
import pt.encomendas.cmflores.data.LinhaEncomenda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendaDetalheScreen(
    encomendaId: Long,
    onVoltar: () -> Unit,
    onEditar: () -> Unit = {}
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val baseDados =
        DatabaseProvider.obterBaseDados(context)

    var encomenda by remember {
        mutableStateOf<Encomenda?>(null)
    }

    var cliente by remember {
        mutableStateOf<Cliente?>(null)
    }

    var linhas by remember {
        mutableStateOf<List<LinhaEncomenda>>(
            emptyList()
        )
    }

    var artigos by remember {
        mutableStateOf<List<Artigo>>(
            emptyList()
        )
    }

    LaunchedEffect(encomendaId) {

        val encomendaCarregada =
            baseDados
                .encomendaDao()
                .obterPorId(encomendaId)

        encomenda =
            encomendaCarregada

        if (encomendaCarregada != null) {

            cliente =
                baseDados
                    .clienteDao()
                    .obterPorId(
                        encomendaCarregada.clienteId
                    )

            linhas =
                baseDados
                    .linhaEncomendaDao()
                    .obterPorEncomenda(
                        encomendaId
                    )

            artigos =
                baseDados
                    .artigoDao()
                    .obterTodos()
        }
    }

    val artigosPorId =
        artigos.associateBy {
                artigo -> artigo.id
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(
                        "Detalhe da encomenda"
                    )
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVoltar
                    ) {

                        Text(
                            "← VOLTAR"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        if (encomenda == null) {

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .padding(16.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        "Encomenda não encontrada.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )
            }

        } else {

            val encomendaAtual =
                encomenda!!

            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
                        .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                item {

                    Button(
                        onClick = onEditar,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "✏️ EDITAR ENCOMENDA"
                        )
                    }
                }

                item {

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
                                Arrangement.spacedBy(
                                    6.dp
                                )
                        ) {

                            Text(
                                text =
                                    encomendaAtual
                                        .numero,

                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineSmall,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                text =
                                    "Cliente: ${
                                        cliente?.nome
                                            ?: "Cliente não encontrado"
                                    }"
                            )

                            if (
                                encomendaAtual
                                    .dataRecolha
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Data de recolha: " +
                                                encomendaAtual
                                                    .dataRecolha
                                )
                            }

                            if (
                                encomendaAtual
                                    .dataEntrega
                                    .isNotBlank()
                            ) {

                                Text(
                                    text =
                                        "Data de entrega: " +
                                                encomendaAtual
                                                    .dataEntrega
                                )
                            }

                            Text(
                                text =
                                    "Estado: " +
                                            encomendaAtual
                                                .estado
                            )
                        }
                    }
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "🌸 ARTIGOS",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.Bold
                    )
                }

                if (linhas.isEmpty()) {

                    item {

                        Text(
                            text =
                                "Não existem linhas nesta encomenda."
                        )
                    }

                } else {

                    items(
                        items = linhas,
                        key = {
                                linha -> linha.id
                        }
                    ) { linha ->

                        val artigo =
                            artigosPorId[
                                linha.artigoId
                            ]

                        LinhaDetalheCard(
                            linha = linha,
                            artigo = artigo
                        )
                    }
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Card(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),

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
                                            encomendaAtual
                                                .total
                                        ),

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LinhaDetalheCard(
    linha: LinhaEncomenda,
    artigo: Artigo?
) {

    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {

            Text(
                text =
                    artigo?.codigo
                        ?: "Artigo não encontrado",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    artigo?.descricao
                        ?: "Artigo não encontrado",

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Quantidade: " +
                            if (
                                linha.quantidade % 1.0 ==
                                0.0
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
                text =
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
                    text =
                        "Observação: " +
                                linha.observacao
                )
            }

            Text(
                text =
                    "Total: € %.2f"
                        .format(
                            linha.total
                        ),

                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

