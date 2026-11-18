package com.example.todolist.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tareas",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["usuarioId"])]
)
data class Tarea(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val titulo: String,
    val descripcion: String,
    val prioridad: String,
    val categoria: String,

    val completada: Boolean = false,

    @ColumnInfo(defaultValue = "''")
    val fecha: String = java.text.SimpleDateFormat(
        "yyyy-MM-dd",
        java.util.Locale.getDefault()
    ).format(java.util.Date()),

    @ColumnInfo(defaultValue = "0")
    val usuarioId: Int = 0,

    @ColumnInfo(defaultValue = "'Pendiente'")
    val estado: String = "Pendiente"
)