package pt.encomendas.cmflores.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import pt.encomendas.cmflores.data.Artigo
import pt.encomendas.cmflores.data.DatabaseProvider
import pt.encomendas.cmflores.importacao.ImportacaoArtigosScreen
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtigosScreen(
    onVoltar: () -> Unit
) {

    val context = LocalContext.current

    var artigos by remember {
        mutableStateOf<List<Artigo>>(emptyList())
    }

    var mostrarImportacao by remember {
        mutableStateOf(false)
    }

    var pesquisa by remember {
        mutableStateOf("")
    }

    var mostrarFormulario by remember {
        mutableStateOf(false)
    }

    var artigoSelecionado by remember {
        mutableStateOf<Artigo?>(null)
    }

    var fotoAmpliada by remember {
        mutableStateOf<String?>(null)
    }

    /*
     * Carrega os artigos da base de dados.
     *
     * O efeito é executado quando:
     * - abrimos/fechamos o formulário
     * - abrimos/fechamos a importação
     *
     * Desta forma, depois de uma importação,
     * a lista é atualizada imediatamente.
     */
    LaunchedEffect(
        mostrarFormulario,
        mostrarImportacao
    ) {

        if (
            !mostrarFormulario &&
            !mostrarImportacao
        ) {

            artigos =
                DatabaseProvider
                    .obterBaseDados(context)
                    .artigoDao()
                    .obterTodos()
        }
    }

    if (mostrarImportacao) {

        ImportacaoArtigosScreen(
            onVoltar = {
                mostrarImportacao = false
            }
        )

        return
    }

    if (mostrarFormulario) {

        ArtigoFormScreen(
            artigoId =
                artigoSelecionado?.id,

            onVoltar = {
                mostrarFormulario = false
                artigoSelecionado = null
            },

            onGuardar = {
                mostrarFormulario = false
                artigoSelecionado = null
            }
        )

        return
    }

    /*
     * Fotografia em ecrã inteiro
     */
    if (fotoAmpliada != null) {

        Dialog(
            onDismissRequest = {
                fotoAmpliada = null
            },

            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clickable {
                            fotoAmpliada = null
                        }
            ) {

                AsyncImage(

                    model =
                        if (
                            fotoAmpliada!!.startsWith("/")
                        ) {
                            File(fotoAmpliada!!)
                        } else {
                            Uri.parse(fotoAmpliada!!)
                        },

                    contentDescription =
                        "Fotografia ampliada",

                    modifier =
                        Modifier.fillMaxSize(),

                    contentScale =
                        ContentScale.Fit
                )

                TextButton(

                    onClick = {
                        fotoAmpliada = null
                    },

                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                ) {

                    Text(
                        text = "✕",
                        fontSize = 28.sp
                    )
                }
            }
        }
    }

    val artigosFiltrados =
        artigos.filter { artigo ->

            artigo.codigo.contains(
                pesquisa,
                ignoreCase = true
            ) ||
                    artigo.descricao.contains(
                        pesquisa,
                        ignoreCase = true
                    ) ||
                    artigo.categoria.contains(
                        pesquisa,
                        ignoreCase = true
                    )
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "🌹 ARTIGOS",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    TextButton(
                        onClick = onVoltar
                    ) {

                        Text(
                            text = "←",
                            fontSize = 24.sp
                        )
                    }
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(

                onClick = {

                    artigoSelecionado = null
                    mostrarFormulario = true
                }
            ) {

                Text(
                    text = "+",
                    fontSize = 24.sp
                )
            }
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
                    mostrarImportacao = true
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    text = "📥 IMPORTAR ARTIGOS",
                    fontSize = 16.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            OutlinedTextField(

                value =
                    pesquisa,

                onValueChange = {
                    pesquisa = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Pesquisar artigo")
                },

                placeholder = {
                    Text(
                        "Código, descrição ou categoria"
                    )
                },

                singleLine = true
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(

                text =
                    "${artigosFiltrados.size} artigo(s)",

                fontSize = 14.sp,

                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            if (artigosFiltrados.isEmpty()) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = "🌹",
                        fontSize = 50.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(

                        text =
                            if (artigos.isEmpty()) {
                                "Ainda não existem artigos."
                            } else {
                                "Nenhum artigo encontrado."
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    if (artigos.isEmpty()) {

                        Button(

                            onClick = {

                                artigoSelecionado = null
                                mostrarFormulario = true
                            }
                        ) {

                            Text(
                                "Criar primeiro artigo"
                            )
                        }
                    }
                }

            } else {

                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize(),

                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    items(

                        items =
                            artigosFiltrados,

                        key = {
                                artigo -> artigo.id
                        }

                    ) { artigo ->

                        ArtigoCard(

                            artigo = artigo,

                            onClick = {

                                artigoSelecionado =
                                    artigo

                                mostrarFormulario =
                                    true
                            },

                            onFotoClick = { caminho ->

                                fotoAmpliada =
                                    caminho
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtigoCard(
    artigo: Artigo,
    onClick: () -> Unit,
    onFotoClick: (String) -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * MINIATURA
             */
            if (artigo.foto.isNotBlank()) {

                AsyncImage(

                    model =
                        if (
                            artigo.foto.startsWith("/")
                        ) {
                            File(artigo.foto)
                        } else {
                            Uri.parse(artigo.foto)
                        },

                    contentDescription =
                        "Fotografia de ${artigo.descricao}",

                    modifier =
                        Modifier
                            .size(85.dp)
                            .clickable {
                                onFotoClick(
                                    artigo.foto
                                )
                            },

                    contentScale =
                        ContentScale.Crop
                )

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )
            }

            /*
             * DADOS DO ARTIGO
             */
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        artigo.codigo,

                    fontSize = 14.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(

                    text =
                        artigo.descricao,

                    fontSize = 18.sp,

                    fontWeight =
                        FontWeight.Bold
                )

                if (artigo.categoria.isNotBlank()) {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(

                        text =
                            "Categoria: ${artigo.categoria}",

                        fontSize = 14.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Row {

                    Text(

                        text =
                            "€ %.2f"
                                .format(
                                    artigo.precoComIva
                                ),

                        fontSize = 16.sp,

                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        artigo.unidade.isNotBlank()
                    ) {

                        Text(

                            text =
                                " / ${artigo.unidade}",

                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(

                    text =
                        if (artigo.ativo) {
                            "🟢 Ativo"
                        } else {
                            "🔴 Inativo"
                        },

                    fontSize = 13.sp
                )
            }
        }
    }
}
