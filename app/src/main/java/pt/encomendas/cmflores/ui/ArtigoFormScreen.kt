package pt.encomendas.cmflores.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.DatabaseProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private fun obterExtensaoImagem(
    context: Context,
    uri: Uri
): String {

    val tipoMime =
        context.contentResolver
            .getType(uri)

    return when (tipoMime) {

        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "image/heic" -> ".heic"
        "image/heif" -> ".heif"

        else -> ".jpg"
    }
}

private fun copiarImagemParaPastaApp(
    context: Context,
    uri: Uri,
    codigoArtigo: String
): String? {

    return try {

        val pastaApp =
            File(
                context.filesDir,
                "Ficheiros App"
            )

        val pastaFotos =
            File(
                pastaApp,
                "Fotos"
            )

        if (!pastaFotos.exists()) {
            pastaFotos.mkdirs()
        }

        val extensao =
            obterExtensaoImagem(
                context,
                uri
            )

        val nomeSeguro =
            codigoArtigo
                .trim()
                .replace(
                    Regex("[^a-zA-Z0-9_-]"),
                    "_"
                )

        val identificador =
            UUID.randomUUID()
                .toString()
                .replace(
                    "-",
                    ""
                )

        val nomeFicheiro =
            "${nomeSeguro}_${identificador}$extensao"

        val ficheiroDestino =
            File(
                pastaFotos,
                nomeFicheiro
            )

        context.contentResolver
            .openInputStream(uri)
            ?.use { input ->

                FileOutputStream(
                    ficheiroDestino
                ).use { output ->

                    input.copyTo(output)
                }
            }
            ?: return null

        ficheiroDestino.absolutePath

    } catch (e: Exception) {

        null
    }
}

private fun eliminarFicheiroFotografia(
    caminho: String
) {

    if (caminho.isBlank()) {
        return
    }

    try {

        val ficheiro =
            File(caminho)

        if (ficheiro.exists()) {
            ficheiro.delete()
        }

    } catch (_: Exception) {
        // Não interromper a operação principal.
    }
}

