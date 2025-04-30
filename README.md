# BWBO2Monitor - 蓝牙监控应用

[Android Version](https://developer.android.com/about/versions/lollipop)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)



Android 蓝牙设备监控应用，支持设备搜索、连接状态管理和数据通信。适配 Android 5.0 (API 21) 及以上版本。

## 功能特性

- ✅ 蓝牙设备扫描与列表展示
- 🔄 四种连接状态可视化：
  - 默认状态 (`bluetooth`)
  - 连接中 (`bluetooth_ing`)
  - 连接成功 (`bluetooth_success`)
  - 连接失败 (`bluetooth_fail`)
- 📶 实时连接状态文字提示
- 🔍 支持经典蓝牙设备发现
- 🔗 自动重连机制（可选）

## 技术栈

- **语言**：Kotlin/Java
- **蓝牙API**：`android.bluetooth`
- **权限管理**：Android Runtime Permissions
- **最低支持**：Android 5.0 (API 21)
- **架构**：MVVM (推荐扩展)

## 快速开始

### 1. 环境要求
- Android Studio Arctic Fox (2020.3.1) 或更高版本
- Gradle 7.0+
- 支持蓝牙4.0+的Android设备

### 2. 项目配置
在 `app/build.gradle` 中添加蓝牙权限：
```groovy
android {
    defaultConfig {
        minSdkVersion 21
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.7.0'
}
```

### 3. 权限声明
在 `AndroidManifest.xml` 中添加：
```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<!-- Android 12+ 需要额外声明 -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" 
                 android:maxSdkVersion="31"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
                 android:maxSdkVersion="30"/>
```

## 关键代码说明

### 蓝牙状态管理
```kotlin
private fun updateBluetoothStatus(resId: Int, text: String) {
    runOnUiThread {
        binding.bluetoothStatus.setImageResource(resId)
        binding.deviceStatus.text = text
    }
}
```

### 设备扫描
```kotlin
fun startBluetoothDiscovery() {
    if (!checkPermissions()) return
    
    bluetoothAdapter?.let { adapter ->
        updateBluetoothStatus(R.drawable.bluetooth_ing, "正在搜索...")
        adapter.startDiscovery()
    }
}
```

## 项目结构
```
app/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com.example.bwbo2monitor/
│   │   │       ├── MainActivity.kt    # 主逻辑
│   │   │       └── BluetoothManager.kt # 蓝牙封装类
│   │   ├── res/
│   │   │   ├── drawable/              # 状态图标
│   │   │   ├── layout/                # UI布局
│   │   │   └── values/                # 字符串资源
│   │   └── AndroidManifest.xml
```

## 扩展建议

1. **添加数据图表**：集成 [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) 显示实时数据
2. **支持低功耗蓝牙**：扩展 `BluetoothLeScanner` 功能
3. **增加设备配对管理**：实现自动配对功能

## 常见问题

❓ **找不到蓝牙设备**  
- 确认设备蓝牙已开启
- 检查是否授予定位权限（Android 6.0+需要）

❓ **连接不稳定**  
- 参考 `BluetoothSocket` 的错误处理逻辑
- 实现心跳包机制保持连接

## 贡献指南
欢迎提交 Pull Request。重大更改请先开 Issue 讨论。
