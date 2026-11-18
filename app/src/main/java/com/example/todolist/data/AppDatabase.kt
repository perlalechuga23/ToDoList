package com.example.todolist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [Tarea::class, Categoria::class, Usuario::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tareaDao(): TareaDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE tareas ADD COLUMN fecha TEXT NOT NULL DEFAULT ''"
                )

                val hoy = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())

                database.execSQL(
                    "UPDATE tareas SET fecha = '$hoy' WHERE fecha = ''"
                )
            }
        }

        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS categorias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS usuarios (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        correo TEXT NOT NULL,
                        contrasenaHash TEXT NOT NULL,
                        tipoUsuario TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE tareas
                    ADD COLUMN usuarioId INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    UPDATE tareas
                    SET usuarioId = (
                        SELECT MIN(id) FROM usuarios
                    )
                    WHERE usuarioId = 0
                    """.trimIndent()
                )
            }
        }

        private val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE tareas
                    ADD COLUMN estado TEXT NOT NULL DEFAULT 'Pendiente'
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    UPDATE tareas
                    SET estado = CASE
                        WHEN completada = 1 THEN 'Completada'
                        ELSE 'Pendiente'
                    END
                    """.trimIndent()
                )
            }
        }

        private val MIGRACION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {

                database.execSQL(
                    """
                    CREATE TABLE tareas_nueva (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        titulo TEXT NOT NULL,
                        descripcion TEXT NOT NULL,
                        prioridad TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        completada INTEGER NOT NULL,
                        fecha TEXT NOT NULL DEFAULT '',
                        usuarioId INTEGER NOT NULL DEFAULT 0,
                        estado TEXT NOT NULL DEFAULT 'Pendiente',
                        FOREIGN KEY(usuarioId) REFERENCES usuarios(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    INSERT INTO tareas_nueva (
                        id, titulo, descripcion, prioridad, categoria,
                        completada, fecha, usuarioId, estado
                    )
                    SELECT
                        id, titulo, descripcion, prioridad, categoria,
                        completada, fecha, usuarioId, estado
                    FROM tareas
                    """.trimIndent()
                )

                database.execSQL("DROP TABLE tareas")
                database.execSQL(
                    "ALTER TABLE tareas_nueva RENAME TO tareas"
                )

                database.execSQL(
                    "CREATE INDEX index_tareas_usuarioId ON tareas(usuarioId)"
                )
            }
        }

        fun obtenerBaseDatos(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instancia = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todolist_database"
                )
                    .addMigrations(
                        MIGRACION_1_2,
                        MIGRACION_2_3,
                        MIGRACION_3_4,
                        MIGRACION_4_5,
                        MIGRACION_5_6,
                        MIGRACION_6_7
                    )
                    .build()

                INSTANCE = instancia
                instancia
            }
        }
    }
}
