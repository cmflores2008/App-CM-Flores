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

    // ---------------------------------------------------------
    // ACRESCENTADO PARA O DASHBOARD E RELATÓRIOS
    // ---------------------------------------------------------

    // Conta quantas encomendas estão com o estado "Pendente"
    @Query("SELECT COUNT(*) FROM encomendas WHERE estado = 'Pendente'")
    suspend fun contarPendentes(): Int

    // Soma o valor total (em euros) de todas as encomendas "Pendentes"
    @Query("SELECT SUM(total) FROM encomendas WHERE estado = 'Pendente'")
    suspend fun somarTotalPendentes(): Double?

    // Soma o valor total de encomendas num intervalo de datas de recolha
    @Query("SELECT SUM(total) FROM encomendas WHERE dataRecolha >= :dataInicio AND dataRecolha <= :dataFim")
    suspend fun somarTotalPorDataRecolha(dataInicio: String, dataFim: String): Double?

    // Obtém as 5 próximas encomendas pendentes, ordenadas pela data de entrega mais próxima
    @Query("SELECT * FROM encomendas WHERE estado = 'Pendente' ORDER BY dataEntrega ASC LIMIT 5")
    suspend fun obterProximasEntregas(): List<Encomenda>
}
