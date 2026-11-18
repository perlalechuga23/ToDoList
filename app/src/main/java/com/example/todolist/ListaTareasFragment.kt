package com.example.todolist

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.todolist.data.AppDatabase
import com.example.todolist.data.Tarea
import com.example.todolist.data.TareaDao
import com.example.todolist.data.TareaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ListaTareasFragment : Fragment(R.layout.fragment_lista_tarea) {

    private lateinit var repositorio: TareaRepository
    private lateinit var dao: TareaDao

    private var tareasActuales = emptyList<Tarea>()
    private var tareasJob: Job? = null

    private var filtroActual = "Todas"
    private var busquedaActual = ""
    private var ordenActual = "Nombre"
    private var categoriaActual = "Todas las categorías"
    private var prioridadActual = "Todas las prioridades"

    private var usuarioId = -1

    private lateinit var contenedor: LinearLayout
    private lateinit var spinnerOrden: Spinner
    private lateinit var spinnerCategoria: Spinner
    private lateinit var spinnerPrioridad: Spinner

    private val prioridades = listOf(
        "Todas las prioridades",
        "Alta",
        "Media",
        "Baja"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        usuarioId = preferencias.getInt("usuario_id", -1)

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "Debes iniciar sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()
        repositorio = TareaRepository(dao)

        contenedor = view.findViewById(R.id.contenedorListaTareas)
        spinnerOrden = view.findViewById(R.id.spOrdenarTareas)
        spinnerCategoria = view.findViewById(R.id.spFiltrarCategoria)
        spinnerPrioridad = view.findViewById(R.id.spFiltrarPrioridad)

        configurarEncabezado(view)
        configurarFiltros(view)
        configurarBuscador(view)
        configurarOrdenamiento()
        configurarPrioridades()

        cargarCategorias()
        cargarTareas()
    }

    private fun configurarEncabezado(view: View) {

        val btnMenu = view.findViewById<TextView>(R.id.btnMenu)
        val btnConfiguracion =
            view.findViewById<TextView>(R.id.btnConfiguracion)
        val btnNuevaTarea =
            view.findViewById<Button>(R.id.btnNuevaTarea)

        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        val tipoUsuario =
            preferencias.getString("tipo_usuario", "Usuario")

        btnMenu.visibility =
            if (tipoUsuario == "Administrador") View.VISIBLE
            else View.GONE

        btnMenu.setOnClickListener {
            abrirFragmento(AdminFragment())
        }

        btnConfiguracion.setOnClickListener {
            abrirFragmento(ConfiguracionFragment())
        }

        btnNuevaTarea.setOnClickListener {
            abrirFragmento(NuevaTareaFragment())
        }
    }

    private fun configurarFiltros(view: View) {

        view.findViewById<Button>(R.id.btnTodas).setOnClickListener {
            filtroActual = "Todas"
            cargarTareas()
        }

        view.findViewById<Button>(R.id.btnHoy).setOnClickListener {
            filtroActual = "Hoy"
            cargarTareas()
        }

        view.findViewById<Button>(R.id.btnPendientes).setOnClickListener {
            filtroActual = "Pendiente"
            cargarTareas()
        }

        view.findViewById<Button>(R.id.btnEnProgreso).setOnClickListener {
            filtroActual = "En progreso"
            cargarTareas()
        }

        view.findViewById<Button>(R.id.btnCompletadas).setOnClickListener {
            filtroActual = "Completada"
            cargarTareas()
        }
    }

    private fun configurarBuscador(view: View) {

        val buscador = view.findViewById<EditText>(R.id.etBuscarTarea)

        buscador.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                busquedaActual = s.toString()
                cargarTareas()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarOrdenamiento() {

        val opciones = listOf("Nombre", "Fecha", "Prioridad")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            opciones
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerOrden.adapter = adapter

        spinnerOrden.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    ordenActual = opciones[position]
                    cargarTareas()
                }
            }
    }

    private fun configurarPrioridades() {

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            prioridades
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerPrioridad.adapter = adapter

        spinnerPrioridad.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    prioridadActual = prioridades[position]
                    cargarTareas()
                }
            }
    }

    private fun cargarCategorias() {

        viewLifecycleOwner.lifecycleScope.launch {

            repositorio.todasLasCategorias.collect { categorias ->

                if (!isAdded) return@collect

                val opciones = mutableListOf("Todas las categorías")
                opciones.addAll(categorias.map { it.nombre })

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    opciones
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spinnerCategoria.adapter = adapter

                spinnerCategoria.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {

                        override fun onNothingSelected(
                            parent: AdapterView<*>?
                        ) {}

                        override fun onItemSelected(
                            parent: AdapterView<*>?,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            categoriaActual = opciones[position]
                            cargarTareas()
                        }
                    }
            }
        }
    }

    // Consultar tareas directamente desde Room
    private fun cargarTareas() {

        tareasJob?.cancel()

        tareasJob = viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                val hoy = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())

                repositorio.filtrarTareas(
                    usuarioId = usuarioId,
                    filtro = filtroActual,
                    hoy = hoy,
                    busqueda = busquedaActual.trim(),
                    categoria = categoriaActual,
                    prioridad = prioridadActual,
                    orden = ordenActual
                ).collect { tareas ->

                    tareasActuales = tareas
                    mostrarTareas()
                }
            }
        }
    }

    // Mostrar los resultados que ya entrega Room
    private fun mostrarTareas() {

        if (!isAdded || !::contenedor.isInitialized) return

        contenedor.removeAllViews()

        if (tareasActuales.isEmpty()) {

            val mensaje = TextView(requireContext())

            mensaje.text = "No hay tareas para mostrar"
            mensaje.textSize = 17f
            mensaje.setTextColor(
                resources.getColor(
                    R.color.texto_secundario,
                    null
                )
            )
            mensaje.setPadding(12, 24, 12, 24)

            contenedor.addView(mensaje)

            return
        }

        tareasActuales.forEach { tarea ->
            agregarTarjeta(tarea)
        }
    }

    private fun agregarTarjeta(tarea: Tarea) {

        val tarjeta = layoutInflater.inflate(
            R.layout.item_tarea,
            contenedor,
            false
        )

        val tvEstado = tarjeta.findViewById<TextView>(R.id.tvEstado)
        val tvTitulo = tarjeta.findViewById<TextView>(R.id.tvTituloTarea)
        val tvDescripcion =
            tarjeta.findViewById<TextView>(R.id.tvDescripcionTarea)
        val tvPrioridad =
            tarjeta.findViewById<TextView>(R.id.tvPrioridadTarea)
        val tvCategoria =
            tarjeta.findViewById<TextView>(R.id.tvCategoriaTarea)

        val spinnerEstado =
            tarjeta.findViewById<Spinner>(R.id.spEstadoTarea)

        val btnEditar =
            tarjeta.findViewById<Button>(R.id.btnEditarTarea)

        val btnEliminar =
            tarjeta.findViewById<Button>(R.id.btnEliminarTarea)

        tvEstado.text = "Estado de la tarea"
        tvTitulo.text = tarea.titulo
        tvDescripcion.text = tarea.descripcion
        tvPrioridad.text = "Prioridad: ${tarea.prioridad}"
        tvCategoria.text = "Categoría: ${tarea.categoria}"

        val estados = listOf(
            "Pendiente",
            "En progreso",
            "Completada"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            estados
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerEstado.adapter = adapter

        val posicion = estados.indexOf(tarea.estado)

        spinnerEstado.setSelection(
            if (posicion >= 0) posicion else 0
        )

        var primeraSeleccion = true

        spinnerEstado.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (primeraSeleccion) {
                        primeraSeleccion = false
                        return
                    }

                    val nuevoEstado = estados[position]

                    if (nuevoEstado == tarea.estado) return

                    viewLifecycleOwner.lifecycleScope.launch {

                        try {

                            val resultado = withContext(Dispatchers.IO) {
                                dao.actualizarEstadoTareaPorUsuario(
                                    tarea.id,
                                    usuarioId,
                                    nuevoEstado,
                                    nuevoEstado == "Completada"
                                )
                            }

                            if (resultado == 0) {
                                Toast.makeText(
                                    requireContext(),
                                    "No tienes permiso para modificar esta tarea",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                requireContext(),
                                "No se pudo actualizar la tarea",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        btnEditar.setOnClickListener {

            val bundle = Bundle().apply {
                putInt("id", tarea.id)
                putString("titulo", tarea.titulo)
                putString("descripcion", tarea.descripcion)
                putString("prioridad", tarea.prioridad)
                putString("categoria", tarea.categoria)
                putBoolean("completada", tarea.completada)
                putString("estado", tarea.estado)
                putString("fecha", tarea.fecha)
                putInt("usuarioId", tarea.usuarioId)
            }

            val fragmento = EditarTareaFragment()
            fragmento.arguments = bundle

            abrirFragmento(fragmento)
        }

        btnEliminar.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar tarea")
                .setMessage("¿Estás segura de que deseas eliminar esta tarea?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar") { _, _ ->

                    viewLifecycleOwner.lifecycleScope.launch {

                        try {

                            val resultado = withContext(Dispatchers.IO) {
                                dao.eliminarTareaPorIdYUsuario(
                                    tarea.id,
                                    usuarioId
                                )
                            }

                            if (resultado > 0) {

                                Toast.makeText(
                                    requireContext(),
                                    "Tarea eliminada",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    requireContext(),
                                    "No tienes permiso para eliminar esta tarea",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                requireContext(),
                                "No se pudo eliminar la tarea",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .show()
        }

        contenedor.addView(tarjeta)
    }

    private fun abrirFragmento(fragmento: Fragment) {

        parentFragmentManager.beginTransaction()
            .replace(R.id.contenedorFragmentos, fragmento)
            .addToBackStack(null)
            .commit()
    }
}
