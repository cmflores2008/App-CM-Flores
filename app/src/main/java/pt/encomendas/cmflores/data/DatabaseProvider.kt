package pt.encomendas.cmflores.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {

    // Migração da versão 1 para a versão 2
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                ALTER TABLE linhas_encomenda
                ADD COLUMN observacao TEXT NOT NULL DEFAULT ''
                """.trimIndent()
            )
        }
    }

    // Migração da versão 2 para a versão 3
    //
    // A estrutura da tabela clientes foi alterada:
    //
    // ANTIGOS:
    // localidadePostal
    // numeroContribuinte
    //
    // NOVOS:
    // codigoPostal
    // localidade
    // numContribuinte
    // codPagamento
    // codVendedor
    //
    // Para evitar deixar colunas antigas na tabela, recriamos
    // completamente a tabela clientes e preservamos os dados existentes.
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

            // 1. Criar a nova tabela com a estrutura definitiva
            db.execSQL(
                """
                CREATE TABLE clientes_nova (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nome TEXT NOT NULL,
                    codigo TEXT NOT NULL,
                    morada TEXT NOT NULL,
                    codigoPostal TEXT NOT NULL,
                    localidade TEXT NOT NULL,
                    numContribuinte TEXT NOT NULL,
                    contacto TEXT NOT NULL,
                    telefone TEXT NOT NULL,
                    email TEXT NOT NULL,
                    telemovel TEXT NOT NULL,
                    codPagamento TEXT NOT NULL,
                    formaPagamento TEXT NOT NULL,
                    codVendedor TEXT NOT NULL,
                    vendedor TEXT NOT NULL
                )
                """.trimIndent()
            )

            // 2. Copiar os dados existentes da tabela antiga
            //
            // Os campos que não existiam anteriormente ficam vazios.
            //
            // localidadePostal  -> localidade
            // numeroContribuinte -> numContribuinte
            db.execSQL(
                """
                INSERT INTO clientes_nova (
                    id,
                    nome,
                    codigo,
                    morada,
                    codigoPostal,
                    localidade,
                    numContribuinte,
                    contacto,
                    telefone,
                    email,
                    telemovel,
                    codPagamento,
                    formaPagamento,
                    codVendedor,
                    vendedor
                )
                SELECT
                    id,
                    nome,
                    codigo,
                    morada,
                    '',
                    localidadePostal,
                    numeroContribuinte,
                    contacto,
                    telefone,
                    email,
                    telemovel,
                    '',
                    formaPagamento,
                    '',
                    vendedor
                FROM clientes
                """.trimIndent()
            )

            // 3. Apagar a tabela antiga
            db.execSQL("DROP TABLE clientes")

            // 4. Dar à nova tabela o nome definitivo
            db.execSQL("ALTER TABLE clientes_nova RENAME TO clientes")
        }
    }

    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun obterBaseDados(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "encomendas_flores.db"
            )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3
                )
                .build()
                .also { INSTANCE = it }
        }
    }
}
