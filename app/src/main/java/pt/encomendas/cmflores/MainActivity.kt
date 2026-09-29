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
import pt.encomendas.cmflores.ui.DashboardScreen
import pt.encomendas.cmflores.importacao.ImportacaoArtigosScreen
import pt.encomendas.cmflores.ui.EncomendaDetalheScreen
import pt.encomendas.cmflores.ui.EncomendaEditarScreen
import pt.encomendas.cmflores.ui.EncomendaFormScreen
import pt.encomendas.cmflores.ui.EncomendasScreen
import pt.encomendas.cmflores.ui.RelatorioDatasScreen
import pt.encomendas.cmflores.ui.ConfiguracoesScreen
import pt.encomendas.cmflores.ui.DadosLojaScreen
import pt.encomendas.cmflores.ui.theme.EncomendasDeFloresTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EncomendasDeFloresTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
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

    var mostrarArtigos by remember { mutableStateOf(false) }
    var mostrarClientes by remember { mutableStateOf(false) }
    var mostrarEncomendas by remember { mutableStateOf(false) }
    var mostrarNovaEncomenda by remember { mutableStateOf(false) }
    var mostrarDashboard by remember { mutableStateOf(false) }
    var mostrarImportacao by remember { mutableStateOf(false) }
    var mostrarConfiguracoes by remember { mutableStateOf(false) }
    var mostrarDadosLoja by remember { mutableStateOf(false) } // <-- Variável para o novo ecrã

    var mostrarRelatorioDatas by remember { mutableStateOf(false) }
    var dataInicioRel by remember { mutableStateOf("") }
    var dataFimRel by remember { mutableStateOf("") }

    var encomendaSelecionadaId by remember { mutableStateOf<Long?>(null) }
    var encomendaEmEdicaoId by remember { mutableStateOf<Long?>(null) }

    if (encomendaEmEdicaoId != null) {
        EncomendaEditarScreen(
            encomendaId = encomendaEmEdicaoId!!,
            onVoltar = {
                encomendaEmEdicaoId = null
                mostrarEncomendas = true
            },
            onGuardada = {
                encomendaSelecionadaId = encomendaEmEdicaoId
                encomendaEmEdicaoId = null
            }
        )
    } else if (encomendaSelecionadaId != null) {
        EncomendaDetalheScreen(
            encomendaId = encomendaSelecionadaId!!,
            onVoltar = {
                encomendaSelecionadaId = null
                mostrarEncomendas = true
            },
            onEditar = {
                encomendaEmEdicaoId = encomendaSelecionadaId
                encomendaSelecionadaId = null
            }
        )
    } else if (mostrarNovaEncomenda) {
        EncomendaFormScreen(
            onVoltar = { mostrarNovaEncomenda = false },
            onGuardada = {
                mostrarNovaEncomenda = false
                mostrarEncomendas = true
            }
        )
    } else if (mostrarEncomendas) {
        EncomendasScreen(
            onVoltar = { mostrarEncomendas = false },
            onNovaEncomenda = {
                mostrarEncomendas = false
                mostrarNovaEncomenda = true
            },
            onAbrirEncomenda = { id ->
                encomendaSelecionadaId = id
                mostrarEncomendas = false
            }
        )
    } else if (mostrarClientes) {
        ClientesScreen(
            onVoltar = { mostrarClientes = false }
        )
    } else if (mostrarArtigos) {
        ArtigosScreen(
            onVoltar = { mostrarArtigos = false }
        )
    } else if (mostrarRelatorioDatas) {
        RelatorioDatasScreen(
            dataInicio = dataInicioRel,
            dataFim = dataFimRel,
            onVoltar = {
                mostrarRelatorioDatas = false
                mostrarDashboard = true
            }
        )
    } else if (mostrarImportacao) {
        ImportacaoArtigosScreen(
            onVoltar = { mostrarImportacao = false }
        )
    } else if (mostrarDashboard) {
        DashboardScreen(
            onVoltar = { mostrarDashboard = false },
            onNavigateToEncomendas = {
                mostrarDashboard = false
                mostrarNovaEncomenda = true
            },
            onNavigateToClientes = {
                mostrarDashboard = false
                mostrarClientes = true
            },
            onNavigateToArtigos = {
                mostrarDashboard = false
                mostrarArtigos = true
            },
            onNavigateToImportacao = {
                mostrarDashboard = false
                mostrarImportacao = true
            },
            onNavigateToPesquisaDatas = { inicio, fim ->
                dataInicioRel = inicio
                dataFimRel = fim
                mostrarDashboard = false
                mostrarRelatorioDatas = true
            }
        )
        // ---------------------------------------------------------
        // DADOS DA LOJA
        // ---------------------------------------------------------
    } else if (mostrarDadosLoja) {
        DadosLojaScreen(
            onVoltar = {
                mostrarDadosLoja = false
                mostrarConfiguracoes = true // Volta para as Configurações
            }
        )
    } else if (mostrarConfiguracoes) {
        ConfiguracoesScreen(
            onVoltar = { mostrarConfiguracoes = false },
            onAbrirDadosLoja = {
                mostrarConfiguracoes = false
                mostrarDadosLoja = true
            }
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            Text(text = "🌸", fontSize = 50.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "ENCOMENDAS DE FLORES", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "Carlos Melo - Comércio de flores, Lda.", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(25.dp))

            BotaoMenu(texto = "🌷  Nova Encomenda", onClick = { mostrarNovaEncomenda = true })
            Spacer(modifier = Modifier.height(12.dp))
            BotaoMenu(texto = "📋  Encomendas", onClick = { mostrarEncomendas = true })
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotaoMenu(texto = "👥  Clientes", modifier = Modifier.weight(1f), onClick = { mostrarClientes = true })
                BotaoMenu(texto = "🌹  Artigos", modifier = Modifier.weight(1f), onClick = { mostrarArtigos = true })
            }
            Spacer(modifier = Modifier.height(12.dp))
            BotaoMenu(texto = "📊  Dashboard", onClick = { mostrarDashboard = true })
            Spacer(modifier = Modifier.height(12.dp))
            BotaoMenu(texto = "⚙️  Configurações", onClick = { mostrarConfiguracoes = true })
        }
    }
}

@Composable
fun BotaoMenu(
    texto: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(55.dp)
    ) {
        Text(text = texto, fontSize = 16.sp)
    }
}
