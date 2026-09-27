package pt.encomendas.cmflores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.encomendas.cmflores.ui.ArtigosScreen
import pt.encomendas.cmflores.ui.ClientesScreen
import pt.encomendas.cmflores.ui.EncomendaDetalheScreen
import pt.encomendas.cmflores.ui.EncomendaEditarScreen
import pt.encomendas.cmflores.ui.EncomendaFormScreen
import pt.encomendas.cmflores.ui.EncomendasScreen
import pt.encomendas.cmflores.ui.theme.EncomendasDeFloresTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContent {

            EncomendasDeFloresTheme {

                Surface(
                    modifier =
                        Modifier.fillMaxSize(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .background
                ) {

                    EcranPrincipal()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPrincipal() {

    var mostrarArtigos by remember {
        mutableStateOf(false)
    }

    var mostrarClientes by remember {
        mutableStateOf(false)
    }

    var mostrarEncomendas by remember {
        mutableStateOf(false)
    }

    var mostrarNovaEncomenda by remember {
        mutableStateOf(false)
    }

    var encomendaSelecionadaId by remember {
        mutableStateOf<Long?>(null)
    }

    var encomendaEmEdicaoId by remember {
        mutableStateOf<Long?>(null)
    }

    // ---------------------------------------------------------
    // EDIÇÃO DE ENCOMENDA
    // ---------------------------------------------------------
    if (encomendaEmEdicaoId != null) {

        EncomendaEditarScreen(

            encomendaId =
                encomendaEmEdicaoId!!,

            onVoltar = {

                encomendaEmEdicaoId =
                    null

                mostrarEncomendas =
                    true
            },

            onGuardada = {

                encomendaSelecionadaId =
                    encomendaEmEdicaoId

                encomendaEmEdicaoId =
                    null
            }
        )

        // ---------------------------------------------------------
        // DETALHE DA ENCOMENDA
        // ---------------------------------------------------------
    } else if (encomendaSelecionadaId != null) {

        EncomendaDetalheScreen(

            encomendaId =
                encomendaSelecionadaId!!,

            onVoltar = {

                encomendaSelecionadaId =
                    null

                mostrarEncomendas =
                    true
            },

            onEditar = {

                encomendaEmEdicaoId =
                    encomendaSelecionadaId

                encomendaSelecionadaId =
                    null
            }
        )

        // ---------------------------------------------------------
        // NOVA ENCOMENDA
        // ---------------------------------------------------------
    } else if (mostrarNovaEncomenda) {

        EncomendaFormScreen(

            onVoltar = {
                mostrarNovaEncomenda =
                    false
            },

            onGuardada = {

                mostrarNovaEncomenda =
                    false

                mostrarEncomendas =
                    true
            }
        )

        // ---------------------------------------------------------
        // LISTA DE ENCOMENDAS
        // ---------------------------------------------------------
    } else if (mostrarEncomendas) {

        EncomendasScreen(

            onVoltar = {
                mostrarEncomendas =
                    false
            },

            onNovaEncomenda = {

                mostrarEncomendas =
                    false

                mostrarNovaEncomenda =
                    true
            },

            onAbrirEncomenda = { id ->

                encomendaSelecionadaId =
                    id

                mostrarEncomendas =
                    false
            }
        )

        // ---------------------------------------------------------
        // CLIENTES
        // ---------------------------------------------------------
    } else if (mostrarClientes) {

        ClientesScreen(

            onVoltar = {
                mostrarClientes =
                    false
            }
        )

        // ---------------------------------------------------------
        // ARTIGOS
        // ---------------------------------------------------------
    } else if (mostrarArtigos) {

        ArtigosScreen(

            onVoltar = {
                mostrarArtigos =
                    false
            }
        )

        // ---------------------------------------------------------
        // MENU PRINCIPAL
        // ---------------------------------------------------------
    } else {

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Top
        ) {

            Spacer(
                modifier =
                    Modifier.height(30.dp)
            )

            Text(
                text = "🌸",
                fontSize = 50.sp
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Text(
                text =
                    "ENCOMENDAS DE FLORES",
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text =
                    "Carlos Melo - Comércio de flores, Lda.",
                fontSize = 14.sp
            )

            Spacer(
                modifier =
                    Modifier.height(25.dp)
            )

            BotaoMenu(
                texto =
                    "🌷  Nova Encomenda",
                onClick = {
                    mostrarNovaEncomenda =
                        true
                }
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            BotaoMenu(
                texto =
                    "📋  Encomendas",
                onClick = {
                    mostrarEncomendas =
                        true
                }
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                BotaoMenu(
                    texto =
                        "👥  Clientes",

                    modifier =
                        Modifier.weight(1f),

                    onClick = {
                        mostrarClientes =
                            true
                    }
                )

                BotaoMenu(
                    texto =
                        "🌹  Artigos",

                    modifier =
                        Modifier.weight(1f),

                    onClick = {
                        mostrarArtigos =
                            true
                    }
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            BotaoMenu(
                texto =
                    "📊  Dashboard"
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            BotaoMenu(
                texto =
                    "⚙️  Configurações"
            )
        }
    }
}

@Composable
fun BotaoMenu(

    texto: String,

    modifier: Modifier =
        Modifier.fillMaxWidth(),

    onClick: () -> Unit = {}

) {

    Button(

        onClick = onClick,

        modifier =
            modifier.height(55.dp)

    ) {

        Text(
            text = texto,
            fontSize = 16.sp
        )
    }
}

