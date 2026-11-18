package com.example.todolist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Update
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    // Insertar, actualizar y eliminar tareas
    @Insert
    suspend fun insertarTarea(tarea: Tarea)

    @Update
    suspend fun actualizarTarea(tarea: Tarea)

    @Delete
    suspend fun eliminarTarea(tarea: Tarea)

    // Obtener tareas de un usuario
    @Query("""
        SELECT * FROM tareas
        WHERE usuarioId = :usuarioId
        ORDER BY id DESC
    """)
    fun obtenerTareasPorUsuario(usuarioId: Int): Flow<List<Tarea>>

    // Buscar una tarea por usuario
    @Query("""
        SELECT * FROM tareas
        WHERE id = :id AND usuarioId = :usuarioId
        LIMIT 1
    """)
    suspend fun obtenerTareaPorIdYUsuario(id: Int, usuarioId: Int): Tarea?

    // Filtrar, buscar y ordenar tareas directamente en Room
    @Query("""
        SELECT * FROM tareas
        WHERE usuarioId = :usuarioId

        AND (
            :filtro = 'Todas'
            OR (:filtro = 'Hoy' AND fecha = :hoy)
            OR (:filtro NOT IN ('Todas', 'Hoy') AND estado = :filtro)
        )

        AND (
            :busqueda = ''
            OR titulo LIKE '%' || :busqueda || '%' COLLATE NOCASE
            OR descripcion LIKE '%' || :busqueda || '%' COLLATE NOCASE
        )

        AND (
            :categoria = 'Todas las categorías'
            OR categoria = :categoria
        )

        AND (
            :prioridad = 'Todas las prioridades'
            OR prioridad = :prioridad
        )

        ORDER BY
        CASE WHEN :orden = 'Nombre' THEN titulo END COLLATE NOCASE ASC,
        CASE WHEN :orden = 'Fecha' THEN fecha END ASC,
        CASE WHEN :orden = 'Prioridad' THEN
            CASE prioridad
                WHEN 'Alta' THEN 1
                WHEN 'Media' THEN 2
                WHEN 'Baja' THEN 3
                ELSE 4
            END
        END ASC,
        id DESC
    """)
    fun filtrarTareas(
        usuarioId: Int,
        filtro: String,
        hoy: String,
        busqueda: String,
        categoria: String,
        prioridad: String,
        orden: String
    ): Flow<List<Tarea>>

    // Cambiar estado de una tarea verificando al propietario
    @Query("""
        UPDATE tareas
        SET estado = :estado, completada = :completada
        WHERE id = :id AND usuarioId = :usuarioId
    """)
    suspend fun actualizarEstadoTareaPorUsuario(
        id: Int,
        usuarioId: Int,
        estado: String,
        completada: Boolean
    ): Int

    // Eliminar tarea verificando al propietario
    @Query("""
        DELETE FROM tareas
        WHERE id = :id AND usuarioId = :usuarioId
    """)
    suspend fun eliminarTareaPorIdYUsuario(id: Int, usuarioId: Int): Int

    // Categorías
    @Insert
    suspend fun insertarCategoria(categoria: Categoria)

    @Delete
    suspend fun eliminarCategoria(categoria: Categoria)

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun obtenerCategorias(): Flow<List<Categoria>>

    // Usuarios
    @Insert
    suspend fun insertarUsuario(usuario: Usuario)

    @Update
    suspend fun actualizarUsuario(usuario: Usuario)

    @Delete
    suspend fun eliminarUsuario(usuario: Usuario)

    @Query("SELECT * FROM usuarios ORDER BY nombre ASC")
    fun obtenerUsuarios(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun buscarUsuarioPorCorreo(correo: String): Usuario?

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contarUsuarios(): Int

    @Query("""
        UPDATE usuarios
        SET tipoUsuario = 'Administrador'
        WHERE id = (SELECT MIN(id) FROM usuarios)
        AND NOT EXISTS (
            SELECT 1 FROM usuarios
            WHERE tipoUsuario = 'Administrador'
        )
    """)
    suspend fun asignarPrimerAdministrador()

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerUsuarioPorId(id: Int): Usuario?

    @Query("""
        SELECT * FROM usuarios
        WHERE correo = :correo AND id != :id
        LIMIT 1
    """)
    suspend fun buscarOtroUsuarioConCorreo(correo: String, id: Int): Usuario?
}
