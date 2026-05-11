package com.rbt.survey.dgps

data class DgpsLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val hrms: Float = 0f,
    val vrms: Float = 0f,
    val fixQuality: Int, // 0=Invalid, 1=GPS fix, 2=DGPS fix, 4=RTK fixed, 5=RTK float
    val satellites: Int,
    val satellitesList: List<SatelliteInfo> = emptyList(),
    val speedMps: Float = 0f,
    val headingDegrees: Float = 0f,
    val pdop: Float? = null,
    val hdop: Float? = null,
    val vdop: Float? = null,
    val ageSeconds: Float? = null,
    val utcDateTime: String? = null,
    val baseStationId: String? = null,
    val distanceToReferenceMeters: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SatelliteInfo(
    val prn: Int,
    val elevation: Int,
    val azimuth: Int,
    val snr: Int,
    val constellation: SatelliteConstellation = SatelliteConstellation.UNKNOWN,
    val usedInFix: Boolean = false
)

enum class SatelliteConstellation(val shortLabel: String, val displayName: String) {
    BEIDOU("BD", "BEIDOU"),
    GPS("GPS", "GPS"),
    GLONASS("GLN", "GLONASS"),
    GALILEO("GAL", "GALILEO"),
    SBAS("SBAS", "SBAS"),
    QZSS("QZSS", "QZSS"),
    IRNSS("IRNSS", "IRNSS"),
    UNKNOWN("UNK", "UNKNOWN")
}

data class RoverSettings(
    val cutOffAngle: String = "10",
    val diffDelay: String = "10",
    val disablePpk: Boolean = true,
    val datalink: String = "Phone Internet",
    val connectingMode: String = "NTRIP",
    val host: String = "103.205.244.106",
    val port: String = "2010",
    val user: String = "rbtonline021",
    val password: String = "cors@2022",
    val pppType: String = "HAS",
    val mountpoint: String = "",
    val autoConnectToNetwork: Boolean = true,
    val baseCoordinatesChangeAlert: Boolean = false
)

data class BaseSettings(
    val baseId: String = "315",
    val diffFormat: String = "RTCM32",
    val cutOffAngle: String = "30",
    val pdop: String = "3",
    val baseStartupMode: String = "Single Point",
    val ppkMode: String = "Disable",
    val datalink: String = "Internal Radio",
    val channel: String = "[1]450.125",
    val protocol: String = "TrimTalk 450S",
    val power: String = "Low",
    val ntripCasterEnabled: Boolean = false,
    val ntripPort: String = "8000",
    val baseAccessPoint: String = "T10R2A1160000001"
)

data class StaticSurveySettings(
    val pointName: String = "TX001",
    val pdop: String = "3.000",
    val cutOffAngle: String = "10",
    val interval: String = "1HZ",
    val observationTime: String = "Unlimited",
    val antennaMeasuringHeight: String = "1.892",
    val antennaMeasuringType: String = "Vertical Height to Device Bottom",
    val antennaHeight: String = "1.984"
)

data class InspectionAccuracySettings(
    val antennaHeight: String = "1.8+0.092m",
    val averagePoints: String = "60",
    val averageInterval: String = "1",
    val exclusionAbnormalRatio: String = "0"
)

data class DeviceSettings(
    val positioningOutputFrequency: String = "1HZ",
    val imuOutputFrequency: String = "5HZ",
    val uGypsophilaTechnology: Boolean = true,
    val timeZone: String = "UTC+08:00"
)

data class GnssSystemSettings(
    val gps: Boolean = true,
    val glonass: Boolean = true,
    val beidou: Boolean = true,
    val galileo: Boolean = true,
    val sbas: Boolean = true,
    val qzss: Boolean = true,
    val irnss: Boolean = true
)

data class NmeaSettings(
    val gpgga: String = "1HZ",
    val gpgsa: String = "1S",
    val gpgsv: String = "5S",
    val gpgst: String = "1S",
    val gpzda: String = "1S",
    val gprmc: String = "OFF",
    val gpvtg: String = "OFF"
)

data class DeviceInformation(
    val deviceSn: String = "T28R59116002315",
    val deviceFirmware: String = "V2_0_54-A-20250915",
    val sensorVersion: String = "60240366",
    val gnssFirmware: String = "",
    val currentDatalink: String = "Phone Internet",
    val batteryPower: String = "65%",
    val expirationDate: String = "20260105",
    val antennaModel: String = "TX-CSX327A",
    val antennaRadiusMm: String = "60.15",
    val antennaHeightMm: String = "58",
    val antennaHl1Mm: String = "33.9",
    val antennaHl2Mm: String = "25.9"
)

data class DgpsUiSettings(
    val selectedBluetoothAddress: String = "",
    val rover: RoverSettings = RoverSettings(),
    val base: BaseSettings = BaseSettings(),
    val staticSurvey: StaticSurveySettings = StaticSurveySettings(),
    val inspectionAccuracy: InspectionAccuracySettings = InspectionAccuracySettings(),
    val deviceSettings: DeviceSettings = DeviceSettings(),
    val gnssSystems: GnssSystemSettings = GnssSystemSettings(),
    val nmea: NmeaSettings = NmeaSettings(),
    val deviceInfo: DeviceInformation = DeviceInformation()
)

fun getFixQualityString(quality: Int): String {
    return when (quality) {
        0 -> "Initialized"
        1 -> "Single"
        2 -> "Code Differential"
        3 -> "Invalid PPS"
        4 -> "Fixed"
        5 -> "Float"
        6 -> "Estimating"
        7 -> "Manual Fixed Value"
        8 -> "Simulated Mode"
        9 -> "WAAS Differential"
        else -> "Unknown"
    }
}

sealed class DgpsStatus {
    object Idle : DgpsStatus()
    object Connecting : DgpsStatus()
    object Connected : DgpsStatus()
    data class Error(val message: String) : DgpsStatus()
}
