package com.example.todolist

import android.content.Context
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
import kotlinx.coroutines.launch

class PerfilFragment : Fragment(R.layout.fragment_perfil) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNombre = view.findViewById<EditText>(R.id.etNombrePerfil)
        val etCorreo = view.findViewById<EditText>(R.id.etCorreoPerfil)
        val etContrasena = view.findViewById<EditText>(R.id.etContrasenaPerfil)

        val btnGuardar = view.findViewById<Button>(R.id.btnGuardarPerfil)
        val btnRegresar = view.findViewById<TextView>(R.id.btnRegresarPerfil)

        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        val usuarioId = preferencias.getInt("usuario_id", -1)

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()

        if (usuarioId == -1) {
            Toast.makeText(
                requireContext(),
                "No hay una sesión activa",
                Toast.LENGTH_SHORT
            ).show()
            parentFragmentManager.popBackStack()
            return
        }

        var usuarioActual: com.example.todolist.data.Usuario? = null

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                usuarioActual = dao.obtenerUsuarioPorId(usuarioId)

                if (usuarioActual != null) {
                    etNombre.setText(usuarioActual!!.nombre)
                    etCorreo.setText(usuarioActual!!.correo)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró el usuario",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar el perfil",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnGuardar.setOnClickListener {

            val nombre = etNombre.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val contrasena = etContrasena.text.toString()

            if (nombre.isEmpty() || correo.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "El nombre y el correo son obligatorios",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(
                    requireContext(),
                    "Ingresa un correo válido",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (contrasena.isNotEmpty() && contrasena.length < 8) {
                Toast.makeText(
                    requireContext(),
                    "La contraseña debe tener al menos 8 caracteres",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val usuario = usuarioActual

                    if (usuario == null) {
                        Toast.makeText(
                            requireContext(),
                            "Espera a que cargue tu perfil",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val correoExistente =
                        dao.buscarOtroUsuarioConCorreo(correo, usuarioId)

                    if (correoExistente != null) {
                        Toast.makeText(
                            requireContext(),
                            "Ese correo ya pertenece a otra cuenta",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val usuarioActualizado = usuario.copy(
                        nombre = nombre,
                        correo = correo,
                        contrasenaHash = if (contrasena.isNotEmpty()) {
                            PasswordUtils.hash(contrasena)
                        } else {
                            usuario.contrasenaHash
                        }
                    )

                    dao.actualizarUsuario(usuarioActualizado)

                    usuarioActual = usuarioActualizado

                    Toast.makeText(
                        requireContext(),
                        "Perfil actualizado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    etContrasena.text.clear()

                    preferencias.edit()
                        .putString("tipo_usuario", usuarioActualizado.tipoUsuario)
                        .apply()

                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Error al guardar los cambios: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnGuardar.isEnabled = true
                }
            }
        }

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}
