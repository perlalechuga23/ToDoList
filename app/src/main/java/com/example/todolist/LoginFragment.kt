package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.todolist.data.AppDatabase
import com.example.todolist.data.PasswordUtils
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etCorreo = view.findViewById<EditText>(R.id.etCorreo)
        val etContrasena = view.findViewById<EditText>(R.id.etContrasena)

        val btnIniciarSesion =
            view.findViewById<Button>(R.id.btnIniciarSesion)

        val btnIrRegistro =
            view.findViewById<Button>(R.id.btnIrRegistro)

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()

        btnIniciarSesion.setOnClickListener {
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Ingresa tu correo y contraseña",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnIniciarSesion.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Asignar administrador si todavía no existe uno
                    dao.asignarPrimerAdministrador()

                    val usuario = dao.buscarUsuarioPorCorreo(correo)

                    if (usuario != null &&
                        PasswordUtils.verificar(
                            contrasena,
                            usuario.contrasenaHash
                        )
                    ) {

                        // Guardar la sesión del usuario
                        requireContext()
                            .getSharedPreferences("sesion", Context.MODE_PRIVATE)
                            .edit()
                            .putInt("usuario_id", usuario.id)
                            .putString("tipo_usuario", usuario.tipoUsuario)
                            .apply()

                        Toast.makeText(
                            requireContext(),
                            "Bienvenido, ${usuario.nombre}",
                            Toast.LENGTH_SHORT
                        ).show()

                        parentFragmentManager.beginTransaction()
                            .replace(
                                R.id.contenedorFragmentos,
                                ListaTareasFragment()
                            )
                            .commit()

                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Correo o contraseña incorrectos",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al iniciar sesión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnIniciarSesion.isEnabled = true
                }
            }
        }

        btnIrRegistro.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.contenedorFragmentos,
                    RegistroFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }
}
