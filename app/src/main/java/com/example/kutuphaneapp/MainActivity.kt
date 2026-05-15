package com.example.kutuphaneapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.kutuphaneapp.viewmodel.SettingsViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private lateinit var altMenu: BottomNavigationView
    private var aktifFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // super.onCreate() çağrısı Hilt field injection'ı tetikler;
        // dataStore bu noktadan sonra erişilebilir hale gelir.
        super.onCreate(savedInstanceState)

        // setContentView() öncesinde tema uygula
        applyStoredTheme()

        val kimlikDogrulama = FirebaseAuth.getInstance()
        if (kimlikDogrulama.currentUser == null) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar: MaterialToolbar = findViewById(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        altMenu = findViewById(R.id.altMenu)
        altMenu.setOnItemSelectedListener { oge ->
            fragmentGoster(oge.itemId)
            true
        }

        if (savedInstanceState == null) {
            altMenu.selectedItemId = R.id.nav_anasayfa
        }

        observeThemeChanges()
    }

    private fun applyStoredTheme() {
        val mode = runBlocking {
            try {
                dataStore.data.map { it[SettingsViewModel.THEME_MODE] ?: "system" }.first()
            } catch (e: Exception) {
                "system"
            }
        }
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
    }

    private fun observeThemeChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStore.data.map { it[SettingsViewModel.THEME_MODE] ?: "system" }.collect { mode ->
                    AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
                }
            }
        }
    }

    private fun nightModeFor(mode: String) = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun seciliMenuOgesiniAyarla(ogeId: Int) {
        altMenu.selectedItemId = ogeId
    }

    private fun fragmentGoster(ogeId: Int) {
        val etiket = ogeId.toString()
        val yonetici = supportFragmentManager
        val mevcut = yonetici.findFragmentByTag(etiket)

        if (mevcut != null && mevcut == aktifFragment) return

        val islem = yonetici.beginTransaction()
        aktifFragment?.let { islem.hide(it) }

        if (mevcut != null) {
            islem.show(mevcut)
            aktifFragment = mevcut
        } else {
            val yeniFragment = when (ogeId) {
                R.id.nav_anasayfa -> HomeFragment()
                R.id.nav_kutuphane -> LibraryFragment()
                R.id.nav_ekle -> AddBookFragment()
                R.id.nav_istatistik -> StatsFragment()
                R.id.nav_profil -> ProfileFragment()
                else -> return
            }
            islem.add(R.id.fragmentKonteyner, yeniFragment, etiket)
            aktifFragment = yeniFragment
        }

        islem.commit()
    }
}
