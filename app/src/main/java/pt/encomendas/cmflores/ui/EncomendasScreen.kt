package pt.encomendas.cmflores.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.data.Encomenda

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncomendasScreen(
    onVoltar: () -> Unit,
    onNovaEncomenda: () -> Unit,
    onAbrirEncomenda: (Long) -> Unit
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val baseDados =
        DatabaseProvider.obterBaseDados(context)

    var encomendas by remember {
        mutableStateOf<List<Encomenda>>(emptyList())
    }

    var clientes by remember {
        mutableStateOf<List<Cliente>>(emptyList())
    }

    LaunchedEffect(Unit) {
        encomendas =
            baseDados
                .encomendaDao()
                .obterTodas()

        clientes =
            baseDados
                .clienteDao()
                .obterTodos()
    }

    val clientesPorId =
        clientes.associateBy { cliente ->
            cliente.id
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Encomendas")
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
                onClick = onNovaEncomenda,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Text("+ NOVA ENCOMENDA")
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            if (encomendas.isEmpty()) {

                Text(
                    text =
                        "Não existem encomendas registadas.",
                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )

            } else {

                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = encomendas,
                        key = { encomenda ->
                            encomenda.id
                        }
                    ) { encomenda ->

                        EncomendaCard(
                            encomenda = encomenda,
                            cliente =
                                clientesPorId[
                                    encomenda.clienteId
                                ],
                            onClick = {
                                onAbrirEncomenda(
                                    encomenda.id
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EncomendaCard(
    encomenda: Encomenda,
    cliente: Cliente?,
    onClick: () -> Unit
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {

            /*
             * Número da encomenda
             *
             * O campo numero já contém a designação
             * da encomenda, por exemplo:
             * "Encomenda nº 123"
             *
             * Por isso não acrescentamos novamente
             * a palavra "Encomenda".
             */
            Text(
                text =
                    encomenda.numero,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
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
                encomenda.dataRecolha.isNotBlank()
            ) {
                Text(
                    text =
                        "Recolha: " +
                                encomenda.dataRecolha
                )
            }

            if (
                encomenda.dataEntrega.isNotBlank()
            ) {
                Text(
                    text =
                        "Entrega: " +
                                encomenda.dataEntrega
                )
            }

            Text(
                text =
                    "Estado: ${encomenda.estado}",
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Total: %.2f €"
                        .format(
                            encomenda.total
                        )
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text =
                    "Toque para consultar os detalhes",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}
