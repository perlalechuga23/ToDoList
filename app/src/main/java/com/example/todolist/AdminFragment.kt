package com.example.todolist

import com.example.todolist.data.AppDatabase
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminFragment : Fragment(R.layout.fragment_admin) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencias = requireContext()
            .getSharedPreferences("sesion", Context.MODE_PRIVATE)

        val usuarioId = preferencias.getInt("usuario_id", -1)

        if (usuarioId == -1) {
            bloquearAcceso()
            return
        }

        val dao = AppDatabase.obtenerBaseDatos(requireContext()).tareaDao()

        viewLifecycleOwner.lifecycleScope.launch {

            val usuario = withContext(Dispatchers.IO) {
                dao.obtenerUsuarioPorId(usuarioId)
            }

            if (!isAdded) return@launch

            if (usuario == null || usuario.tipoUsuario != "Administrador") {
                bloquearAcceso()
                return@launch
            }

            mostrarOpciones(view)
        }
    }

    private fun bloquearAcceso() {
        Toast.makeText(
            requireContext(),
            "No tienes permisos para acceder al panel",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }

    private fun mostrarOpciones(view: View) {

        val btnRegresar =
            view.findViewById<TextView>(R.id.btnRegresarAdmin)

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val opcionCategorias =
            view.findViewById<View>(R.id.opcionCategoriasAdmin)

        opcionCategorias.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, CategoriasFragment())
                .addToBackStack(null)
                .commit()
        }

        val opcionConfiguracion =
            view.findViewById<View>(R.id.opcionConfiguracionAdmin)

        opcionConfiguracion.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, ConfiguracionFragment())
                .addToBackStack(null)
                .commit()
        }

        val opcionUsuarios =
            view.findViewById<View>(R.id.opcionUsuariosAdmin)

        opcionUsuarios.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, UsuariosFragment())
                .addToBackStack(null)
                .commit()
        }

        val opcionPermisos =
            view.findViewById<View>(R.id.opcionPermisosAdmin)

        opcionPermisos.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, PermisosFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
