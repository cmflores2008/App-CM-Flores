package pt.encomendas.cmflores.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DadosLojaScreen(
    onVoltar: () -> Unit
) {
    val context = LocalContext.current

    // Acedemos à memória rápida do telemóvel para ir buscar os dados gravados anteriormente
    val sharedPreferences = remember {
        context.getSharedPreferences("cmflores_prefs", Context.MODE_PRIVATE)
    }

    // Se não houver nada gravado, ele assume "CM FLORES" por defeito
    var nomeLoja by remember { mutableStateOf(sharedPreferences.getString("loja_nome", "CM FLORES") ?: "") }
    var moradaLoja by remember { mutableStateOf(sharedPreferences.getString("loja_morada", "") ?: "") }
    var nifLoja by remember { mutableStateOf(sharedPreferences.getString("loja_nif", "") ?: "") }
    var contactosLoja by remember { mutableStateOf(sharedPreferences.getString("loja_contactos", "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados da Empresa") },
                navigationIcon = {
                    TextButton(onClick = onVoltar) {
                        Text("← VOLTAR")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Estes dados vão aparecer no cabeçalho dos teus documentos em PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            OutlinedTextField(
                value = nomeLoja,
                onValueChange = { nomeLoja = it },
                label = { Text("Nome da Loja") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = moradaLoja,
                onValueChange = { moradaLoja = it },
                label = { Text("Morada") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = nifLoja,
                onValueChange = { nifLoja = it },
                label = { Text("NIF") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = contactosLoja,
                onValueChange = { contactosLoja = it },
                label = { Text("Telefone / Contactos") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    // Grava as alterações na memória rápida
                    sharedPreferences.edit().apply {
                        putString("loja_nome", nomeLoja.trim())
                        putString("loja_morada", moradaLoja.trim())
                        putString("loja_nif", nifLoja.trim())
                        putString("loja_contactos", contactosLoja.trim())
                        apply()
                    }
                    Toast.makeText(context, "✅ Dados guardados com sucesso!", Toast.LENGTH_SHORT).show()
                    onVoltar()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text("GUARDAR ALTERAÇÕES", fontSize = 16.sp)
            }
        }
    }
}