@Composable
fun ArtigoFormScreen(
    artigoId: Long? = null,
    onGuardar: () -> Unit,
    onVoltar: () -> Unit
) {

    val context = LocalContext.current

    val modoEdicao =
        artigoId != null

    var codigo by remember {
        mutableStateOf("")
    }

    var descricao by remember {
        mutableStateOf("")
    }

    var categoria by remember {
        mutableStateOf("")
    }

    var precoSemIva by remember {
        mutableStateOf("")
    }

    var precoComIva by remember {
        mutableStateOf("")
    }

    var unidade by remember {
        mutableStateOf("")
    }

    var foto by remember {
        mutableStateOf("")
    }

    var fotoOriginal by remember {
        mutableStateOf("")
    }

    var fotografiasNovas by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var ativo by remember {
        mutableStateOf(true)
    }

    var mensagemErro by remember {
        mutableStateOf("")
    }

    var carregando by remember {
        mutableStateOf(modoEdicao)
    }

    var guardando by remember {
        mutableStateOf(false)
    }

    var mostrarConfirmacaoEliminar by remember {
        mutableStateOf(false)
    }

    var mostrarConfirmacaoRemoverFoto by remember {
        mutableStateOf(false)
    }

    var copiandoFotografia by remember {
        mutableStateOf(false)
    }

    val selecionarImagem =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri == null) {
                return@rememberLauncherForActivityResult
            }

            if (codigo.trim().isBlank()) {

                mensagemErro =
                    "⚠️ Primeiro indique o código do artigo."

                return@rememberLauncherForActivityResult
            }

            copiandoFotografia = true
            mensagemErro = ""

            CoroutineScope(
                Dispatchers.IO
            ).launch {

                val caminho =
                    copiarImagemParaPastaApp(
                        context = context,
                        uri = uri,
                        codigoArtigo = codigo
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    copiandoFotografia = false

                    if (caminho != null) {

                        foto = caminho

                        fotografiasNovas =
                            fotografiasNovas + caminho

                    } else {

                        mensagemErro =
                            "❌ Não foi possível copiar a fotografia."
                    }
                }
            }
        }

    LaunchedEffect(artigoId) {

        if (artigoId != null) {

            carregando = true

            val artigo =
                withContext(Dispatchers.IO) {

                    val db =
                        DatabaseProvider
                            .obterBaseDados(context)

                    db.artigoDao()
                        .obterPorId(artigoId)
                }

            if (artigo != null) {

                codigo =
                    artigo.codigo

                descricao =
                    artigo.descricao

                categoria =
                    artigo.categoria

                precoSemIva =
                    artigo.precoSemIva
                        .toString()

                precoComIva =
                    artigo.precoComIva
                        .toString()

                unidade =
                    artigo.unidade

                foto =
                    artigo.foto

                fotoOriginal =
                    artigo.foto

                ativo =
                    artigo.ativo
            }

            carregando = false
        }
    }

    if (carregando) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally,

            verticalArrangement =
                Arrangement.Center
        ) {

            Text(
                text =
                    "⏳ A carregar artigo...",

                fontSize = 18.sp
            )
        }

        return
    }

    /*
     * CONFIRMAÇÃO PARA REMOVER FOTOGRAFIA
     */
    if (mostrarConfirmacaoRemoverFoto) {

        AlertDialog(

            onDismissRequest = {

                if (!guardando) {
                    mostrarConfirmacaoRemoverFoto =
                        false
                }
            },

            title = {

                Text(
                    text =
                        "🗑️ Remover fotografia"
                )
            },

            text = {

                Text(
                    text =
                        "Tem a certeza que pretende remover a fotografia deste artigo?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        mostrarConfirmacaoRemoverFoto =
                            false

                        val fotografiaAtual =
                            foto

                        /*
                         * Se a fotografia atual foi selecionada
                         * durante esta edição e ainda não foi guardada,
                         * basta eliminá-la fisicamente.
                         */
                        if (
                            fotografiaAtual.isNotBlank() &&
                            fotografiasNovas.contains(
                                fotografiaAtual
                            )
                        ) {

                            CoroutineScope(
                                Dispatchers.IO
                            ).launch {

                                eliminarFicheiroFotografia(
                                    fotografiaAtual
                                )

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    fotografiasNovas =
                                        fotografiasNovas.filter {
                                            it != fotografiaAtual
                                        }

                                    foto = ""

                                    mensagemErro = ""
                                }
                            }

                            return@TextButton
                        }

                        /*
                         * Se estamos a editar um artigo que já tinha
                         * fotografia guardada, atualizamos primeiro
                         * a base de dados.
                         */
                        if (
                            modoEdicao &&
                            artigoId != null &&
                            fotografiaAtual.isNotBlank()
                        ) {

                            guardando = true

                            CoroutineScope(
                                Dispatchers.IO
                            ).launch {

                                try {

                                    val db =
                                        DatabaseProvider
                                            .obterBaseDados(
                                                context
                                            )

                                    val dao =
                                        db.artigoDao()

                                    val existente =
                                        dao.obterPorId(
                                            artigoId
                                        )

                                    if (existente == null) {

                                        withContext(
                                            Dispatchers.Main
                                        ) {

                                            guardando = false

                                            mensagemErro =
                                                "❌ O artigo não foi encontrado."
                                        }

                                        return@launch
                                    }

                                    val atualizado =
                                        existente.copy(
                                            foto = ""
                                        )

                                    dao.atualizar(
                                        atualizado
                                    )

                                    /*
                                     * Só depois da BD estar atualizada
                                     * apagamos o ficheiro físico.
                                     */
                                    eliminarFicheiroFotografia(
                                        fotografiaAtual
                                    )

                                    withContext(
                                        Dispatchers.Main
                                    ) {

                                        foto = ""
                                        fotoOriginal = ""

                                        guardando = false
                                        mensagemErro = ""
                                    }

                                } catch (e: Exception) {

                                    withContext(
                                        Dispatchers.Main
                                    ) {

                                        guardando = false

                                        mensagemErro =
                                            "❌ Erro ao remover fotografia:\n" +
                                                    (
                                                            e.message
                                                                ?: "Erro desconhecido"
                                                            )
                                    }
                                }
                            }

                        } else {

                            foto = ""
                            mensagemErro = ""
                        }
                    },

                    enabled =
                        !guardando
                ) {

                    Text(
                        text =
                            "REMOVER"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        mostrarConfirmacaoRemoverFoto =
                            false
                    },

                    enabled =
                        !guardando
                ) {

                    Text(
                        text =
                            "CANCELAR"
                    )
                }
            }
        )
    }

    /*
     * CONFIRMAÇÃO PARA ELIMINAR ARTIGO
     */
    if (mostrarConfirmacaoEliminar) {

        AlertDialog(

            onDismissRequest = {

                if (!guardando) {
                    mostrarConfirmacaoEliminar =
                        false
                }
            },

            title = {

                Text(
                    text =
                        "🗑️ Eliminar artigo"
                )
            },

            text = {

                Text(
                    text =
                        "Tem a certeza que pretende eliminar o artigo:\n\n" +
                                "\"$descricao\"?\n\n" +
                                "Esta operação não pode ser anulada."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        val id =
                            artigoId

                        if (id == null) {

                            mostrarConfirmacaoEliminar =
                                false

                            return@TextButton
                        }

                        guardando = true

                        mostrarConfirmacaoEliminar =
                            false

                        CoroutineScope(
                            Dispatchers.IO
                        ).launch {

                            try {

                                val db =
                                    DatabaseProvider
                                        .obterBaseDados(
                                            context
                                        )

                                val dao =
                                    db.artigoDao()

                                val artigo =
                                    dao.obterPorId(id)

                                if (artigo != null) {

                                    dao.eliminar(
                                        artigo
                                    )
                                }

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    guardando = false

                                    onGuardar()
                                }

                            } catch (e: Exception) {

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    guardando = false

                                    mensagemErro =
                                        "❌ Erro ao eliminar:\n" +
                                                (
                                                        e.message
                                                            ?: "Erro desconhecido"
                                                        )
                                }
                            }
                        }
                    },

                    enabled =
                        !guardando
                ) {

                    Text(
                        text =
                            "ELIMINAR"
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        mostrarConfirmacaoEliminar =
                            false
                    },

                    enabled =
                        !guardando
                ) {

                    Text(
                        text =
                            "CANCELAR"
                    )
                }
            }
        )
    }

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )

        Text(

            text =
                if (modoEdicao) {
                    "✏️ EDITAR ARTIGO"
                } else {
                    "➕ NOVO ARTIGO"
                },

            fontSize = 24.sp
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        OutlinedTextField(

            value =
                codigo,

            onValueChange = {

                codigo = it
                mensagemErro = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Código *")
            },

            singleLine = true,

            enabled =
                !modoEdicao &&
                        !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(

            value =
                descricao,

            onValueChange = {

                descricao = it
                mensagemErro = ""
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Descrição *")
            },

            singleLine = false,

            minLines = 2,

            enabled =
                !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(

            value =
                categoria,

            onValueChange = {

                categoria = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Categoria")
            },

            singleLine = true,

            enabled =
                !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(

            value =
                precoSemIva,

            onValueChange = {

                precoSemIva =
                    it
                        .replace(
                            ",",
                            "."
                        )
                        .filter {

                            it.isDigit() ||
                                    it == '.'
                        }
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Preço S/ IVA (€)")
            },

            singleLine = true,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Decimal
                ),

            enabled =
                !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(

            value =
                precoComIva,

            onValueChange = {

                precoComIva =
                    it
                        .replace(
                            ",",
                            "."
                        )
                        .filter {

                            it.isDigit() ||
                                    it == '.'
                        }
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Preço C/ IVA (€)")
            },

            singleLine = true,

            keyboardOptions =
                KeyboardOptions(
                    keyboardType =
                        KeyboardType.Decimal
                ),

            enabled =
                !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        OutlinedTextField(

            value =
                unidade,

            onValueChange = {

                unidade = it
            },

            modifier =
                Modifier.fillMaxWidth(),

            label = {
                Text("Unidade")
            },

            placeholder = {
                Text(
                    "Ex.: unidade, ramo, caixa"
                )
            },

            singleLine = true,

            enabled =
                !guardando &&
                        !copiandoFotografia
        )

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        if (foto.isNotBlank()) {

            Text(

                text =
                    "🖼️ Fotografia",

                modifier =
                    Modifier.fillMaxWidth(),

                fontSize = 16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            AsyncImage(

                model =
                    if (foto.startsWith("/")) {
                        File(foto)
                    } else {
                        Uri.parse(foto)
                    },

                contentDescription =
                    "Fotografia do artigo",

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

        } else {

            Text(

                text =
                    "📷 Nenhuma fotografia selecionada",

                modifier =
                    Modifier.fillMaxWidth(),

                fontSize = 15.sp
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )
        }

        Button(

            onClick = {

                selecionarImagem.launch(
                    "image/*"
                )
            },

            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                !guardando &&
                        !copiandoFotografia

        ) {

            Text(

                text =
                    if (copiandoFotografia) {
                        "⏳ A COPIAR..."
                    } else if (foto.isBlank()) {
                        "📷 SELECIONAR FOTOGRAFIA"
                    } else {
                        "🔄 SUBSTITUIR FOTOGRAFIA"
                    },

                fontSize = 16.sp
            )
        }

        /*
         * BOTÃO REMOVER FOTOGRAFIA
         */
        if (foto.isNotBlank()) {

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            TextButton(

                onClick = {

                    mostrarConfirmacaoRemoverFoto =
                        true
                },

                enabled =
                    !guardando &&
                            !copiandoFotografia,

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    text =
                        "🗑️ REMOVER FOTOGRAFIA",

                    fontSize = 15.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(18.dp)
        )

        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(

                text =
                    if (ativo) {
                        "🟢 Artigo ativo"
                    } else {
                        "🔴 Artigo inativo"
                    },

                fontSize = 16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Switch(

                checked =
                    ativo,

                onCheckedChange = {

                    ativo = it
                },

                enabled =
                    !guardando &&
                            !copiandoFotografia
            )
        }

        if (mensagemErro.isNotBlank()) {

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(

                text =
                    mensagemErro,

                fontSize = 15.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        Button(

            onClick = {

                if (codigo.trim().isBlank()) {

                    mensagemErro =
                        "⚠️ O código é obrigatório."

                    return@Button
                }

                if (descricao.trim().isBlank()) {

                    mensagemErro =
                        "⚠️ A descrição é obrigatória."

                    return@Button
                }

                val valorSemIva =

                    if (
                        precoSemIva
                            .trim()
                            .isBlank()
                    ) {

                        0.0

                    } else {

                        precoSemIva
                            .replace(
                                ",",
                                "."
                            )
                            .toDoubleOrNull()
                    }

                if (valorSemIva == null) {

                    mensagemErro =
                        "⚠️ O preço S/ IVA não é válido."

                    return@Button
                }

                val valorComIva =

                    if (
                        precoComIva
                            .trim()
                            .isBlank()
                    ) {

                        0.0

                    } else {

                        precoComIva
                            .replace(
                                ",",
                                "."
                            )
                            .toDoubleOrNull()
                    }

                if (valorComIva == null) {

                    mensagemErro =
                        "⚠️ O preço C/ IVA não é válido."

                    return@Button
                }

                guardando = true
                mensagemErro = ""

                CoroutineScope(
                    Dispatchers.IO
                ).launch {

                    try {

                        val db =
                            DatabaseProvider
                                .obterBaseDados(
                                    context
                                )

                        val dao =
                            db.artigoDao()

                        if (modoEdicao) {

                            val existente =
                                artigoId?.let {

                                    dao.obterPorId(it)
                                }

                            if (existente == null) {

                                fotografiasNovas.forEach { caminho ->
                                    eliminarFicheiroFotografia(
                                        caminho
                                    )
                                }

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    guardando = false

                                    mensagemErro =
                                        "❌ O artigo não foi encontrado."
                                }

                                return@launch
                            }

                            val artigoComCodigo =
                                dao.obterPorCodigo(
                                    codigo.trim()
                                )

                            if (
                                artigoComCodigo != null &&
                                artigoComCodigo.id != existente.id
                            ) {

                                fotografiasNovas.forEach { caminho ->
                                    eliminarFicheiroFotografia(
                                        caminho
                                    )
                                }

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    guardando = false

                                    mensagemErro =
                                        "⚠️ Já existe outro artigo com o código \"$codigo\"."
                                }

                                return@launch
                            }

                            val atualizado =
                                existente.copy(

                                    codigo =
                                        codigo.trim(),

                                    descricao =
                                        descricao.trim(),

                                    categoria =
                                        categoria.trim(),

                                    precoSemIva =
                                        valorSemIva,

                                    precoComIva =
                                        valorComIva,

                                    unidade =
                                        unidade.trim(),

                                    foto =
                                        foto,

                                    ativo =
                                        ativo
                                )

                            dao.atualizar(
                                atualizado
                            )

                            /*
                             * Se a fotografia foi substituída,
                             * eliminamos a fotografia anterior.
                             */
                            if (
                                fotoOriginal.isNotBlank() &&
                                fotoOriginal != foto
                            ) {

                                eliminarFicheiroFotografia(
                                    fotoOriginal
                                )
                            }

                            fotografiasNovas.forEach { caminho ->

                                if (caminho != foto) {

                                    eliminarFicheiroFotografia(
                                        caminho
                                    )
                                }
                            }

                        } else {

                            val existente =
                                dao.obterPorCodigo(
                                    codigo.trim()
                                )

                            if (existente != null) {

                                fotografiasNovas.forEach { caminho ->
                                    eliminarFicheiroFotografia(
                                        caminho
                                    )
                                }

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    guardando = false

                                    mensagemErro =
                                        "⚠️ Já existe um artigo com o código \"$codigo\"."
                                }

                                return@launch
                            }

                            val novoArtigo =
                                Artigo(

                                    codigo =
                                        codigo.trim(),

                                    descricao =
                                        descricao.trim(),

                                    categoria =
                                        categoria.trim(),

                                    precoSemIva =
                                        valorSemIva,

                                    precoComIva =
                                        valorComIva,

                                    unidade =
                                        unidade.trim(),

                                    foto =
                                        foto,

                                    ativo =
                                        ativo
                                )

                            dao.inserir(
                                novoArtigo
                            )

                            fotografiasNovas.forEach { caminho ->

                                if (caminho != foto) {

                                    eliminarFicheiroFotografia(
                                        caminho
                                    )
                                }
                            }
                        }

                        withContext(
                            Dispatchers.Main
                        ) {

                            guardando = false

                            onGuardar()
                        }

                    } catch (e: Exception) {

                        fotografiasNovas.forEach { caminho ->

                            eliminarFicheiroFotografia(
                                caminho
                            )
                        }

                        withContext(
                            Dispatchers.Main
                        ) {

                            guardando = false

                            if (modoEdicao) {
                                foto = fotoOriginal
                            }

                            mensagemErro =
                                "❌ Erro ao guardar:\n" +
                                        (
                                                e.message
                                                    ?: "Erro desconhecido"
                                                )
                        }
                    }
                }
            },

            modifier =
                Modifier.fillMaxWidth(),

            enabled =
                !guardando &&
                        !copiandoFotografia

        ) {

            Text(

                text =
                    if (guardando) {
                        "⏳ A GUARDAR..."
                    } else {
                        "💾 GUARDAR ARTIGO"
                    }
            )
        }

        if (modoEdicao) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            Button(

                onClick = {

                    mostrarConfirmacaoEliminar =
                        true
                },

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    !guardando &&
                            !copiandoFotografia

            ) {

                Text(

                    text =
                        "🗑️ ELIMINAR ARTIGO",

                    fontSize = 16.sp
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        TextButton(

            onClick = {

                CoroutineScope(
                    Dispatchers.IO
                ).launch {

                    fotografiasNovas.forEach { caminho ->

                        eliminarFicheiroFotografia(
                            caminho
                        )
                    }

                    withContext(
                        Dispatchers.Main
                    ) {

                        onVoltar()
                    }
                }
            },

            enabled =
                !guardando &&
                        !copiandoFotografia

        ) {

            Text(
                text =
                    "← CANCELAR"
            )
        }

        Spacer(
            modifier =
                Modifier.height(20.dp)
        )
    }
}
