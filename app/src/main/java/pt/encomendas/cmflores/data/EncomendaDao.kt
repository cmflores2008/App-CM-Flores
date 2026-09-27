package pt.encomendas.cmflores.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EncomendaDao {

    @Query("SELECT * FROM encomendas ORDER BY id DESC")
    suspend fun obterTodas(): List<Encomenda>

    @Query("SELECT * FROM encomendas WHERE id = :id")
    suspend fun obterPorId(id: Long): Encomenda?

    @Query(
        "SELECT * FROM encomendas " +
                "WHERE clienteId = :clienteId " +
                "ORDER BY id DESC"
    )
    suspend fun obterPorCliente(clienteId: Long): List<Encomenda>

    @Insert
    suspend fun inserir(encomenda: Encomenda): Long

    @Update
    suspend fun atualizar(encomenda: Encomenda)

    @Delete
    suspend fun eliminar(encomenda: Encomenda)
}

