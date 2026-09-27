package pt.encomendas.cmflores.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface LinhaEncomendaDao {

    @Query("SELECT * FROM linhas_encomenda WHERE encomendaId = :encomendaId")
    suspend fun obterPorEncomenda(encomendaId: Long): List<LinhaEncomenda>

    @Insert
    suspend fun inserir(linha: LinhaEncomenda): Long

    @Update
    suspend fun atualizar(linha: LinhaEncomenda)

    @Delete
    suspend fun eliminar(linha: LinhaEncomenda)
}
