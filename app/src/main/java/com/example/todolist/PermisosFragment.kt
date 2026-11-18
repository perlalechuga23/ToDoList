package com.example.todolist

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.todolist.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermisosFragment : Fragment(R.layout.fragment_permisos) {

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

            val btnRegresar =
                view.findViewById<TextView>(R.id.btnRegresarPermisos)

            btnRegresar.setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun bloquearAcceso() {
        Toast.makeText(
            requireContext(),
            "No tienes permisos para acceder",
            Toast.LENGTH_SHORT
        ).show()

        parentFragmentManager.popBackStack()
    }
}
