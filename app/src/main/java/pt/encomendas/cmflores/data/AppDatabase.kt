package pt.encomendas.cmflores.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Cliente::class,
        Artigo::class,
        Encomenda::class,
        LinhaEncomenda::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clienteDao(): ClienteDao

    abstract fun artigoDao(): ArtigoDao

    abstract fun encomendaDao(): EncomendaDao

    abstract fun linhaEncomendaDao(): LinhaEncomendaDao
}

