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

    // ---------------------------------------------------------
    // ACRESCENTADO PARA O DASHBOARD
    // ---------------------------------------------------------

    // Conta quantos artigos não têm foto (ou onde o campo está vazio)
    @Query("SELECT COUNT(*) FROM artigos WHERE foto IS NULL OR TRIM(foto) = ''")
    suspend fun contarSemFoto(): Int

    // Conta o total geral de artigos no catálogo
    @Query("SELECT COUNT(*) FROM artigos")
    suspend fun contarTotal(): Int
}
