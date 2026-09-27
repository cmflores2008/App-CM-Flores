package pt.encomendas.cmflores.importacao

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.DatabaseProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Composable
fun ImportacaoArtigosScreen(
    onVoltar: () -> Unit
) {

    val context = LocalContext.current

    var pastaImportacaoSelecionada by remember {
        mutableStateOf<Uri?>(null)
    }

    var mensagem by remember {
        mutableStateOf(
            "Nenhuma pasta de importação selecionada."
        )
    }

    var importando by remember {
        mutableStateOf(false)
    }

    val seletorPastaImportacao =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                pastaImportacaoSelecionada = uri

                mensagem =
                    "📁 Pasta de importação selecionada."

            } else {

                pastaImportacaoSelecionada = null

                mensagem =
                    "Nenhuma pasta de importação selecionada."
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Text(
                text = "📦",
                fontSize = 50.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "IMPORTAR ARTIGOS",
                fontSize = 24.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Importação através do pacote USB",
                fontSize = 15.sp
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Button(
                onClick = {

                    seletorPastaImportacao.launch(null)

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !importando
            ) {

                Text(
                    text = "📁 ESCOLHER PASTA DE IMPORTAÇÃO"
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (pastaImportacaoSelecionada != null) {

                Text(
                    text = "✅ Pasta de importação selecionada"
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text =
                        "A pasta deve conter:\n" +
                                "• artigos.csv\n" +
                                "• fotografias/"
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            Text(
                text = mensagem
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            if (pastaImportacaoSelecionada != null) {

                Button(
                    onClick = {

                        val uriPasta =
                            pastaImportacaoSelecionada

                        if (uriPasta == null) {

                            mensagem =
                                "Nenhuma pasta de importação selecionada."

                            return@Button
                        }

                        importando = true

                        mensagem =
                            "⏳ A importar artigos e fotografias..."

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            val resultado =
                                importarPacote(
                                    context = context,
                                    uriPastaImportacao = uriPasta
                                )

                            withContext(
                                Dispatchers.Main
                            ) {

                                importando = false

                                mensagem = resultado
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !importando
                ) {

                    Text(
                        text =
                            if (importando) {
                                "⏳ A IMPORTAR..."
                            } else {
                                "📥 IMPORTAR ARTIGOS"
                            }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(30.dp)
            )
        }

        Button(
            onClick = onVoltar,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            enabled = !importando
        ) {

            Text(
                text = "← VOLTAR"
            )
        }
    }
}

private suspend fun importarPacote(
    context: Context,
    uriPastaImportacao: Uri
): String {

    var processados = 0
    var novos = 0
    var atualizados = 0

    var fotografiasEncontradas = 0
    var fotografiasNaoEncontradas = 0

    var fotografiasCopiadas = 0
    var fotografiasComErro = 0

    val erros =
        mutableListOf<String>()

    val fotografiasEncontradasLista =
        mutableListOf<String>()

    val fotografiasNaoEncontradasLista =
        mutableListOf<String>()

    return try {

        val pastaImportacao =
            DocumentFile.fromTreeUri(
                context,
                uriPastaImportacao
            )

        if (pastaImportacao == null) {

            return "❌ Não foi possível abrir a pasta selecionada."
        }

        /*
         * ---------------------------------------------------------
         * 1. PROCURAR O ARTIGOS.CSV
         * ---------------------------------------------------------
         */

        val ficheiroCsv =
            encontrarFicheiroRecursivamente(
                pasta = pastaImportacao,
                nomeProcurado = "artigos.csv"
            )

        if (ficheiroCsv == null) {

            return """
                ❌ O ficheiro artigos.csv não foi encontrado.

                Certifica-te de que selecionaste a pasta:
                IMPORTACAO_CMFLORES
            """.trimIndent()
        }

        /*
         * ---------------------------------------------------------
         * 2. PROCURAR A PASTA DE FOTOGRAFIAS
         *
         * Primeiro procuramos uma pasta chamada "fotografias"
         * em QUALQUER nível da pasta selecionada.
         *
         * Se não existir, usamos a própria pasta selecionada
         * como origem das fotografias e procuramos recursivamente
         * todos os ficheiros de imagem.
         * ---------------------------------------------------------
         */

        val pastaFotografias =
            encontrarPastaRecursivamente(
                pasta = pastaImportacao,
                nomesProcurados = listOf(
                    "fotografias",
                    "fotografia",
                    "imagens",
                    "imagem"
                )
            )

        val indiceFotografias =
            if (pastaFotografias != null) {

                criarIndiceFotografias(
                    pasta = pastaFotografias
                )

            } else {

                /*
                 * Não encontrou uma pasta específica.
                 *
                 * Em vez de assumir que não existem fotografias,
                 * pesquisamos toda a pasta selecionada.
                 */
                criarIndiceFotografias(
                    pasta = pastaImportacao
                )
            }

        /*
         * ---------------------------------------------------------
         * 3. PASTA INTERNA DA APP
         * ---------------------------------------------------------
         */

        val pastaFotografiasInternas =
            File(
                context.filesDir,
                "fotografias_artigos"
            )

        if (
            !pastaFotografiasInternas.exists()
        ) {

            if (
                !pastaFotografiasInternas.mkdirs() &&
                !pastaFotografiasInternas.exists()
            ) {

                return """
                    ❌ Não foi possível criar a pasta interna
                    para armazenar as fotografias.
                """.trimIndent()
            }
        }

        /*
         * ---------------------------------------------------------
         * 4. ABRIR CSV
         * ---------------------------------------------------------
         */

        val inputStream =
            context.contentResolver
                .openInputStream(
                    ficheiroCsv.uri
                )

        if (inputStream == null) {

            return "❌ Não foi possível abrir o artigos.csv."
        }

        inputStream.use { stream ->

            BufferedReader(
                InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
                )
            ).use { reader ->

                val linhaCabecalho =
                    reader.readLine()

                if (
                    linhaCabecalho == null ||
                    linhaCabecalho.isBlank()
                ) {

                    return "❌ O artigos.csv está vazio."
                }

                val cabecalhos =
                    analisarLinhaCsv(
                        linhaCabecalho
                    )

                val indices =
                    mutableMapOf<String, Int>()

                cabecalhos.forEachIndexed {
                        index,
                        nome ->

                    indices[
                        normalizarCabecalho(nome)
                    ] = index
                }

                val colunaCodigo =
                    indices["codigo"]

                val colunaDescricao =
                    indices["descricao"]

                val colunaUnidade =
                    indices["unidade"]

                val colunaSemIva =
                    indices["siva"]

                val colunaComIva =
                    indices["civa"]

                val colunaCategoria =
                    indices["categoria"]

                val colunaFoto =
                    indices["foto"]

                if (colunaCodigo == null) {

                    return "❌ A coluna 'Codigo' não foi encontrada no CSV."
                }

                if (colunaDescricao == null) {

                    return "❌ A coluna 'Descrição' não foi encontrada no CSV."
                }

                if (colunaUnidade == null) {

                    return "❌ A coluna 'Unidade' não foi encontrada no CSV."
                }

                if (colunaSemIva == null) {

                    return "❌ A coluna 'S/ Iva' não foi encontrada no CSV."
                }

                if (colunaComIva == null) {

                    return "❌ A coluna 'C/ Iva' não foi encontrada no CSV."
                }

                if (colunaCategoria == null) {

                    return "❌ A coluna 'Categoria' não foi encontrada no CSV."
                }

                if (colunaFoto == null) {

                    return "❌ A coluna 'Foto' não foi encontrada no CSV."
                }

                val db =
                    DatabaseProvider.obterBaseDados(
                        context
                    )

                val dao =
                    db.artigoDao()

                var numeroLinha = 1

                while (true) {

                    val linha =
                        reader.readLine()
                            ?: break

                    numeroLinha++

                    if (linha.isBlank()) {
                        continue
                    }

                    try {

                        val valores =
                            analisarLinhaCsv(
                                linha
                            )

                        val codigo =
                            obterValorCsv(
                                valores,
                                colunaCodigo
                            ).trim()

                        if (codigo.isBlank()) {

                            erros.add(
                                "• Linha $numeroLinha — Código vazio."
                            )

                            continue
                        }

                        val descricao =
                            obterValorCsv(
                                valores,
                                colunaDescricao
                            ).trim()

                        if (descricao.isBlank()) {

                            erros.add(
                                "• Linha $numeroLinha — $codigo\n" +
                                        "  Descrição vazia."
                            )

                            continue
                        }

                        val unidade =
                            obterValorCsv(
                                valores,
                                colunaUnidade
                            ).trim()

                        val categoria =
                            obterValorCsv(
                                valores,
                                colunaCategoria
                            ).trim()

                        val fotoExcel =
                            obterValorCsv(
                                valores,
                                colunaFoto
                            ).trim()

                        val textoSemIva =
                            obterValorCsv(
                                valores,
                                colunaSemIva
                            ).trim()

                        val textoComIva =
                            obterValorCsv(
                                valores,
                                colunaComIva
                            ).trim()

                        val precoSemIva =
                            if (
                                textoSemIva.isBlank()
                            ) {

                                0.0

                            } else {

                                converterPreco(
                                    textoSemIva
                                ) ?: run {

                                    erros.add(
                                        "• Linha $numeroLinha — $codigo\n" +
                                                "  Preço S/ Iva inválido: \"$textoSemIva\"."
                                    )

                                    continue
                                }
                            }

                        val precoComIva =
                            if (
                                textoComIva.isBlank()
                            ) {

                                0.0

                            } else {

                                converterPreco(
                                    textoComIva
                                ) ?: run {

                                    erros.add(
                                        "• Linha $numeroLinha — $codigo\n" +
                                                "  Preço C/ Iva inválido: \"$textoComIva\"."
                                    )

                                    continue
                                }
                            }

                        /*
                         * Se a coluna Foto estiver vazia,
                         * usamos o próprio código do artigo.
                         */
                        val referenciaFotografia =
                            if (
                                fotoExcel.isNotBlank()
                            ) {

                                fotoExcel

                            } else {

                                codigo
                            }

                        var caminhoFotografiaLocal:
                                String? = null

                        /*
                         * -------------------------------------------------
                         * PROCURAR FOTOGRAFIA
                         * -------------------------------------------------
                         */

                        if (
                            referenciaFotografia.isNotBlank() &&
                            indiceFotografias.isNotEmpty()
                        ) {

                            val chaveFoto =
                                normalizarNomeFotografia(
                                    referenciaFotografia
                                )

                            val uriFotografia =
                                indiceFotografias[
                                    chaveFoto
                                ]

                            if (
                                uriFotografia != null
                            ) {

                                fotografiasEncontradas++

                                try {

                                    val ficheiroOrigem =
                                        DocumentFile.fromSingleUri(
                                            context,
                                            uriFotografia
                                        )

                                    val nomeOrigem =
                                        ficheiroOrigem?.name
                                            ?: "foto.jpg"

                                    val extensao =
                                        obterExtensaoFotografia(
                                            nomeOrigem
                                        )

                                    val nomeLocal =
                                        criarNomeFicheiroLocal(
                                            codigo = codigo,
                                            extensao = extensao
                                        )

                                    val ficheiroDestino =
                                        File(
                                            pastaFotografiasInternas,
                                            nomeLocal
                                        )

                                    copiarFotografiaParaArmazenamentoInterno(
                                        context = context,
                                        uriOrigem = uriFotografia,
                                        ficheiroDestino =
                                            ficheiroDestino
                                    )

                                    caminhoFotografiaLocal =
                                        ficheiroDestino.absolutePath

                                    fotografiasCopiadas++

                                    fotografiasEncontradasLista.add(
                                        "$codigo → $referenciaFotografia → ENCONTRADA E COPIADA"
                                    )

                                } catch (e: Exception) {

                                    fotografiasComErro++

                                    erros.add(
                                        "• Linha $numeroLinha — $codigo\n" +
                                                "  Fotografia encontrada, mas não foi possível copiá-la.\n" +
                                                "  ${e.message ?: "Erro desconhecido"}"
                                    )
                                }

                            } else {

                                fotografiasNaoEncontradas++

                                fotografiasNaoEncontradasLista.add(
                                    "$codigo → $referenciaFotografia"
                                )
                            }

                        } else {

                            fotografiasNaoEncontradas++

                            fotografiasNaoEncontradasLista.add(
                                "$codigo → $referenciaFotografia"
                            )
                        }

                        /*
                         * -------------------------------------------------
                         * GUARDAR / ATUALIZAR ARTIGO
                         * -------------------------------------------------
                         */

                        val existente =
                            dao.obterPorCodigo(
                                codigo
                            )

                        if (existente == null) {

                            val novoArtigo =
                                Artigo(
                                    codigo = codigo,
                                    descricao = descricao,
                                    categoria = categoria,
                                    precoSemIva = precoSemIva,
                                    precoComIva = precoComIva,
                                    unidade = unidade,
                                    foto =
                                        caminhoFotografiaLocal
                                            ?: "",
                                    ativo = true
                                )

                            dao.inserir(
                                novoArtigo
                            )

                            novos++

                        } else {

                            val artigoAtualizado =
                                existente.copy(
                                    codigo = codigo,
                                    descricao = descricao,
                                    categoria = categoria,
                                    precoSemIva = precoSemIva,
                                    precoComIva = precoComIva,
                                    unidade = unidade,
                                    foto =
                                        caminhoFotografiaLocal
                                            ?: existente.foto
                                )

                            dao.atualizar(
                                artigoAtualizado
                            )

                            atualizados++
                        }

                        processados++

                    } catch (e: Exception) {

                        erros.add(
                            "• Linha $numeroLinha — Erro inesperado:\n" +
                                    "  ${e.message ?: "Erro desconhecido"}"
                        )
                    }
                }
            }
        }

        /*
         * ---------------------------------------------------------
         * RESULTADO
         * ---------------------------------------------------------
         */

        val resultado =
            StringBuilder()

        resultado.append(
            """
            ✅ IMPORTAÇÃO CONCLUÍDA

            📦 Pacote: IMPORTACAO_CMFLORES

            📊 Processados: $processados
            🆕 Novos: $novos
            🔄 Atualizados: $atualizados

            📷 Fotografias no índice: ${indiceFotografias.size}
            📷 Fotografias encontradas: $fotografiasEncontradas
            💾 Fotografias copiadas para a app: $fotografiasCopiadas
            ⚠️ Fotografias não encontradas: $fotografiasNaoEncontradas
            ❌ Fotografias com erro: $fotografiasComErro

            ⚠️ Erros: ${erros.size}
            """.trimIndent()
        )

        if (
            fotografiasEncontradasLista.isNotEmpty()
        ) {

            resultado.append(
                "\n\n📷 FOTOGRAFIAS ENCONTRADAS:\n\n"
            )

            fotografiasEncontradasLista.forEach {
                    fotografia ->

                resultado.append(
                    "• $fotografia\n"
                )
            }
        }

        if (
            fotografiasNaoEncontradasLista.isNotEmpty()
        ) {

            resultado.append(
                "\n\n⚠️ FOTOGRAFIAS NÃO ENCONTRADAS:\n\n"
            )

            fotografiasNaoEncontradasLista.forEach {
                    fotografia ->

                resultado.append(
                    "• $fotografia\n"
                )
            }
        }

        if (erros.isNotEmpty()) {

            resultado.append(
                "\n\n⚠️ DETALHES DOS ERROS:\n\n"
            )

            erros.forEach { erro ->

                resultado.append(
                    erro
                )

                resultado.append(
                    "\n\n"
                )
            }
        }

        resultado
            .toString()
            .trimEnd()

    } catch (e: Exception) {

        "❌ Erro durante a importação:\n" +
                (
                        e.message
                            ?: "Erro desconhecido"
                        )
    }
}

private fun copiarFotografiaParaArmazenamentoInterno(
    context: Context,
    uriOrigem: Uri,
    ficheiroDestino: File
) {

    val inputStream =
        context.contentResolver
            .openInputStream(
                uriOrigem
            )
            ?: throw Exception(
                "Não foi possível abrir a fotografia de origem."
            )

    inputStream.use { input ->

        FileOutputStream(
            ficheiroDestino
        ).use { output ->

            val buffer =
                ByteArray(
                    8192
                )

            while (true) {

                val quantidade =
                    input.read(
                        buffer
                    )

                if (quantidade == -1) {
                    break
                }

                output.write(
                    buffer,
                    0,
                    quantidade
                )
            }

            output.flush()
        }
    }
}

private fun obterExtensaoFotografia(
    nome: String
): String {

    val nomeMinusculo =
        nome
            .trim()
            .lowercase()

    return when {

        nomeMinusculo.endsWith(".jpeg") ->
            ".jpeg"

        nomeMinusculo.endsWith(".jpg") ->
            ".jpg"

        nomeMinusculo.endsWith(".png") ->
            ".png"

        nomeMinusculo.endsWith(".webp") ->
            ".webp"

        nomeMinusculo.endsWith(".bmp") ->
            ".bmp"

        nomeMinusculo.endsWith(".gif") ->
            ".gif"

        else ->
            ".jpg"
    }
}

private fun criarNomeFicheiroLocal(
    codigo: String,
    extensao: String
): String {

    val codigoSeguro =
        codigo
            .trim()
            .replace(
                Regex("[^a-zA-Z0-9._-]"),
                "_"
            )

    return codigoSeguro +
            extensao
}

private fun encontrarFicheiroRecursivamente(
    pasta: DocumentFile,
    nomeProcurado: String
): DocumentFile? {

    val ficheiros =
        try {

            pasta.listFiles()

        } catch (_: Exception) {

            return null
        }

    for (ficheiro in ficheiros) {

        if (
            ficheiro.isFile &&
            ficheiro.name.equals(
                nomeProcurado,
                ignoreCase = true
            )
        ) {

            return ficheiro
        }
    }

    for (ficheiro in ficheiros) {

        if (ficheiro.isDirectory) {

            val encontrado =
                encontrarFicheiroRecursivamente(
                    pasta = ficheiro,
                    nomeProcurado = nomeProcurado
                )

            if (encontrado != null) {
                return encontrado
            }
        }
    }

    return null
}

private fun encontrarPastaRecursivamente(
    pasta: DocumentFile,
    nomesProcurados: List<String>
): DocumentFile? {

    val ficheiros =
        try {

            pasta.listFiles()

        } catch (_: Exception) {

            return null
        }

    /*
     * Primeiro procuramos neste nível.
     */
    for (ficheiro in ficheiros) {

        if (
            ficheiro.isDirectory &&
            nomesProcurados.any {
                    nome ->
                ficheiro.name.equals(
                    nome,
                    ignoreCase = true
                )
            }
        ) {

            return ficheiro
        }
    }

    /*
     * Depois procuramos em todos os níveis inferiores.
     */
    for (ficheiro in ficheiros) {

        if (ficheiro.isDirectory) {

            val encontrado =
                encontrarPastaRecursivamente(
                    pasta = ficheiro,
                    nomesProcurados = nomesProcurados
                )

            if (encontrado != null) {
                return encontrado
            }
        }
    }

    return null
}

private fun criarIndiceFotografias(
    pasta: DocumentFile
): Map<String, Uri> {

    val indice =
        HashMap<String, Uri>()

    adicionarFotografiasRecursivamente(
        pasta = pasta,
        indice = indice
    )

    return indice
}

private fun adicionarFotografiasRecursivamente(
    pasta: DocumentFile,
    indice: MutableMap<String, Uri>
) {

    val ficheiros =
        try {

            pasta.listFiles()

        } catch (_: Exception) {

            return
        }

    for (ficheiro in ficheiros) {

        if (ficheiro.isDirectory) {

            adicionarFotografiasRecursivamente(
                pasta = ficheiro,
                indice = indice
            )

            continue
        }

        if (!ficheiro.isFile) {
            continue
        }

        val nome =
            ficheiro.name
                ?: continue

        if (
            !extensaoFotografiaValida(nome)
        ) {
            continue
        }

        /*
         * Retira apenas a extensão FINAL.
         *
         * Assim:
         *
         * CA.BRAX16.jpg -> ca.brax16
         * CA.VERX16.jpg -> ca.verx16
         * CRM.STD.png   -> crm.std
         *
         * Os pontos existentes no código são preservados.
         */
        val nomeNormalizado =
            normalizarNomeFotografia(
                nome
            )

        if (
            nomeNormalizado.isBlank()
        ) {
            continue
        }

        if (
            !indice.containsKey(
                nomeNormalizado
            )
        ) {

            indice[nomeNormalizado] =
                ficheiro.uri
        }
    }
}

private fun extensaoFotografiaValida(
    nome: String
): Boolean {

    val nomeMinusculo =
        nome
            .trim()
            .lowercase()

    return nomeMinusculo.endsWith(".jpg") ||
            nomeMinusculo.endsWith(".jpeg") ||
            nomeMinusculo.endsWith(".png") ||
            nomeMinusculo.endsWith(".webp") ||
            nomeMinusculo.endsWith(".bmp") ||
            nomeMinusculo.endsWith(".gif")
}

private fun normalizarNomeFotografia(
    nome: String
): String {

    /*
     * Remove eventuais caminhos.
     *
     * Exemplo:
     *
     * fotografias/CA.BRAX16.jpg
     *
     * passa a:
     *
     * CA.BRAX16.jpg
     */
    val nomeSemCaminho =
        nome
            .substringAfterLast("/")
            .substringAfterLast("\\")

    val nomeLimpo =
        nomeSemCaminho
            .trim()
            .trim('"')
            .lowercase()

    /*
     * IMPORTANTE:
     *
     * Retiramos SOMENTE uma extensão de imagem conhecida.
     *
     * Não utilizamos simplesmente o último ponto,
     * porque o ponto pode fazer parte do código.
     */

    val extensoes =
        listOf(
            ".jpeg",
            ".jpg",
            ".png",
            ".webp",
            ".bmp",
            ".gif"
        )

    for (extensao in extensoes) {

        if (
            nomeLimpo.endsWith(extensao)
        ) {

            return nomeLimpo
                .dropLast(
                    extensao.length
                )
                .trim()
        }
    }

    /*
     * Se não tiver extensão, mantém o nome.
     */
    return nomeLimpo
}

private fun obterValorCsv(
    valores: List<String>,
    indice: Int
): String {

    return if (
        indice in valores.indices
    ) {

        valores[indice]

    } else {

        ""
    }
}

private fun analisarLinhaCsv(
    linha: String
): List<String> {

    val resultado =
        mutableListOf<String>()

    val atual =
        StringBuilder()

    var dentroDeAspas = false

    var indice = 0

    while (
        indice < linha.length
    ) {

        val caracter =
            linha[indice]

        when {

            caracter == '"' -> {

                if (
                    dentroDeAspas &&
                    indice + 1 < linha.length &&
                    linha[indice + 1] == '"'
                ) {

                    atual.append('"')

                    indice++

                } else {

                    dentroDeAspas =
                        !dentroDeAspas
                }
            }

            caracter == ';' &&
                    !dentroDeAspas -> {

                resultado.add(
                    atual.toString()
                )

                atual.clear()
            }

            else -> {

                atual.append(
                    caracter
                )
            }
        }

        indice++
    }

    resultado.add(
        atual.toString()
    )

    return resultado
}

private fun normalizarCabecalho(
    texto: String
): String {

    return texto
        .trim()
        .removePrefix("\uFEFF")
        .lowercase()
        .replace("á", "a")
        .replace("à", "a")
        .replace("ã", "a")
        .replace("â", "a")
        .replace("é", "e")
        .replace("ê", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ô", "o")
        .replace("õ", "o")
        .replace("ú", "u")
        .replace("ç", "c")
        .replace(" ", "")
        .replace("_", "")
        .replace("-", "")
        .replace("/", "")
}

private fun converterPreco(
    textoOriginal: String
): Double? {

    var texto =
        textoOriginal
            .trim()
            .replace("€", "")
            .replace("EUR", "")
            .replace(" ", "")

    if (texto.isBlank()) {

        return null
    }

    val temVirgula =
        texto.contains(",")

    val temPonto =
        texto.contains(".")

    texto =
        when {

            temVirgula && temPonto -> {

                val ultimaVirgula =
                    texto.lastIndexOf(",")

                val ultimoPonto =
                    texto.lastIndexOf(".")

                if (
                    ultimaVirgula >
                    ultimoPonto
                ) {

                    texto
                        .replace(".", "")
                        .replace(",", ".")

                } else {

                    texto.replace(",", "")
                }
            }

            temVirgula -> {

                texto.replace(",", ".")
            }

            else -> {

                texto
            }
        }

    return texto.toDoubleOrNull()
}
