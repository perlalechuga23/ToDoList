package com.example.todolist.data

import kotlinx.coroutines.flow.Flow

class TareaRepository(private val tareaDao: TareaDao) {

    // Obtener tareas de un usuario
    fun tareasDeUsuario(usuarioId: Int): Flow<List<Tarea>> {
        return tareaDao.obtenerTareasPorUsuario(usuarioId)
    }

    // Buscar, filtrar y ordenar tareas desde Room
    fun filtrarTareas(
        usuarioId: Int,
        filtro: String,
        hoy: String,
        busqueda: String,
        categoria: String,
        prioridad: String,
        orden: String
    ): Flow<List<Tarea>> {
        return tareaDao.filtrarTareas(
            usuarioId,
            filtro,
            hoy,
            busqueda,
            categoria,
            prioridad,
            orden
        )
    }

    // Funciones de tareas
    suspend fun insertar(tarea: Tarea) {
        tareaDao.insertarTarea(tarea)
    }

    suspend fun actualizar(tarea: Tarea) {
        tareaDao.actualizarTarea(tarea)
    }

    suspend fun eliminar(tarea: Tarea) {
        tareaDao.eliminarTarea(tarea)
    }

    // Funciones de categorías
    val todasLasCategorias: Flow<List<Categoria>> =
        tareaDao.obtenerCategorias()

    suspend fun insertarCategoria(categoria: Categoria) {
        tareaDao.insertarCategoria(categoria)
    }

    suspend fun eliminarCategoria(categoria: Categoria) {
        tareaDao.eliminarCategoria(categoria)
    }
}
