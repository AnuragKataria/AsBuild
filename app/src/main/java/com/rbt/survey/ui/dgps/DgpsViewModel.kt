package com.rbt.survey.ui.dgps

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rbt.survey.data.local.UserPreferences
import com.rbt.survey.dgps.BaseSettings
import com.rbt.survey.dgps.DgpsManager
import com.rbt.survey.dgps.DgpsUiSettings
import com.rbt.survey.dgps.DeviceInformation
import com.rbt.survey.dgps.DeviceSettings
import com.rbt.survey.dgps.GnssSystemSettings
import com.rbt.survey.dgps.InspectionAccuracySettings
import com.rbt.survey.dgps.NmeaSettings
import com.rbt.survey.dgps.RoverSettings
import com.rbt.survey.dgps.StaticSurveySettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DgpsViewModel(
    private val userPreferences: UserPreferences,
    private val dgpsManager: DgpsManager
) : ViewModel() {
    companion object {
        private const val KEY_ROVER_CUTOFF = "dgps_rover_cutoff"
        private const val KEY_ROVER_DIFF_DELAY = "dgps_rover_diff_delay"
        private const val KEY_ROVER_DISABLE_PPK = "dgps_rover_disable_ppk"
        private const val KEY_ROVER_DATALINK = "dgps_rover_datalink"
        private const val KEY_ROVER_CONNECT_MODE = "dgps_rover_connect_mode"
        private const val KEY_ROVER_PPP_TYPE = "dgps_rover_ppp_type"
        private const val KEY_ROVER_AUTO_CONNECT = "dgps_rover_auto_connect"
        private const val KEY_ROVER_BASE_COORD_ALERT = "dgps_rover_base_coord_alert"

        private const val KEY_BASE_ID = "dgps_base_id"
        private const val KEY_BASE_DIFF_FORMAT = "dgps_base_diff_format"
        private const val KEY_BASE_CUTOFF = "dgps_base_cutoff"
        private const val KEY_BASE_PDOP = "dgps_base_pdop"
        private const val KEY_BASE_STARTUP_MODE = "dgps_base_startup_mode"
        private const val KEY_BASE_PPK_MODE = "dgps_base_ppk_mode"
        private const val KEY_BASE_DATALINK = "dgps_base_datalink"
        private const val KEY_BASE_CHANNEL = "dgps_base_channel"
        private const val KEY_BASE_PROTOCOL = "dgps_base_protocol"
        private const val KEY_BASE_POWER = "dgps_base_power"
        private const val KEY_BASE_NTRIP_ENABLED = "dgps_base_ntrip_enabled"
        private const val KEY_BASE_NTRIP_PORT = "dgps_base_ntrip_port"
        private const val KEY_BASE_ACCESS_POINT = "dgps_base_access_point"

        private const val KEY_STATIC_POINT_NAME = "dgps_static_point_name"
        private const val KEY_STATIC_PDOP = "dgps_static_pdop"
        private const val KEY_STATIC_CUTOFF = "dgps_static_cutoff"
        private const val KEY_STATIC_INTERVAL = "dgps_static_interval"
        private const val KEY_STATIC_OBSERVATION = "dgps_static_observation_time"
        private const val KEY_STATIC_ANTENNA_MEASURING_HEIGHT = "dgps_static_antenna_measuring_height"
        private const val KEY_STATIC_ANTENNA_MEASURING_TYPE = "dgps_static_antenna_measuring_type"
        private const val KEY_STATIC_ANTENNA_HEIGHT = "dgps_static_antenna_height"

        private const val KEY_INSPECTION_ANTENNA_HEIGHT = "dgps_inspection_antenna_height"
        private const val KEY_INSPECTION_AVERAGE_POINTS = "dgps_inspection_average_points"
        private const val KEY_INSPECTION_AVERAGE_INTERVAL = "dgps_inspection_average_interval"
        private const val KEY_INSPECTION_EXCLUSION_RATIO = "dgps_inspection_exclusion_ratio"

        private const val KEY_DEVICE_POSITIONING_OUTPUT = "dgps_device_positioning_output"
        private const val KEY_DEVICE_IMU_OUTPUT = "dgps_device_imu_output"
        private const val KEY_DEVICE_UGYPSOPHILA = "dgps_device_ugypsophila"
        private const val KEY_DEVICE_TIMEZONE = "dgps_device_timezone"

        private const val KEY_GNSS_GPS = "dgps_gnss_gps"
        private const val KEY_GNSS_GLONASS = "dgps_gnss_glonass"
        private const val KEY_GNSS_BEIDOU = "dgps_gnss_beidou"
        private const val KEY_GNSS_GALILEO = "dgps_gnss_galileo"
        private const val KEY_GNSS_SBAS = "dgps_gnss_sbas"
        private const val KEY_GNSS_QZSS = "dgps_gnss_qzss"
        private const val KEY_GNSS_IRNSS = "dgps_gnss_irnss"

        private const val KEY_NMEA_GPGGA = "dgps_nmea_gpgga"
        private const val KEY_NMEA_GPGSA = "dgps_nmea_gpgsa"
        private const val KEY_NMEA_GPGSV = "dgps_nmea_gpgsv"
        private const val KEY_NMEA_GPGST = "dgps_nmea_gpgst"
        private const val KEY_NMEA_GPZDA = "dgps_nmea_gpzda"
        private const val KEY_NMEA_GPRMC = "dgps_nmea_gprmc"
        private const val KEY_NMEA_GPVTG = "dgps_nmea_gpvtg"

        private const val KEY_DEVICE_SN = "dgps_info_device_sn"
        private const val KEY_DEVICE_FIRMWARE = "dgps_info_device_firmware"
        private const val KEY_DEVICE_SENSOR_VERSION = "dgps_info_sensor_version"
        private const val KEY_DEVICE_GNSS_FIRMWARE = "dgps_info_gnss_firmware"
        private const val KEY_DEVICE_CURRENT_DATALINK = "dgps_info_current_datalink"
        private const val KEY_DEVICE_BATTERY = "dgps_info_battery"
        private const val KEY_DEVICE_EXPIRATION = "dgps_info_expiration"
        private const val KEY_DEVICE_ANTENNA_MODEL = "dgps_info_antenna_model"
        private const val KEY_DEVICE_ANTENNA_RADIUS = "dgps_info_antenna_radius"
        private const val KEY_DEVICE_ANTENNA_HEIGHT = "dgps_info_antenna_height"
        private const val KEY_DEVICE_ANTENNA_HL1 = "dgps_info_antenna_hl1"
        private const val KEY_DEVICE_ANTENNA_HL2 = "dgps_info_antenna_hl2"
    }

    private val _bluetoothDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bluetoothDevices = _bluetoothDevices.asStateFlow()

    val dgpsStatus = dgpsManager.status
    val dgpsLocation = dgpsManager.location
    val isNtripConnected = dgpsManager.isNtripConnected

    private val _uiSettings = MutableStateFlow(DgpsUiSettings())
    val uiSettings = _uiSettings.asStateFlow()

    private val _mountpoints = MutableStateFlow<List<String>>(emptyList())
    val mountpoints = _mountpoints.asStateFlow()

    private val _isFetchingMountpoints = MutableStateFlow(false)
    val isFetchingMountpoints = _isFetchingMountpoints.asStateFlow()

    val savedAddress = userPreferences.dgpsDeviceAddress
    val savedHost = userPreferences.corsHost
    val savedPort = userPreferences.corsPort
    val savedMountpoint = userPreferences.corsMountpoint
    val savedUser = userPreferences.corsUser
    val savedPass = userPreferences.corsPass
    val useDgps = userPreferences.useDgps
    val useCors = userPreferences.useCors

    init {
        viewModelScope.launch {
            loadUiSettings()
        }
    }

    @SuppressLint("MissingPermission")
    fun scanDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            _bluetoothDevices.value = adapter.bondedDevices.toList()
        }
    }

    fun selectBluetoothDevice(address: String) {
        viewModelScope.launch {
            userPreferences.saveDgpsSettings(
                address = address,
                host = null,
                port = null,
                mountpoint = null,
                user = null,
                pass = null,
                useDgps = true,
                useCors = true // keep as is
            )
            _uiSettings.update { it.copy(selectedBluetoothAddress = address) }
            dgpsManager.connectBluetooth(address)
        }
    }

    fun disconnectBluetooth() {
        dgpsManager.disconnectBluetooth()
    }

    fun connectCors(
        host: String,
        port: String,
        mountpoint: String,
        user: String,
        pass: String
    ) {
        viewModelScope.launch {
            try {
                val p = port.toIntOrNull() ?: return@launch
                dgpsManager.connectNtrip(host, p, mountpoint, user, pass)
                saveRoverSettings(
                    _uiSettings.value.rover.copy(
                        host = host,
                        port = port,
                        mountpoint = mountpoint,
                        user = user,
                        password = pass
                    )
                )
            } catch (e: Exception) {
                if (e.message?.contains("401") == true) {
                    _errorFlow.emit("CORS Authentication Failed (401). Check username/password.")
                } else {
                    _errorFlow.emit("CORS Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun disconnectCors() {
        dgpsManager.disconnectNtrip()
    }

    fun startRover(settings: RoverSettings = _uiSettings.value.rover) {
        viewModelScope.launch {
            saveRoverSettings(settings)
            val address = _uiSettings.value.selectedBluetoothAddress
            if (address.isBlank()) {
                _errorFlow.emit("Select a DGPS Bluetooth device first.")
                return@launch
            }
            dgpsManager.connectBluetooth(address)
            if (settings.connectingMode.equals("NTRIP", ignoreCase = true)) {
                val p = settings.port.toIntOrNull()
                if (p == null) {
                    _errorFlow.emit("Enter a valid NTRIP server port.")
                    return@launch
                }
                if (settings.mountpoint.isBlank()) {
                    _errorFlow.emit("Select or enter an NTRIP mountpoint.")
                    return@launch
                }
                dgpsManager.connectNtrip(
                    settings.host,
                    p,
                    settings.mountpoint,
                    settings.user,
                    settings.password
                )
            } else {
                dgpsManager.disconnectNtrip()
            }
        }
    }

    fun stopRover() {
        dgpsManager.disconnect()
    }

    fun startBase(settings: BaseSettings = _uiSettings.value.base) {
        viewModelScope.launch {
            saveBaseSettings(settings)
            val address = _uiSettings.value.selectedBluetoothAddress
            if (address.isBlank()) {
                _errorFlow.emit("Select a DGPS Bluetooth device before starting base mode.")
                return@launch
            }
            dgpsManager.connectBluetooth(address)
            _errorFlow.emit("Base profile saved. Receiver command support for native base-mode control can be added next.")
        }
    }

    fun saveRoverSettings(settings: RoverSettings) {
        viewModelScope.launch {
            persistRoverSettings(settings)
        }
    }

    fun saveBaseSettings(settings: BaseSettings) {
        viewModelScope.launch {
            persistBaseSettings(settings)
        }
    }

    fun saveStaticSurveySettings(settings: StaticSurveySettings) {
        viewModelScope.launch {
            persistStaticSurveySettings(settings)
        }
    }

    fun saveInspectionAccuracySettings(settings: InspectionAccuracySettings) {
        viewModelScope.launch {
            persistInspectionAccuracySettings(settings)
        }
    }

    fun saveDeviceSettings(settings: DeviceSettings) {
        viewModelScope.launch {
            _uiSettings.update { it.copy(deviceSettings = settings) }
            userPreferences.saveStringSetting(KEY_DEVICE_POSITIONING_OUTPUT, settings.positioningOutputFrequency)
            userPreferences.saveStringSetting(KEY_DEVICE_IMU_OUTPUT, settings.imuOutputFrequency)
            userPreferences.saveBooleanSetting(KEY_DEVICE_UGYPSOPHILA, settings.uGypsophilaTechnology)
            userPreferences.saveStringSetting(KEY_DEVICE_TIMEZONE, settings.timeZone)
        }
    }

    fun saveGnssSystems(settings: GnssSystemSettings) {
        viewModelScope.launch {
            _uiSettings.update { it.copy(gnssSystems = settings) }
            userPreferences.saveBooleanSetting(KEY_GNSS_GPS, settings.gps)
            userPreferences.saveBooleanSetting(KEY_GNSS_GLONASS, settings.glonass)
            userPreferences.saveBooleanSetting(KEY_GNSS_BEIDOU, settings.beidou)
            userPreferences.saveBooleanSetting(KEY_GNSS_GALILEO, settings.galileo)
            userPreferences.saveBooleanSetting(KEY_GNSS_SBAS, settings.sbas)
            userPreferences.saveBooleanSetting(KEY_GNSS_QZSS, settings.qzss)
            userPreferences.saveBooleanSetting(KEY_GNSS_IRNSS, settings.irnss)
        }
    }

    fun saveNmeaSettings(settings: NmeaSettings) {
        viewModelScope.launch {
            _uiSettings.update { it.copy(nmea = settings) }
            userPreferences.saveStringSetting(KEY_NMEA_GPGGA, settings.gpgga)
            userPreferences.saveStringSetting(KEY_NMEA_GPGSA, settings.gpgsa)
            userPreferences.saveStringSetting(KEY_NMEA_GPGSV, settings.gpgsv)
            userPreferences.saveStringSetting(KEY_NMEA_GPGST, settings.gpgst)
            userPreferences.saveStringSetting(KEY_NMEA_GPZDA, settings.gpzda)
            userPreferences.saveStringSetting(KEY_NMEA_GPRMC, settings.gprmc)
            userPreferences.saveStringSetting(KEY_NMEA_GPVTG, settings.gpvtg)
        }
    }

    fun disconnect() {
        dgpsManager.disconnect()
    }

    private val _errorFlow = MutableSharedFlow<String>()
    val errorFlow = _errorFlow.asSharedFlow()

    fun fetchMountpoints(host: String, port: String) {
        val p = port.toIntOrNull() ?: return
        viewModelScope.launch {
            _isFetchingMountpoints.value = true
            try {
                val results = dgpsManager.fetchMountpoints(host, p)
                if (results.isEmpty()) {
                    _errorFlow.emit("No mountpoints found or host unreachable")
                } else {
                    _mountpoints.value = results
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorFlow.emit("Connection error: ${e.localizedMessage}")
            } finally {
                _isFetchingMountpoints.value = false
            }
        }
    }

    private suspend fun loadUiSettings() {
        val selectedBluetoothAddress = savedAddress.first().orEmpty()
        val rover = RoverSettings(
            cutOffAngle = userPreferences.getStringSetting(KEY_ROVER_CUTOFF, "10"),
            diffDelay = userPreferences.getStringSetting(KEY_ROVER_DIFF_DELAY, "10"),
            disablePpk = userPreferences.getBooleanSetting(KEY_ROVER_DISABLE_PPK, true),
            datalink = userPreferences.getStringSetting(KEY_ROVER_DATALINK, "Phone Internet"),
            connectingMode = userPreferences.getStringSetting(KEY_ROVER_CONNECT_MODE, "NTRIP"),
            host = savedHost.first() ?: "103.205.244.106",
            port = savedPort.first() ?: "2010",
            user = savedUser.first() ?: "rbtonline021",
            password = savedPass.first() ?: "cors@2022",
            pppType = userPreferences.getStringSetting(KEY_ROVER_PPP_TYPE, "HAS"),
            mountpoint = savedMountpoint.first().orEmpty(),
            autoConnectToNetwork = userPreferences.getBooleanSetting(KEY_ROVER_AUTO_CONNECT, true),
            baseCoordinatesChangeAlert = userPreferences.getBooleanSetting(KEY_ROVER_BASE_COORD_ALERT, false)
        )
        val base = BaseSettings(
            baseId = userPreferences.getStringSetting(KEY_BASE_ID, "315"),
            diffFormat = userPreferences.getStringSetting(KEY_BASE_DIFF_FORMAT, "RTCM32"),
            cutOffAngle = userPreferences.getStringSetting(KEY_BASE_CUTOFF, "30"),
            pdop = userPreferences.getStringSetting(KEY_BASE_PDOP, "3"),
            baseStartupMode = userPreferences.getStringSetting(KEY_BASE_STARTUP_MODE, "Single Point"),
            ppkMode = userPreferences.getStringSetting(KEY_BASE_PPK_MODE, "Disable"),
            datalink = userPreferences.getStringSetting(KEY_BASE_DATALINK, "Internal Radio"),
            channel = userPreferences.getStringSetting(KEY_BASE_CHANNEL, "[1]450.125"),
            protocol = userPreferences.getStringSetting(KEY_BASE_PROTOCOL, "TrimTalk 450S"),
            power = userPreferences.getStringSetting(KEY_BASE_POWER, "Low"),
            ntripCasterEnabled = userPreferences.getBooleanSetting(KEY_BASE_NTRIP_ENABLED, false),
            ntripPort = userPreferences.getStringSetting(KEY_BASE_NTRIP_PORT, "8000"),
            baseAccessPoint = userPreferences.getStringSetting(KEY_BASE_ACCESS_POINT, "T10R2A1160000001")
        )
        val staticSurvey = StaticSurveySettings(
            pointName = userPreferences.getStringSetting(KEY_STATIC_POINT_NAME, "TX001"),
            pdop = userPreferences.getStringSetting(KEY_STATIC_PDOP, "3.000"),
            cutOffAngle = userPreferences.getStringSetting(KEY_STATIC_CUTOFF, "10"),
            interval = userPreferences.getStringSetting(KEY_STATIC_INTERVAL, "1HZ"),
            observationTime = userPreferences.getStringSetting(KEY_STATIC_OBSERVATION, "Unlimited"),
            antennaMeasuringHeight = userPreferences.getStringSetting(KEY_STATIC_ANTENNA_MEASURING_HEIGHT, "1.892"),
            antennaMeasuringType = userPreferences.getStringSetting(KEY_STATIC_ANTENNA_MEASURING_TYPE, "Vertical Height to Device Bottom"),
            antennaHeight = userPreferences.getStringSetting(KEY_STATIC_ANTENNA_HEIGHT, "1.984")
        )
        val inspectionAccuracy = InspectionAccuracySettings(
            antennaHeight = userPreferences.getStringSetting(KEY_INSPECTION_ANTENNA_HEIGHT, "1.8+0.092m"),
            averagePoints = userPreferences.getStringSetting(KEY_INSPECTION_AVERAGE_POINTS, "60"),
            averageInterval = userPreferences.getStringSetting(KEY_INSPECTION_AVERAGE_INTERVAL, "1"),
            exclusionAbnormalRatio = userPreferences.getStringSetting(KEY_INSPECTION_EXCLUSION_RATIO, "0")
        )
        val deviceSettings = DeviceSettings(
            positioningOutputFrequency = userPreferences.getStringSetting(KEY_DEVICE_POSITIONING_OUTPUT, "1HZ"),
            imuOutputFrequency = userPreferences.getStringSetting(KEY_DEVICE_IMU_OUTPUT, "5HZ"),
            uGypsophilaTechnology = userPreferences.getBooleanSetting(KEY_DEVICE_UGYPSOPHILA, true),
            timeZone = userPreferences.getStringSetting(KEY_DEVICE_TIMEZONE, "UTC+08:00")
        )
        val gnssSystems = GnssSystemSettings(
            gps = userPreferences.getBooleanSetting(KEY_GNSS_GPS, true),
            glonass = userPreferences.getBooleanSetting(KEY_GNSS_GLONASS, true),
            beidou = userPreferences.getBooleanSetting(KEY_GNSS_BEIDOU, true),
            galileo = userPreferences.getBooleanSetting(KEY_GNSS_GALILEO, true),
            sbas = userPreferences.getBooleanSetting(KEY_GNSS_SBAS, true),
            qzss = userPreferences.getBooleanSetting(KEY_GNSS_QZSS, true),
            irnss = userPreferences.getBooleanSetting(KEY_GNSS_IRNSS, true)
        )
        val nmea = NmeaSettings(
            gpgga = userPreferences.getStringSetting(KEY_NMEA_GPGGA, "1HZ"),
            gpgsa = userPreferences.getStringSetting(KEY_NMEA_GPGSA, "1S"),
            gpgsv = userPreferences.getStringSetting(KEY_NMEA_GPGSV, "5S"),
            gpgst = userPreferences.getStringSetting(KEY_NMEA_GPGST, "1S"),
            gpzda = userPreferences.getStringSetting(KEY_NMEA_GPZDA, "1S"),
            gprmc = userPreferences.getStringSetting(KEY_NMEA_GPRMC, "OFF"),
            gpvtg = userPreferences.getStringSetting(KEY_NMEA_GPVTG, "OFF")
        )
        val deviceInfo = DeviceInformation(
            deviceSn = userPreferences.getStringSetting(KEY_DEVICE_SN, "T28R59116002315"),
            deviceFirmware = userPreferences.getStringSetting(KEY_DEVICE_FIRMWARE, "V2_0_54-A-20250915"),
            sensorVersion = userPreferences.getStringSetting(KEY_DEVICE_SENSOR_VERSION, "60240366"),
            gnssFirmware = userPreferences.getStringSetting(KEY_DEVICE_GNSS_FIRMWARE, ""),
            currentDatalink = userPreferences.getStringSetting(KEY_DEVICE_CURRENT_DATALINK, rover.datalink),
            batteryPower = userPreferences.getStringSetting(KEY_DEVICE_BATTERY, "65%"),
            expirationDate = userPreferences.getStringSetting(KEY_DEVICE_EXPIRATION, "20260105"),
            antennaModel = userPreferences.getStringSetting(KEY_DEVICE_ANTENNA_MODEL, "TX-CSX327A"),
            antennaRadiusMm = userPreferences.getStringSetting(KEY_DEVICE_ANTENNA_RADIUS, "60.15"),
            antennaHeightMm = userPreferences.getStringSetting(KEY_DEVICE_ANTENNA_HEIGHT, "58"),
            antennaHl1Mm = userPreferences.getStringSetting(KEY_DEVICE_ANTENNA_HL1, "33.9"),
            antennaHl2Mm = userPreferences.getStringSetting(KEY_DEVICE_ANTENNA_HL2, "25.9")
        )

        _uiSettings.value = DgpsUiSettings(
            selectedBluetoothAddress = selectedBluetoothAddress,
            rover = rover,
            base = base,
            staticSurvey = staticSurvey,
            inspectionAccuracy = inspectionAccuracy,
            deviceSettings = deviceSettings,
            gnssSystems = gnssSystems,
            nmea = nmea,
            deviceInfo = deviceInfo
        )
    }

    private suspend fun persistRoverSettings(settings: RoverSettings) {
        _uiSettings.update {
            it.copy(
                rover = settings,
                deviceInfo = it.deviceInfo.copy(currentDatalink = settings.datalink)
            )
        }
        userPreferences.saveStringSetting(KEY_ROVER_CUTOFF, settings.cutOffAngle)
        userPreferences.saveStringSetting(KEY_ROVER_DIFF_DELAY, settings.diffDelay)
        userPreferences.saveBooleanSetting(KEY_ROVER_DISABLE_PPK, settings.disablePpk)
        userPreferences.saveStringSetting(KEY_ROVER_DATALINK, settings.datalink)
        userPreferences.saveStringSetting(KEY_ROVER_CONNECT_MODE, settings.connectingMode)
        userPreferences.saveStringSetting(KEY_ROVER_PPP_TYPE, settings.pppType)
        userPreferences.saveBooleanSetting(KEY_ROVER_AUTO_CONNECT, settings.autoConnectToNetwork)
        userPreferences.saveBooleanSetting(KEY_ROVER_BASE_COORD_ALERT, settings.baseCoordinatesChangeAlert)
        userPreferences.saveStringSetting(KEY_DEVICE_CURRENT_DATALINK, settings.datalink)
        userPreferences.saveDgpsSettings(
            address = _uiSettings.value.selectedBluetoothAddress.takeIf { it.isNotBlank() },
            host = settings.host,
            port = settings.port,
            mountpoint = settings.mountpoint,
            user = settings.user,
            pass = settings.password,
            useDgps = _uiSettings.value.selectedBluetoothAddress.isNotBlank(),
            useCors = settings.connectingMode.equals("NTRIP", ignoreCase = true)
        )
    }

    private suspend fun persistBaseSettings(settings: BaseSettings) {
        _uiSettings.update { it.copy(base = settings) }
        userPreferences.saveStringSetting(KEY_BASE_ID, settings.baseId)
        userPreferences.saveStringSetting(KEY_BASE_DIFF_FORMAT, settings.diffFormat)
        userPreferences.saveStringSetting(KEY_BASE_CUTOFF, settings.cutOffAngle)
        userPreferences.saveStringSetting(KEY_BASE_PDOP, settings.pdop)
        userPreferences.saveStringSetting(KEY_BASE_STARTUP_MODE, settings.baseStartupMode)
        userPreferences.saveStringSetting(KEY_BASE_PPK_MODE, settings.ppkMode)
        userPreferences.saveStringSetting(KEY_BASE_DATALINK, settings.datalink)
        userPreferences.saveStringSetting(KEY_BASE_CHANNEL, settings.channel)
        userPreferences.saveStringSetting(KEY_BASE_PROTOCOL, settings.protocol)
        userPreferences.saveStringSetting(KEY_BASE_POWER, settings.power)
        userPreferences.saveBooleanSetting(KEY_BASE_NTRIP_ENABLED, settings.ntripCasterEnabled)
        userPreferences.saveStringSetting(KEY_BASE_NTRIP_PORT, settings.ntripPort)
        userPreferences.saveStringSetting(KEY_BASE_ACCESS_POINT, settings.baseAccessPoint)
    }

    private suspend fun persistStaticSurveySettings(settings: StaticSurveySettings) {
        _uiSettings.update { it.copy(staticSurvey = settings) }
        userPreferences.saveStringSetting(KEY_STATIC_POINT_NAME, settings.pointName)
        userPreferences.saveStringSetting(KEY_STATIC_PDOP, settings.pdop)
        userPreferences.saveStringSetting(KEY_STATIC_CUTOFF, settings.cutOffAngle)
        userPreferences.saveStringSetting(KEY_STATIC_INTERVAL, settings.interval)
        userPreferences.saveStringSetting(KEY_STATIC_OBSERVATION, settings.observationTime)
        userPreferences.saveStringSetting(KEY_STATIC_ANTENNA_MEASURING_HEIGHT, settings.antennaMeasuringHeight)
        userPreferences.saveStringSetting(KEY_STATIC_ANTENNA_MEASURING_TYPE, settings.antennaMeasuringType)
        userPreferences.saveStringSetting(KEY_STATIC_ANTENNA_HEIGHT, settings.antennaHeight)
    }

    private suspend fun persistInspectionAccuracySettings(settings: InspectionAccuracySettings) {
        _uiSettings.update { it.copy(inspectionAccuracy = settings) }
        userPreferences.saveStringSetting(KEY_INSPECTION_ANTENNA_HEIGHT, settings.antennaHeight)
        userPreferences.saveStringSetting(KEY_INSPECTION_AVERAGE_POINTS, settings.averagePoints)
        userPreferences.saveStringSetting(KEY_INSPECTION_AVERAGE_INTERVAL, settings.averageInterval)
        userPreferences.saveStringSetting(KEY_INSPECTION_EXCLUSION_RATIO, settings.exclusionAbnormalRatio)
    }
}

class DgpsViewModelFactory(
    private val userPreferences: UserPreferences,
    private val dgpsManager: DgpsManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DgpsViewModel(userPreferences, dgpsManager) as T
    }
}
