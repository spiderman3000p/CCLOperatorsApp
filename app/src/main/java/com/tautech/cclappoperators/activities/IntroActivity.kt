package com.tautech.cclappoperators.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro
import com.github.appintro.AppIntroFragment
import com.github.appintro.AppIntroPageTransformerType
import com.tautech.cclappoperators.R

class IntroActivity : AppIntro() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_intro)
        // Call addSlide passing your Fragments.
        // You can use AppIntroFragment to use a pre-built fragment
        addSlide(AppIntroFragment.newInstance(
            title = "Bienvenido...",
            description = "Esta es la aplicacion para los operadores de CCL",
            backgroundDrawable = R.drawable.truck_driver2,
        ))
        addSlide(AppIntroFragment.newInstance(
            title = "...Maneja tus tareas",
            description = "Desde aca puedes gestionar todas las planificaciones",
            backgroundDrawable = R.drawable.high_angle_delivery_man_checking_packages_list2,
        ))
        addSlide(AppIntroFragment.newInstance(
            title = "...Necesitamos ciertos permisos",
            description = "Por favor aprueba todos los permisos que necesitamos para funcionar correctamente",
            backgroundDrawable = R.drawable.zebra_tc25_2_800x414__3_,
        ))
        addSlide(AppIntroFragment.newInstance(
            title = "Empecemos!",
            description = "Ahora debes iniciar sesion con tus credenciales de operador para poder usar esta aplicacion",
            backgroundDrawable = R.drawable.class_a_truck_driver_628x419__2_,
        ))
        askForPermissions(
            permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                /*Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET*/
            ),
            slideNumber = 3,
            required = true)
        setTransformer(AppIntroPageTransformerType.Zoom)
    }

    override fun onSkipPressed(currentFragment: Fragment?) {
        super.onSkipPressed(currentFragment)
        // Decide what to do when the user clicks on "Skip"
        saveIntroDone()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Decide what to do when the user clicks on "Done"
        saveIntroDone()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    fun saveIntroDone() {
        val sharedPref = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("firstRun", true)
            commit()
        }
    }

    override fun onUserDeniedPermission(permissionName: String) {
        // User pressed "Deny" on the permission dialog
        showAlert(getString(R.string.warning), getString(R.string.user_denied_permission_msg))
    }
    override fun onUserDisabledPermission(permissionName: String) {
        // User pressed "Deny" + "Don't ask again" on the permission dialog
        showAlert(getString(R.string.warning), getString(R.string.user_denied_permission_msg))
    }

    fun showAlert(title: String, message: String) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(title)
            builder.setMessage(message)
            builder.setPositiveButton("Aceptar", null)
            val dialog: AlertDialog = builder.create();
            dialog.show();
        }
    }
}