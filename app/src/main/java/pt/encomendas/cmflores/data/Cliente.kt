package pt.encomendas.cmflores.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clientes")
data class Cliente(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val nome: String,
    val codigo: String = "",
    val morada: String = "",
    val codigoPostal: String = "",
    val localidade: String = "",
    val numContribuinte: String = "",
    val contacto: String = "",
    val telefone: String = "",
    val email: String = "",
    val telemovel: String = "",
    val codPagamento: String = "",
    val formaPagamento: String = "",
    val codVendedor: String = "",
    val vendedor: String = ""
)
