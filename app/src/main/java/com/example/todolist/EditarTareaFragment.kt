package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.todolist.data.AppDatabase
import com.example.todolist.data.Tarea
import com.example.todolist.data.TareaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditarTareaFragment : Fragment(R.layout.fragment_nueva_tarea) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRegresar = view.findViewById<TextView>(R.id.btnRegresarTarea)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarTarea)
        val etTitulo = view.findViewById<EditText>(R.id.etTituloTarea)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcionTarea)
        val rgPrioridad = view.findViewById<RadioGroup>(R.id.rgPrioridad)
        val spCategoria = view.findViewById<Spinner>(R.id.spCategoria)
        val spEstado = view.findViewById<Spinner>(R.id.spEstado)

        // Recuperar los datos de la tarea
        val argumentos = requireArguments()

        val id = argumentos.getInt("id")
        val tituloOriginal = argumentos.getString("titulo") ?: ""
        val descripcionOriginal = argumentos.getString("descripcion") ?: ""
        val prioridadOriginal = argumentos.getString("prioridad") ?: ""
        val categoriaOriginal = argumentos.getString("categoria") ?: ""
        val completada = argumentos.getBoolean("completada")
        val estadoOriginal = argumentos.getString("estado")
            ?: if (completada) "Completada" else "Pendiente"
        val fecha = argumentos.getString("fecha") ?: ""

        // Obtener el usuario de la sesión activa
        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        val usuarioId = preferencias.getInt("usuario_id", -1)

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "No hay una sesión activa",
                Toast.LENGTH_SHORT
            ).show()

            parentFragmentManager.popBackStack()
            return
        }

        // Conectar con Room
        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()
        val repositorio = TareaRepository(dao)

        // Comprobar que la tarea pertenece al usuario
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tareaOriginal = withContext(Dispatchers.IO) {
                    dao.obtenerTareaPorIdYUsuario(id, usuarioId)
                }

                if (tareaOriginal == null) {
                    Toast.makeText(
                        requireContext(),
                        "No tienes permiso para modificar esta tarea",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                    return@launch
                }

                // Mostrar los datos actuales
                etTitulo.setText(tareaOriginal.titulo)
                etDescripcion.setText(tareaOriginal.descripcion)

                // Cargar categorías
                val categorias = mutableListOf<String>()

                val adaptadorCategorias = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    categorias
                )

                adaptadorCategorias.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spCategoria.adapter = adaptadorCategorias

                // Cargar categorías disponibles
                launch {
                    repositorio.todasLasCategorias.collect { lista ->
                        categorias.clear()
                        categorias.addAll(lista.map { it.nombre })

                        if (categoriaOriginal.isNotEmpty() &&
                            !categorias.contains(categoriaOriginal)
                        ) {
                            categorias.add(categoriaOriginal)
                        }

                        if (categorias.isEmpty()) {
                            categorias.add("Sin categoría")
                        }

                        adaptadorCategorias.notifyDataSetChanged()

                        val posicion = categorias.indexOf(tareaOriginal.categoria)
                        if (posicion >= 0) {
                            spCategoria.setSelection(posicion)
                        }
                    }
                }

                // Cargar estados
                val estados = listOf(
                    "Pendiente",
                    "En progreso",
                    "Completada"
                )

                val adaptadorEstados = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    estados
                )

                adaptadorEstados.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spEstado.adapter = adaptadorEstados

                val posicionEstado = estados.indexOf(tareaOriginal.estado)

                spEstado.setSelection(
                    if (posicionEstado >= 0) posicionEstado else 0
                )

                // Seleccionar prioridad original
                for (i in 0 until rgPrioridad.childCount) {
                    val boton = rgPrioridad.getChildAt(i)

                    if (boton is RadioButton &&
                        boton.text.toString() == tareaOriginal.prioridad
                    ) {
                        rgPrioridad.check(boton.id)
                        break
                    }
                }

                btnGuardar.text = "Guardar cambios"

                // Guardar cambios
                btnGuardar.setOnClickListener {

                    val titulo = etTitulo.text.toString().trim()
                    val descripcion = etDescripcion.text.toString().trim()

                    if (titulo.isEmpty()) {
                        etTitulo.error = "Escribe el título"
                        return@setOnClickListener
                    }

                    if (rgPrioridad.checkedRadioButtonId == -1) {
                        Toast.makeText(
                            requireContext(),
                            "Selecciona una prioridad",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val prioridadId = rgPrioridad.checkedRadioButtonId

                    val prioridad = view.findViewById<RadioButton>(
                        prioridadId
                    ).text.toString()

                    val categoria =
                        spCategoria.selectedItem?.toString()
                            ?: tareaOriginal.categoria

                    val estado =
                        spEstado.selectedItem?.toString() ?: "Pendiente"

                    val tareaModificada = tareaOriginal.copy(
                        titulo = titulo,
                        descripcion = descripcion,
                        prioridad = prioridad,
                        categoria = categoria,
                        completada = estado == "Completada",
                        estado = estado
                    )

                    btnGuardar.isEnabled = false

                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            // Volver a comprobar el propietario antes de actualizar
                            val tareaVerificada = withContext(Dispatchers.IO) {
                                dao.obtenerTareaPorIdYUsuario(id, usuarioId)
                            }

                            if (tareaVerificada == null) {
                                Toast.makeText(
                                    requireContext(),
                                    "No tienes permiso para modificar esta tarea",
                                    Toast.LENGTH_SHORT
                                ).show()

                                parentFragmentManager.popBackStack()
                                return@launch
                            }

                            withContext(Dispatchers.IO) {
                                repositorio.actualizar(tareaModificada)
                            }

                            Toast.makeText(
                                requireContext(),
                                "Tarea actualizada correctamente",
                                Toast.LENGTH_SHORT
                            ).show()

                            parentFragmentManager.popBackStack()

                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                "Error al actualizar: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {
                            btnGuardar.isEnabled = true
                        }
                    }
                }

            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar la tarea: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                parentFragmentManager.popBackStack()
            }
        }

        // Regresar
        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
