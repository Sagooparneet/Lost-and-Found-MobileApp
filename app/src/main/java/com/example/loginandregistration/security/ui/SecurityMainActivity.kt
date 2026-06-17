package com.example.loginandregistration.security.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.loginandregistration.R
import com.example.loginandregistration.databinding.ActivitySecurityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class SecurityMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySecurityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // The view is now set

        // =======================================================
        // FIX: Tell the activity to use our new Toolbar
        // =======================================================
        setSupportActionBar(binding.toolbar)

        // 1. Get the NavHostFragment from the layout.
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_security) as NavHostFragment

        // 2. Get the NavController from the NavHostFragment.
        val navController = navHostFragment.navController

        val navView: BottomNavigationView = binding.securityBottomNavView

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_security_dashboard,
                R.id.navigation_security_reports,
                R.id.navigation_security_create,
                R.id.navigation_security_profile
            )
        )

        // This line will now work because we have set a support action bar
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}
