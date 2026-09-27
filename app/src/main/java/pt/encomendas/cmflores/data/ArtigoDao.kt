package pt.encomendas.cmflores.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ArtigoDao {

    @Query("SELECT * FROM artigos ORDER BY codigo")
    suspend fun obterTodos(): List<Artigo>

    @Query("SELECT * FROM artigos WHERE id = :id")
    suspend fun obterPorId(id: Long): Artigo?

    @Query("SELECT * FROM artigos WHERE codigo = :codigo LIMIT 1")
    suspend fun obterPorCodigo(codigo: String): Artigo?

    @Insert
    suspend fun inserir(artigo: Artigo): Long

    @Update
    suspend fun atualizar(artigo: Artigo)

    @Delete
    suspend fun eliminar(artigo: Artigo)
}

