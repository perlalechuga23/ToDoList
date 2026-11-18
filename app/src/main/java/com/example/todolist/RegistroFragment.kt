package com.example.todolist

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.todolist.data.AppDatabase
import com.example.todolist.data.PasswordUtils
import com.example.todolist.data.Usuario
import kotlinx.coroutines.launch

class RegistroFragment : Fragment(R.layout.fragment_registro) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etCorreo = view.findViewById<EditText>(R.id.etCorreoRegistro)
        val etContrasena = view.findViewById<EditText>(R.id.etContrasenaRegistro)
        val etConfirmar = view.findViewById<EditText>(R.id.etConfirmarContrasena)

        val btnCrearCuenta = view.findViewById<Button>(R.id.btnCrearCuenta)
        val btnRegresar = view.findViewById<TextView>(R.id.btnRegresarRegistro)

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()

        btnCrearCuenta.setOnClickListener {

            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString()
            val confirmar = etConfirmar.text.toString()

            if (nombre.isEmpty() || correo.isEmpty() ||
                contrasena.isEmpty() || confirmar.isEmpty()
            ) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(requireContext(), "Ingresa un correo válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena.length < 8) {
                Toast.makeText(requireContext(), "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contrasena != confirmar) {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnCrearCuenta.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val usuarioExistente = dao.buscarUsuarioPorCorreo(correo)

                    if (usuarioExistente != null) {
                        Toast.makeText(
                            requireContext(),
                            "Este correo ya está registrado",
                            Toast.LENGTH_SHORT
                        ).show()

                    } else {

                        // Promover la cuenta más antigua si aún no existe administrador
                        dao.asignarPrimerAdministrador()

                        // Contar las cuentas registradas
                        val totalUsuarios = dao.contarUsuarios()

                        val tipoUsuario = if (totalUsuarios == 0) {
                            "Administrador"
                        } else {
                            "Usuario"
                        }

                        val usuario = Usuario(
                            nombre = nombre,
                            correo = correo,
                            contrasenaHash = PasswordUtils.hash(contrasena),
                            tipoUsuario = tipoUsuario
                        )

                        dao.insertarUsuario(usuario)

                        Toast.makeText(
                            requireContext(),
                            "Cuenta creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        parentFragmentManager.beginTransaction()
                            .replace(R.id.contenedorFragmentos, LoginFragment())
                            .commit()
                    }

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al crear la cuenta: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {
                    btnCrearCuenta.isEnabled = true
                }
            }
        }

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
