package pt.encomendas.cmflores.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encomendas")
data class Encomenda(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val numero: String,

    val clienteId: Long,

    val dataRecolha: String = "",

    val dataEntrega: String = "",

    val total: Double = 0.0,

    val estado: String = "Pendente"
)