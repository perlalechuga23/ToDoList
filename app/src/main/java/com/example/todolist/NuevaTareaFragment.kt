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

class NuevaTareaFragment : Fragment(R.layout.fragment_nueva_tarea) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRegresar = view.findViewById<TextView>(R.id.btnRegresarTarea)
        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarTarea)
        val etTitulo = view.findViewById<EditText>(R.id.etTituloTarea)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcionTarea)
        val rgPrioridad = view.findViewById<RadioGroup>(R.id.rgPrioridad)
        val spCategoria = view.findViewById<Spinner>(R.id.spCategoria)
        val spEstado = view.findViewById<Spinner>(R.id.spEstado)

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()
        val repositorio = TareaRepository(dao)

        // Obtener el usuario que inició sesión
        val preferencias = requireContext().getSharedPreferences(
            "sesion",
            Context.MODE_PRIVATE
        )

        val usuarioId = preferencias.getInt("usuario_id", -1)

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

        viewLifecycleOwner.lifecycleScope.launch {
            repositorio.todasLasCategorias.collect { lista ->
                categorias.clear()

                if (lista.isEmpty()) {
                    categorias.add("Sin categoría")
                } else {
                    categorias.addAll(lista.map { it.nombre })
                }

                adaptadorCategorias.notifyDataSetChanged()
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
        spEstado.setSelection(0)

        // Regresar
        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Guardar tarea
        btnGuardar.setOnClickListener {

            val titulo = etTitulo.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (usuarioId == -1) {
                Toast.makeText(
                    requireContext(),
                    "No hay una sesión activa",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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
                spCategoria.selectedItem?.toString() ?: "Sin categoría"

            val estado =
                spEstado.selectedItem?.toString() ?: "Pendiente"

            val nuevaTarea = Tarea(
                titulo = titulo,
                descripcion = descripcion,
                prioridad = prioridad,
                categoria = categoria,
                completada = estado == "Completada",
                estado = estado,
                usuarioId = usuarioId
            )

            btnGuardar.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repositorio.insertar(nuevaTarea)
                    }

                    Toast.makeText(
                        requireContext(),
                        "Tarea guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al guardar la tarea: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {
                    btnGuardar.isEnabled = true
                }
            }
        }
    }
}
