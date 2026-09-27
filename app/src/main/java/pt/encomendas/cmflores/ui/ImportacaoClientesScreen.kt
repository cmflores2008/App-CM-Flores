package pt.encomendas.cmflores

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import pt.encomendas.cmflores.data.Cliente
import pt.encomendas.cmflores.data.DatabaseProvider
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale

@Composable
fun ImportacaoClientesScreen(
    onVoltar: () -> Unit,
    onImportacaoConcluida: () -> Unit = {}
) {

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val scope =
        rememberCoroutineScope()

    var ficheiroSelecionado by remember {
        mutableStateOf<Uri?>(null)
    }

    var nomeFicheiro by remember {
        mutableStateOf("")
    }

    var importacaoEmCurso by remember {
        mutableStateOf(false)
    }

    var resultado by remember {
        mutableStateOf("")
    }

    val selecionarFicheiro =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                ficheiroSelecionado = uri

                nomeFicheiro =
                    obterNomeFicheiro(
                        context = context,
                        uri = uri
                    )

                resultado = ""
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
    ) {

        Text(
            text = "📥 IMPORTAR CLIENTES",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

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
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Ficheiro Excel",
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Text(
                    text =
                        if (nomeFicheiro.isNotBlank()) {
                            nomeFicheiro
                        } else {
                            "Nenhum ficheiro selecionado"
                        }
                )

                Button(

                    onClick = {

                        selecionarFicheiro.launch(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                    },

                    enabled =
                        !importacaoEmCurso,

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text("📂 SELECIONAR EXCEL")
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        )
                        .padding(16.dp)
            ) {

                if (resultado.isBlank()) {

                    Text(
                        text = """
                            O ficheiro Excel deve conter as seguintes colunas:

                            Nome
                            Codigo
                            Morada
                            Código Postal
                            Localidade
                            Num.Contribuinte
                            Contacto
                            Telefone
                            Email
                            Telemovel
                            Cód. Pagamento
                            Forma de Pagamento
                            Cód. Vendedor
                            Vendedor

                            O Código será utilizado para determinar se o cliente já existe.

                            • Código inexistente → novo cliente
                            • Código existente → atualização do cliente
                            • Linhas sem Nome ou Código → ignoradas

                            Quando uma célula do Excel estiver vazia durante
                            uma atualização, o valor existente na app será mantido.
                        """.trimIndent()
                    )

                } else {

                    Text(
                        text = resultado,
                        style =
                            MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(

                onClick = onVoltar,

                enabled =
                    !importacaoEmCurso,

                modifier =
                    Modifier.weight(1f)
            ) {

                Text("← VOLTAR")
            }

            Button(

                onClick = {

                    val uri =
                        ficheiroSelecionado

                    if (uri == null) {

                        resultado =
                            "⚠️ Selecione primeiro um ficheiro Excel."

                    } else {

                        scope.launch {

                            importacaoEmCurso = true

                            resultado =
                                "⏳ A importar clientes..."

                            val textoResultado =
                                importarClientes(
                                    context = context,
                                    uri = uri
                                )

                            resultado =
                                textoResultado

                            importacaoEmCurso =
                                false

                            if (
                                textoResultado.startsWith(
                                    "✅ IMPORTAÇÃO CONCLUÍDA"
                                )
                            ) {

                                onImportacaoConcluida()
                            }
                        }
                    }
                },

                enabled =
                    ficheiroSelecionado != null &&
                            !importacaoEmCurso,

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    if (importacaoEmCurso) {
                        "⏳ A IMPORTAR..."
                    } else {
                        "📥 IMPORTAR"
                    }
                )
            }
        }
    }
}

/* ============================================================
   IMPORTAÇÃO
   ============================================================ */

private suspend fun importarClientes(
    context: Context,
    uri: Uri
): String {

    var clientesLidos = 0
    var clientesNovos = 0
    var clientesAtualizados = 0
    var linhasIgnoradas = 0
    var erros = 0

    var inputStream: InputStream? = null
    var workbook: XSSFWorkbook? = null

    try {

        inputStream =
            context.contentResolver.openInputStream(uri)

        if (inputStream == null) {

            return "❌ Não foi possível abrir o ficheiro Excel."
        }

        workbook =
            XSSFWorkbook(inputStream)

        if (workbook.numberOfSheets == 0) {

            return "❌ O ficheiro Excel não contém folhas."
        }

        val sheet =
            workbook.getSheetAt(0)

        if (sheet.physicalNumberOfRows < 2) {

            return "❌ O ficheiro Excel não contém dados de clientes."
        }

        val primeiraLinha =
            sheet.getRow(0)

        if (primeiraLinha == null) {

            return "❌ Não foi possível encontrar a linha dos cabeçalhos."
        }

        val formatter =
            DataFormatter()

        val colunas =
            mutableMapOf<String, Int>()

        for (cell in primeiraLinha) {

            val texto =
                formatter
                    .formatCellValue(cell)
                    .trim()

            if (texto.isNotBlank()) {

                val chave =
                    normalizarCabecalho(texto)

                colunas[chave] =
                    cell.columnIndex
            }
        }

        // =====================================================
        // COLUNAS
        // =====================================================

        val colunaNome =
            encontrarColuna(
                colunas,
                "nome"
            )

        val colunaCodigo =
            encontrarColuna(
                colunas,
                "codigo",
                "cod"
            )

        val colunaMorada =
            encontrarColuna(
                colunas,
                "morada"
            )

        val colunaCodigoPostal =
            encontrarColuna(
                colunas,
                "codigopostal",
                "codpostal",
                "postal",
                "cp"
            )

        /*
         * Compatibilidade com o antigo Excel.
         *
         * Se ainda existir uma coluna "Localidade Postal",
         * será usada como localidade caso não exista uma coluna
         * "Localidade" separada.
         */
        val colunaLocalidade =
            encontrarColuna(
                colunas,
                "localidade"
            )

        val colunaLocalidadePostal =
            encontrarColuna(
                colunas,
                "localidadepostal"
            )

        val colunaNumeroContribuinte =
            encontrarColuna(
                colunas,
                "numcontribuinte",
                "numerocontribuinte",
                "nif",
                "contribuinte"
            )

        val colunaContacto =
            encontrarColuna(
                colunas,
                "contacto"
            )

        val colunaTelefone =
            encontrarColuna(
                colunas,
                "telefone"
            )

        val colunaEmail =
            encontrarColuna(
                colunas,
                "email"
            )

        val colunaTelemovel =
            encontrarColuna(
                colunas,
                "telemovel",
                "telemovel"
            )

        val colunaCodPagamento =
            encontrarColuna(
                colunas,
                "codpagamento",
                "codigopagamento",
                "codpag"
            )

        val colunaFormaPagamento =
            encontrarColuna(
                colunas,
                "formadepagamento",
                "formapagamento",
                "pagamento"
            )

        val colunaCodVendedor =
            encontrarColuna(
                colunas,
                "codvendedor",
                "codigovendedor"
            )

        val colunaVendedor =
            encontrarColuna(
                colunas,
                "vendedor"
            )

        // =====================================================
        // COLUNAS OBRIGATÓRIAS
        // =====================================================

        if (colunaNome == null) {

            return "❌ A coluna 'Nome' não foi encontrada no Excel."
        }

        if (colunaCodigo == null) {

            return "❌ A coluna 'Codigo' não foi encontrada no Excel."
        }

        val dao =
            DatabaseProvider
                .obterBaseDados(context)
                .clienteDao()

        // =====================================================
        // LINHAS
        // =====================================================

        for (
        numeroLinha
        in 1..sheet.lastRowNum
        ) {

            val row =
                sheet.getRow(numeroLinha)

            if (row == null) {

                linhasIgnoradas++
                continue
            }

            try {

                val nome =
                    obterTextoCelula(
                        row,
                        colunaNome,
                        formatter
                    )

                val codigo =
                    obterTextoCelula(
                        row,
                        colunaCodigo,
                        formatter
                    )

                if (
                    nome.isBlank() ||
                    codigo.isBlank()
                ) {

                    linhasIgnoradas++
                    continue
                }

                clientesLidos++

                val morada =
                    obterTextoCelula(
                        row,
                        colunaMorada,
                        formatter
                    )

                val codigoPostal =
                    obterTextoCelula(
                        row,
                        colunaCodigoPostal,
                        formatter
                    )

                /*
                 * Se existir a nova coluna "Localidade",
                 * usamos essa.
                 *
                 * Se não existir, mas existir "Localidade Postal",
                 * usamos o conteúdo antigo como localidade.
                 */
                val localidade =
                    if (colunaLocalidade != null) {

                        obterTextoCelula(
                            row,
                            colunaLocalidade,
                            formatter
                        )

                    } else {

                        obterTextoCelula(
                            row,
                            colunaLocalidadePostal,
                            formatter
                        )
                    }

                val numContribuinte =
                    obterTextoCelula(
                        row,
                        colunaNumeroContribuinte,
                        formatter
                    )

                val contacto =
                    obterTextoCelula(
                        row,
                        colunaContacto,
                        formatter
                    )

                val telefone =
                    obterTextoCelula(
                        row,
                        colunaTelefone,
                        formatter
                    )

                val email =
                    obterTextoCelula(
                        row,
                        colunaEmail,
                        formatter
                    )

                val telemovel =
                    obterTextoCelula(
                        row,
                        colunaTelemovel,
                        formatter
                    )

                val codPagamento =
                    obterTextoCelula(
                        row,
                        colunaCodPagamento,
                        formatter
                    )

                val formaPagamento =
                    obterTextoCelula(
                        row,
                        colunaFormaPagamento,
                        formatter
                    )

                val codVendedor =
                    obterTextoCelula(
                        row,
                        colunaCodVendedor,
                        formatter
                    )

                val vendedor =
                    obterTextoCelula(
                        row,
                        colunaVendedor,
                        formatter
                    )

                // =================================================
                // PROCURAR CLIENTE PELO CÓDIGO
                // =================================================

                val existente =
                    dao.obterPorCodigo(codigo)

                // =================================================
                // NOVO CLIENTE
                // =================================================

                if (existente == null) {

                    val novoCliente =
                        Cliente(

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

                    dao.inserir(
                        novoCliente
                    )

                    clientesNovos++

                } else {

                    // =================================================
                    // ATUALIZAÇÃO
                    // =================================================

                    /*
                     * Quando uma célula do Excel está vazia,
                     * mantemos o valor que já existe na app.
                     */

                    val clienteAtualizado =
                        existente.copy(

                            nome =
                                if (
                                    nome.isNotBlank()
                                ) {
                                    nome.trim()
                                } else {
                                    existente.nome
                                },

                            codigo =
                                existente.codigo,

                            morada =
                                if (
                                    morada.isNotBlank()
                                ) {
                                    morada.trim()
                                } else {
                                    existente.morada
                                },

                            codigoPostal =
                                if (
                                    codigoPostal.isNotBlank()
                                ) {
                                    codigoPostal.trim()
                                } else {
                                    existente.codigoPostal
                                },

                            localidade =
                                if (
                                    localidade.isNotBlank()
                                ) {
                                    localidade.trim()
                                } else {
                                    existente.localidade
                                },

                            numContribuinte =
                                if (
                                    numContribuinte.isNotBlank()
                                ) {
                                    numContribuinte.trim()
                                } else {
                                    existente.numContribuinte
                                },

                            contacto =
                                if (
                                    contacto.isNotBlank()
                                ) {
                                    contacto.trim()
                                } else {
                                    existente.contacto
                                },

                            telefone =
                                if (
                                    telefone.isNotBlank()
                                ) {
                                    telefone.trim()
                                } else {
                                    existente.telefone
                                },

                            email =
                                if (
                                    email.isNotBlank()
                                ) {
                                    email.trim()
                                } else {
                                    existente.email
                                },

                            telemovel =
                                if (
                                    telemovel.isNotBlank()
                                ) {
                                    telemovel.trim()
                                } else {
                                    existente.telemovel
                                },

                            codPagamento =
                                if (
                                    codPagamento.isNotBlank()
                                ) {
                                    codPagamento.trim()
                                } else {
                                    existente.codPagamento
                                },

                            formaPagamento =
                                if (
                                    formaPagamento.isNotBlank()
                                ) {
                                    formaPagamento.trim()
                                } else {
                                    existente.formaPagamento
                                },

                            codVendedor =
                                if (
                                    codVendedor.isNotBlank()
                                ) {
                                    codVendedor.trim()
                                } else {
                                    existente.codVendedor
                                },

                            vendedor =
                                if (
                                    vendedor.isNotBlank()
                                ) {
                                    vendedor.trim()
                                } else {
                                    existente.vendedor
                                }
                        )

                    dao.atualizar(
                        clienteAtualizado
                    )

                    clientesAtualizados++
                }

            } catch (_: Exception) {

                erros++
            }
        }

        return """
            ✅ IMPORTAÇÃO CONCLUÍDA

            👥 Clientes lidos: $clientesLidos

            ➕ Clientes novos: $clientesNovos

            🔄 Clientes atualizados: $clientesAtualizados

            ⚠️ Linhas ignoradas: $linhasIgnoradas

            ❌ Erros: $erros
        """.trimIndent()

    } catch (e: Exception) {

        return """
            ❌ ERRO NA IMPORTAÇÃO

            ${e.message ?: "Erro desconhecido."}

            Clientes lidos: $clientesLidos
            Novos: $clientesNovos
            Atualizados: $clientesAtualizados
            Ignorados: $linhasIgnoradas
            Erros: $erros
        """.trimIndent()

    } finally {

        try {
            workbook?.close()
        } catch (_: Exception) {
        }

        try {
            inputStream?.close()
        } catch (_: Exception) {
        }
    }
}

/* ============================================================
   CABEÇALHOS
   ============================================================ */

private fun normalizarCabecalho(
    texto: String
): String {

    val semAcentos =
        Normalizer
            .normalize(
                texto.lowercase(
                    Locale.getDefault()
                ),
                Normalizer.Form.NFD
            )
            .replace(
                Regex(
                    "\\p{InCombiningDiacriticalMarks}+"
                ),
                ""
            )

    return semAcentos
        .replace(
            Regex("[^a-z0-9]"),
            ""
        )
}

private fun encontrarColuna(
    colunas: Map<String, Int>,
    vararg nomes: String
): Int? {

    for (nome in nomes) {

        val chave =
            normalizarCabecalho(nome)

        val indice =
            colunas[chave]

        if (indice != null) {
            return indice
        }
    }

    return null
}

/* ============================================================
   CÉLULAS EXCEL
   ============================================================ */

private fun obterTextoCelula(
    row: org.apache.poi.ss.usermodel.Row,
    coluna: Int?,
    formatter: DataFormatter
): String {

    if (coluna == null) {
        return ""
    }

    val cell =
        row.getCell(
            coluna,
            org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL
        )
            ?: return ""

    return try {

        when (cell.cellType) {

            CellType.FORMULA -> {

                formatter
                    .formatCellValue(cell)
                    .trim()
            }

            CellType.NUMERIC -> {

                formatter
                    .formatCellValue(cell)
                    .trim()
            }

            CellType.STRING -> {

                cell
                    .stringCellValue
                    .trim()
            }

            CellType.BOOLEAN -> {

                cell
                    .booleanCellValue
                    .toString()
            }

            else -> {

                formatter
                    .formatCellValue(cell)
                    .trim()
            }
        }

    } catch (_: Exception) {

        try {

            formatter
                .formatCellValue(cell)
                .trim()

        } catch (_: Exception) {

            ""
        }
    }
}

/* ============================================================
   NOME DO FICHEIRO
   ============================================================ */

private fun obterNomeFicheiro(
    context: Context,
    uri: Uri
): String {

    var nome = ""

    try {

        val cursor =
            context.contentResolver.query(
                uri,
                arrayOf(
                    android.provider.OpenableColumns.DISPLAY_NAME
                ),
                null,
                null,
                null
            )

        cursor?.use {

            if (it.moveToFirst()) {

                val indice =
                    it.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )

                if (indice >= 0) {

                    nome =
                        it.getString(indice)
                }
            }
        }

    } catch (_: Exception) {
    }

    return if (nome.isNotBlank()) {

        nome

    } else {

        "Ficheiro Excel selecionado"
    }
}
