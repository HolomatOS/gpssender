package com.holomatos.gpssender

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.holomatos.gpssender.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val STOP_PIN = "0707"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var tracking = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                beginTracking()
            } else {
                Toast.makeText(this, "Location permission is required to share your position", Toast.LENGTH_LONG).show()
            }
        }

    private val deviceAdminLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Whether granted or not, we continue - admin is an extra layer, not a hard requirement
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AppDeviceAdminReceiver::class.java)

        val prefs = getSharedPreferences(LocationForegroundService.PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(LocationForegroundService.KEY_PAIRING_CODE, null)
        if (!savedCode.isNullOrEmpty()) {
            binding.pairingCodeInput.setText(savedCode)
        }

        promptDeviceAdminIfNeeded()

        binding.startStopButton.setOnClickListener {
            if (!tracking) {
                val code = binding.pairingCodeInput.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(this, "Enter a pairing code first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                checkPermissionAndStart(code)
            } else {
                promptStopPin()
            }
        }
    }

    private fun promptStopPin() {
        val pinInput = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter PIN"
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Stop sharing")
            .setMessage("Enter PIN to stop location sharing")
            .setView(pinInput)
            .setPositiveButton("Confirm") { _, _ ->
                if (pinInput.text.toString() == STOP_PIN) {
                    stopTracking()
                } else {
                    Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun promptDeviceAdminIfNeeded() {
        val isActive = devicePolicyManager.isAdminActive(adminComponent)
        if (!isActive) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Activating device admin prevents this app from being uninstalled without first deactivating it here. Recommended for anti-theft tracking."
                )
            }
            deviceAdminLauncher.launch(intent)
        }
    }

    private fun checkPermissionAndStart(code: String) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            beginTracking()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun beginTracking() {
        val code = binding.pairingCodeInput.text.toString().trim()
        if (code.isEmpty()) return

        getSharedPreferences(LocationForegroundService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LocationForegroundService.KEY_PAIRING_CODE, code)
            .apply()

        val serviceIntent = Intent(this, LocationForegroundService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        tracking = true
        binding.startStopButton.text = "STOP SHARING"
        binding.statusText.text = "SHARING TO CODE: $code (background service active)"
    }

    private fun stopTracking() {
        stopService(Intent(this, LocationForegroundService::class.java))
        getSharedPreferences(LocationForegroundService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(LocationForegroundService.KEY_PAIRING_CODE)
            .apply()
        tracking = false
        binding.startStopButton.text = "START SHARING"
        binding.statusText.text = "NOT SHARING"
    }
}
