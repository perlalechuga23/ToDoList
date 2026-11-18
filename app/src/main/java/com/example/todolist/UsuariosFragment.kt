package com.example.todolist

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.todolist.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsuariosFragment : Fragment(R.layout.fragment_usuarios) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        val usuarioActualId = preferencias.getInt("usuario_id", -1)

        if (usuarioActualId == -1) {
            bloquearAcceso()
            return
        }

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()

        viewLifecycleOwner.lifecycleScope.launch {

            val usuarioActual = withContext(Dispatchers.IO) {
                dao.obtenerUsuarioPorId(usuarioActualId)
            }

            if (!isAdded) return@launch

            if (usuarioActual == null ||
                usuarioActual.tipoUsuario != "Administrador"
            ) {
                bloquearAcceso()
                return@launch
            }

            mostrarUsuarios(view, usuarioActualId, dao)
        }
    }

    private fun bloquearAcceso() {
        Toast.makeText(
            requireContext(),
            "No tienes permisos para administrar usuarios",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun mostrarUsuarios(
        view: View,
        usuarioActualId: Int,
        dao: com.example.todolist.data.TareaDao
    ) {

        val btnRegresar =
            view.findViewById<TextView>(R.id.btnRegresarUsuarios)

        val contenedor =
            view.findViewById<LinearLayout>(R.id.contenedorListaUsuarios)

        val tvVacio =
            view.findViewById<TextView>(R.id.tvListaUsuariosVacia)

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                dao.obtenerUsuarios().collect { usuarios ->

                    contenedor.removeAllViews()

                    if (usuarios.isEmpty()) {
                        contenedor.addView(tvVacio)

                    } else {

                        usuarios.forEach { usuario ->

                            val tarjeta = LinearLayout(requireContext()).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(20, 18, 20, 18)

                                setBackgroundColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.fondo_tarjeta
                                    )
                                )
                            }

                            val nombre = TextView(requireContext()).apply {
                                text = usuario.nombre
                                textSize = 18f

                                setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.texto_secundario
                                    )
                                )
                            }

                            val correo = TextView(requireContext()).apply {
                                text = usuario.correo
                                textSize = 15f

                                setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.texto_principal
                                    )
                                )
                            }

                            val tipo = TextView(requireContext()).apply {
                                text = "Tipo de usuario: ${usuario.tipoUsuario}"
                                textSize = 14f

                                setTextColor(
                                    ContextCompat.getColor(
                                        requireContext(),
                                        R.color.texto_principal
                                    )
                                )
                            }

                            tarjeta.addView(nombre)
                            tarjeta.addView(correo)
                            tarjeta.addView(tipo)

                            if (usuario.id != usuarioActualId) {

                                val btnEliminar =
                                    Button(requireContext()).apply {

                                        text = "Eliminar usuario"

                                        setTextColor(
                                            ContextCompat.getColor(
                                                requireContext(),
                                                android.R.color.white
                                            )
                                        )

                                        backgroundTintList =
                                            ContextCompat.getColorStateList(
                                                requireContext(),
                                                R.color.boton_principal
                                            )
                                    }

                                btnEliminar.setOnClickListener {

                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Eliminar usuario")
                                        .setMessage(
                                            "¿Deseas eliminar a ${usuario.nombre}? " +
                                                    "También se eliminarán todas sus tareas."
                                        )
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton("Eliminar") { _, _ ->

                                            viewLifecycleOwner.lifecycleScope.launch {

                                                try {
                                                    val sigueSiendoAdmin =
                                                        withContext(Dispatchers.IO) {
                                                            dao.obtenerUsuarioPorId(
                                                                usuarioActualId
                                                            )
                                                        }

                                                    if (sigueSiendoAdmin == null ||
                                                        sigueSiendoAdmin.tipoUsuario != "Administrador"
                                                    ) {
                                                        bloquearAcceso()
                                                        return@launch
                                                    }

                                                    withContext(Dispatchers.IO) {
                                                        dao.eliminarUsuario(usuario)
                                                    }

                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Usuario eliminado correctamente",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                } catch (e: Exception) {
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "No se pudo eliminar el usuario",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                        .show()
                                }

                                tarjeta.addView(btnEliminar)
                            }

                            val parametros = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )

                            parametros.bottomMargin = 20

                            contenedor.addView(tarjeta, parametros)
                        }
                    }
                }
            }
        }
    }
}
