package com.example.bwbo2monitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothStatus: ImageView
    private lateinit var btnScan: ImageButton
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    companion object {
        private const val TAG = "BluetoothMonitor"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 2
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> updateBluetoothStatus(R.drawable.bluetooth_success)
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> updateBluetoothStatus(R.drawable.bluetooth_fail)
                BluetoothAdapter.ACTION_STATE_CHANGED -> handleBluetoothStateChange(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBluetooth()
        registerBluetoothReceiver()
    }

    private fun initViews() {
        bluetoothStatus = findViewById(R.id.bluetoothStatus)
        btnScan = findViewById(R.id.btnScan)

        btnScan.setOnClickListener {
            checkPermissions {
                startBluetoothDiscovery()
            }
        }
    }

    private fun setupBluetooth() {
        when {
            bluetoothAdapter == null -> {
                showToast("设备不支持蓝牙")
                finish()
            }
            !bluetoothAdapter!!.isEnabled -> {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun checkPermissions(onGranted: () -> Unit) {
        val neededPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!checkBluetoothScanPermission()) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (!checkLocationPermission()) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (neededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                neededPermissions.toTypedArray(),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        } else {
            onGranted()
        }
    }

    private fun startBluetoothDiscovery() {
        // 显式检查权限状态
        if (!hasRequiredPermissions()) {
            showToast("缺少必要权限")
            return
        }

        updateBluetoothStatus(R.drawable.bluetooth_ing)

        bluetoothAdapter?.let { adapter ->
            try {
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                if (adapter.startDiscovery()) {
                    showToast("开始搜索设备...")
                }
            } catch (e: SecurityException) {
                handleSecurityException(e)
            }
        } ?: showToast("蓝牙适配器不可用")
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothScanPermission()
        } else {
            checkLocationPermission()
        }
    }

    private fun checkBluetoothScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun handleSecurityException(e: SecurityException) {
        Log.e(TAG, "安全异常: ${e.message}")
        showToast("权限被拒绝，无法执行操作")
        updateBluetoothStatus(R.drawable.bluetooth_fail)
    }

    private fun updateBluetoothStatus(resId: Int) {
        runOnUiThread {
            bluetoothStatus.setImageResource(resId)
        }
    }

    private fun handleBluetoothStateChange(intent: Intent) {
        when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_OFF -> updateBluetoothStatus(R.drawable.bluetooth_fail)
            BluetoothAdapter.STATE_ON -> updateBluetoothStatus(R.drawable.bluetooth)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode != RESULT_OK) {
                    showToast("必须启用蓝牙")
                    finish()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBluetoothDiscovery()
                } else {
                    showToast("权限被拒绝，无法扫描设备")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}