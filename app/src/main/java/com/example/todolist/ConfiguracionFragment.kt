package com.example.todolist

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class ConfiguracionFragment : Fragment(R.layout.fragment_configuracion) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val preferencias = requireContext().getSharedPreferences(
            "preferencias_app",
            Context.MODE_PRIVATE
        )

        // Regresar
        val btnRegresar =
            view.findViewById<TextView>(R.id.btnRegresarConfiguracion)

        btnRegresar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Mi perfil
        val opcionMiPerfil =
            view.findViewById<View>(R.id.opcionMiPerfil)

        opcionMiPerfil.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, PerfilFragment())
                .addToBackStack(null)
                .commit()
        }

        // Notificaciones
        val opcionNotificaciones =
            view.findViewById<View>(R.id.opcionNotificaciones)

        opcionNotificaciones.setOnClickListener {

            val activadas = preferencias.getBoolean(
                "notificaciones",
                true
            )

            val opciones = arrayOf("Activadas", "Desactivadas")

            AlertDialog.Builder(requireContext())
                .setTitle("Notificaciones")
                .setSingleChoiceItems(
                    opciones,
                    if (activadas) 0 else 1
                ) { dialog, seleccion ->

                    val nuevoEstado = seleccion == 0

                    preferencias.edit()
                        .putBoolean("notificaciones", nuevoEstado)
                        .apply()

                    Toast.makeText(
                        requireContext(),
                        if (nuevoEstado)
                            "Notificaciones activadas"
                        else
                            "Notificaciones desactivadas",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Vista clara u oscura
        val opcionVista =
            view.findViewById<View>(R.id.opcionVista)

        opcionVista.setOnClickListener {

            val vistaGuardada = preferencias.getString(
                "vista",
                "clara"
            )

            val opciones = arrayOf("Clara", "Oscura")

            AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar vista")
                .setSingleChoiceItems(
                    opciones,
                    if (vistaGuardada == "oscura") 1 else 0
                ) { dialog, seleccion ->

                    val nuevaVista =
                        if (seleccion == 0) "clara" else "oscura"

                    preferencias.edit()
                        .putString("vista", nuevaVista)
                        .apply()

                    AppCompatDelegate.setDefaultNightMode(
                        if (seleccion == 0)
                            AppCompatDelegate.MODE_NIGHT_NO
                        else
                            AppCompatDelegate.MODE_NIGHT_YES
                    )

                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        // Administrar categorías
        val opcionCategorias =
            view.findViewById<View>(R.id.opcionAdministrarCategorias)

        opcionCategorias.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedorFragmentos, CategoriasFragment())
                .addToBackStack(null)
                .commit()
        }

        // Cerrar sesión
        val opcionCerrarSesion =
            view.findViewById<View>(R.id.opcionCerrarSesion)

        opcionCerrarSesion.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás segura de que deseas cerrar sesión?")
                .setPositiveButton("Sí, cerrar sesión") { _, _ ->

                    // Eliminar los datos de la sesión actual
                    requireContext()
                        .getSharedPreferences("sesion", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()

                    // Limpiar el historial de navegación
                    parentFragmentManager.popBackStack(
                        null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )

                    // Regresar al inicio de sesión
                    parentFragmentManager.beginTransaction()
                        .replace(
                            R.id.contenedorFragmentos,
                            LoginFragment()
                        )
                        .commit()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
