package pt.encomendas.cmflores.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "linhas_encomenda")
data class LinhaEncomenda(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val encomendaId: Long,

    val artigoId: Long,

    val quantidade: Double,

    val unidade: String = "",

    val precoUnitario: Double,

    val total: Double,

    val observacao: String = ""
)
