package pt.encomendas.cmflores.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onVoltar: () -> Unit,
    onAbrirDadosLoja: () -> Unit // <-- Nova ação adicionada aqui!
) {
    val context = LocalContext.current
    val nomeDaBaseDeDados = "encomendas_flores.db"

    val criarDocumentoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { realizarBackup(context, nomeDaBaseDeDados, it) }
    }

    val abrirDocumentoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { restaurarBackup(context, nomeDaBaseDeDados, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Segurança e Dados", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Cria cópias de segurança regularmente para não perderes clientes, artigos ou encomendas.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val dataFormatada = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                            criarDocumentoLauncher.launch("Backup_CMFLORES_$dataFormatada.db")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("💾  FAZER CÓPIA DE SEGURANÇA", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            abrirDocumentoLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📂  RESTAURAR CÓPIA DE DADOS", fontSize = 16.sp)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Dados da Empresa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Configura o nome, NIF e morada que aparecem no cabeçalho dos teus PDFs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAbrirDadosLoja, // <-- Botão agora abre o novo ecrã!
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⚙️  EDITAR DADOS DA LOJA", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private fun realizarBackup(context: Context, dbName: String, uriDestino: Uri) {
    try {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) {
            Toast.makeText(context, "Base de dados não encontrada!", Toast.LENGTH_LONG).show()
            return
        }

        val inputStream = FileInputStream(dbFile)
        val outputStream = context.contentResolver.openOutputStream(uriDestino)

        if (outputStream != null) {
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Toast.makeText(context, "✅ Backup realizado com sucesso!", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "❌ Erro ao fazer backup: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun restaurarBackup(context: Context, dbName: String, uriOrigem: Uri) {
    try {
        val dbFile = context.getDatabasePath(dbName)
        val inputStream = context.contentResolver.openInputStream(uriOrigem)
        val outputStream = FileOutputStream(dbFile)

        if (inputStream != null) {
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            Toast.makeText(context, "✅ Backup restaurado! Por favor, fecha e volta a abrir a aplicação.", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "❌ Erro ao restaurar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
