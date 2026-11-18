package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.todolist.data.AppDatabase
import com.example.todolist.data.Categoria
import com.example.todolist.data.TareaDao
import com.example.todolist.data.TareaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriasFragment : Fragment(R.layout.fragment_categorias) {

    private lateinit var repositorio: TareaRepository
    private lateinit var dao: TareaDao
    private var usuarioActualId = -1
    private var categoriasActuales = emptyList<Categoria>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencias = requireContext().getSharedPreferences(
            "sesion",
            Context.MODE_PRIVATE
        )

        usuarioActualId = preferencias.getInt("usuario_id", -1)

        if (usuarioActualId == -1) {
            bloquearAcceso()
            return
        }

        dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()
        repositorio = TareaRepository(dao)

        viewLifecycleOwner.lifecycleScope.launch {

            val usuario = withContext(Dispatchers.IO) {
                dao.obtenerUsuarioPorId(usuarioActualId)
            }

            if (!isAdded) return@launch

            if (usuario == null || usuario.tipoUsuario != "Administrador") {
                bloquearAcceso()
                return@launch
            }

            mostrarPantalla(view)
        }
    }

    private fun bloquearAcceso() {
        Toast.makeText(
            requireContext(),
            "No tienes permisos para administrar categorías",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun verificarAdministrador(): Boolean {
        return true
    }

    private fun mostrarPantalla(view: View) {

        val btnRegresar =
            view.findViewById<TextView>(R.id.btnRegresarCategorias)

        val btnAgregar =
            view.findViewById<Button>(R.id.btnAgregarCategoria)

        val contenedor =
            view.findViewById<LinearLayout>(R.id.contenedorCategorias)

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repositorio.todasLasCategorias.collect { categorias ->
                    categoriasActuales = categorias
                    mostrarCategorias(contenedor)
                }
            }
        }

        btnAgregar.setOnClickListener {

            val entrada = EditText(requireContext())
            entrada.hint = "Nombre de la categoría"

            AlertDialog.Builder(requireContext())
                .setTitle("Nueva categoría")
                .setView(entrada)
                .setPositiveButton("Agregar") { _, _ ->

                    val nombre = entrada.text.toString().trim()

                    if (nombre.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "Escribe un nombre",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else if (categoriasActuales.any {
                            it.nombre.equals(nombre, ignoreCase = true)
                        }) {
                        Toast.makeText(
                            requireContext(),
                            "Esa categoría ya existe",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        viewLifecycleOwner.lifecycleScope.launch {

                            try {
                                val usuario = withContext(Dispatchers.IO) {
                                    dao.obtenerUsuarioPorId(usuarioActualId)
                                }

                                if (usuario == null ||
                                    usuario.tipoUsuario != "Administrador"
                                ) {
                                    bloquearAcceso()
                                    return@launch
                                }

                                withContext(Dispatchers.IO) {
                                    repositorio.insertarCategoria(
                                        Categoria(nombre = nombre)
                                    )
                                }

                                Toast.makeText(
                                    requireContext(),
                                    "Categoría agregada",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "No se pudo agregar la categoría",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarCategorias(contenedor: LinearLayout) {

        contenedor.removeAllViews()

        if (categoriasActuales.isEmpty()) {
            val mensaje = TextView(requireContext())
            mensaje.text = "No hay categorías registradas"
            mensaje.textSize = 16f

            mensaje.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.texto_secundario
                )
            )

            mensaje.setPadding(18, 18, 18, 18)
            contenedor.addView(mensaje)
            return
        }

        categoriasActuales.forEach { categoria ->

            val nuevaCategoria = TextView(requireContext())

            nuevaCategoria.text =
                "📁  ${categoria.nombre}   (Mantén presionado para eliminar)"

            nuevaCategoria.textSize = 16f

            nuevaCategoria.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.texto_principal
                )
            )

            nuevaCategoria.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.fondo_tarjeta
                )
            )

            nuevaCategoria.setPadding(18, 18, 18, 18)

            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            parametros.bottomMargin = 12

            contenedor.addView(nuevaCategoria, parametros)

            nuevaCategoria.setOnLongClickListener {

                AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar categoría")
                    .setMessage("¿Deseas eliminar ${categoria.nombre}?")
                    .setPositiveButton("Sí, eliminar") { _, _ ->

                        viewLifecycleOwner.lifecycleScope.launch {

                            try {
                                val usuario = withContext(Dispatchers.IO) {
                                    dao.obtenerUsuarioPorId(usuarioActualId)
                                }

                                if (usuario == null ||
                                    usuario.tipoUsuario != "Administrador"
                                ) {
                                    bloquearAcceso()
                                    return@launch
                                }

                                withContext(Dispatchers.IO) {
                                    repositorio.eliminarCategoria(categoria)
                                }

                                Toast.makeText(
                                    requireContext(),
                                    "Categoría eliminada",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    "No se pudo eliminar la categoría",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

                true
            }
        }
    }
}
