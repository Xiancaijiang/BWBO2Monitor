package com.example.bwbo2monitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID

class MainActivity : AppCompatActivity() {

    // UI组件
    private lateinit var bluetoothStatus: ImageView
    private lateinit var deviceStatus: TextView
    private lateinit var btnScan: ImageButton
    private lateinit var deviceListView: ListView

    // 蓝牙相关
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }
    private var connectedDevice: BluetoothDevice? = null
    private var connectThread: ConnectThread? = null
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceListAdapter: ArrayAdapter<String>

    // 常量
    companion object {
        private const val TAG = "BluetoothMonitor"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 2
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID
        private const val CONNECTION_TIMEOUT = 10000L // 10秒连接超时
    }

    // 广播接收器：监听蓝牙状态变化和设备发现
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // 兼容不同 API 版本的设备获取方式
                    val device = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        }

                        else -> {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    }
                    device?.let { handleDiscoveredDevice(it) }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> updateStatus(R.drawable.bluetooth_success, "已连接")
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> updateStatus(R.drawable.bluetooth_fail, "连接断开")
                BluetoothAdapter.ACTION_STATE_CHANGED -> handleBluetoothStateChange(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> updateStatus(R.drawable.bluetooth, "搜索完成")
            }
        }
    }
    private fun handleDiscoveredDevice(device: BluetoothDevice) {
        if (!discoveredDevices.any { it.address == device.address }) {
            discoveredDevices.add(device)
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
            deviceListAdapter.add("${device.name ?: "未知设备"}\n${device.address}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupBluetooth()
        registerBluetoothReceiver()
        setupDeviceList()
    }

    // 初始化UI组件
    private fun initViews() {
        bluetoothStatus = findViewById(R.id.bluetoothStatus)
        deviceStatus = findViewById(R.id.deviceStatus)
        btnScan = findViewById(R.id.btnScan)
        deviceListView = findViewById(R.id.deviceList)

        btnScan.setOnClickListener {
            checkPermissions {
                startBluetoothDiscovery()
            }
        }
    }

    // 设置设备列表
    private fun setupDeviceList() {
        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceListView.adapter = deviceListAdapter
        deviceListView.setOnItemClickListener { _, _, position, _ ->
            if (!hasConnectionPermission()) { // 点击时再次检查权限
                requestBluetoothConnectPermission()
                return@setOnItemClickListener
            }

            stopDiscovery()
            val device = discoveredDevices.getOrNull(position) ?: run {
                updateStatus(R.drawable.bluetooth_fail, "无效设备")
                return@setOnItemClickListener
            }
            connectToDevice(device)
        }
    }

    // 连接指定设备
    private fun connectToDevice(device: BluetoothDevice) {
        updateStatus(R.drawable.bluetooth_ing, "正在连接...")
        connectThread?.cancel()
        connectThread = ConnectThread(device).apply { start() }

        // 设置连接超时
        Handler(Looper.getMainLooper()).postDelayed({
            if (connectedDevice == null) {
                updateStatus(R.drawable.bluetooth_fail, "连接超时")
                connectThread?.cancel()
            }
        }, CONNECTION_TIMEOUT)
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

    // 启用蓝牙
    private fun enableBluetooth() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_ENABLE_BT)
            return
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
    }

    // 注册广播接收器
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    // 启动设备发现
    private fun startBluetoothDiscovery() {
        if (!hasRequiredPermissions()) {
            showToast("缺少必要权限")
            return
        }

        updateStatus(R.drawable.bluetooth_ing, "正在搜索...")
        discoveredDevices.clear()
        deviceListAdapter.clear()

        bluetoothAdapter?.let { adapter ->
            try {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                if (adapter.startDiscovery()) {
                    showToast("开始搜索设备...")
                }
            } catch (e: SecurityException) {
                handleSecurityException(e)
            }
        } ?: showToast("蓝牙适配器不可用")
    }

    // 停止设备发现
    private fun stopDiscovery() {
        try {
            bluetoothAdapter?.takeIf { it.isDiscovering }?.cancelDiscovery()
        } catch (e: SecurityException) {
            Log.e(TAG, "停止搜索失败: ${e.message}")
        }
    }

    // 权限检查
    private fun checkPermissions(onGranted: () -> Unit) {
        val neededPermissions = mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!checkBluetoothScanPermission()) add(Manifest.permission.BLUETOOTH_SCAN)
                if (!checkBluetoothConnectPermission()) add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                if (!checkLocationPermission()) add(Manifest.permission.ACCESS_FINE_LOCATION)
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

    // 更新状态（图标+文字）
    private fun updateStatus(resId: Int, text: String) {
        runOnUiThread {
            bluetoothStatus.setImageResource(resId)
            deviceStatus.text = text
        }
    }

    // 连接线程
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() { // 添加成员变量
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                // 需要 BLUETOOTH_CONNECT 权限（Android 12+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!checkBluetoothConnectPermission()) {
                        runOnUiThread {
                            updateStatus(R.drawable.bluetooth_fail, "缺少连接权限")
                            requestBluetoothConnectPermission()
                        }
                        return@lazy null
                    }
                }
                device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "创建Socket失败: ${e.message}")
                null
            } catch (e: SecurityException) { // 处理权限异常
                Log.e(TAG, "安全异常: ${e.message}")
                null
            }
        }

        override fun run() {
            try {
                mmSocket?.let { socket ->
                    // 连接前检查权限（兼容 Android 12 以下）
                    if (!hasConnectionPermission()) {
                        runOnUiThread {
                            updateStatus(R.drawable.bluetooth_fail, "权限不足")
                            requestBluetoothConnectPermission()
                        }
                        return
                    }

                    socket.connect()
                    connectedDevice = device
                    runOnUiThread {
                        updateStatus(R.drawable.bluetooth_success, "已连接: ${device.name ?: "未知设备"}")
                    }
                } ?: run {
                    runOnUiThread {
                        updateStatus(R.drawable.bluetooth_fail, "连接失败")
                    }
                }
            } catch (e: IOException) {
                // ...原有代码...
            } catch (e: SecurityException) { // 显式捕获权限异常
                Log.e(TAG, "连接权限被拒绝: ${e.message}")
                runOnUiThread {
                    updateStatus(R.drawable.bluetooth_fail, "权限被拒绝")
                }
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "关闭Socket失败: ${e.message}")
            }
        }
    }

    // 检查是否具备所有必要权限
    private fun hasConnectionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkBluetoothConnectPermission()
        } else {
            // 旧版本只需检查位置权限
            checkLocationPermission()
        }
    }

    // 单独请求连接权限的方法
    private fun requestBluetoothConnectPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_PERMISSIONS
            )
        }
    }

    // 在 MainActivity 类中添加以下三个权限检查方法
    private fun checkBluetoothScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkBluetoothConnectPermission(): Boolean { // 新增的权限检查
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // 低版本不需要此权限
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 修改 hasRequiredPermissions 方法
    private fun hasRequiredPermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                checkBluetoothScanPermission() && checkBluetoothConnectPermission()
            }
            else -> {
                checkLocationPermission()
            }
        }
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

    // 修改 onRequestPermissionsResult
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_BLUETOOTH_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // 权限获取后自动重试连接
                    connectedDevice?.let { connectToDevice(it) }
                } else {
                    updateStatus(R.drawable.bluetooth_fail, "权限被拒绝")
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