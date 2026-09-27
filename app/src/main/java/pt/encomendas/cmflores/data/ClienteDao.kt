package pt.encomendas.cmflores.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes ORDER BY nome")
    suspend fun obterTodos(): List<Cliente>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun obterPorId(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE codigo = :codigo LIMIT 1")
    suspend fun obterPorCodigo(codigo: String): Cliente?

    @Insert
    suspend fun inserir(cliente: Cliente): Long

    @Update
    suspend fun atualizar(cliente: Cliente)

    @Delete
    suspend fun eliminar(cliente: Cliente)
}
