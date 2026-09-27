package pt.encomendas.cmflores.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artigos")
data class Artigo(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val codigo: String,
    val descricao: String,
    val categoria: String = "",
    val precoSemIva: Double = 0.0,
    val precoComIva: Double = 0.0,
    val unidade: String = "",
    val foto: String = "",
    val ativo: Boolean = true
)
