package com.rbt.survey.dgps

class NmeaParser {
    fun parse(nmea: String): DgpsLocation? {
        if (!nmea.startsWith("$")) return null
        
        val parts = nmea.split(",")
        if (parts.isEmpty()) return null
        
        val sentenceType = parts[0]
        
        return when {
            sentenceType.endsWith("GGA") -> parseGGA(parts)
            else -> null
        }
    }

    private fun parseGGA(parts: List<String>): DgpsLocation? {
        if (parts.size < 15) return null
        
        try {
            val latRaw = parts[2]
            val latDir = parts[3]
            val lonRaw = parts[4]
            val lonDir = parts[5]
            val fixQuality = parts[6].toIntOrNull() ?: 0
            val satellites = parts[7].toIntOrNull() ?: 0
            val hdop = parts[8].toFloatOrNull() ?: 0f
            val altitude = parts[9].toDoubleOrNull() ?: 0.0
            
            if (latRaw.isEmpty() || lonRaw.isEmpty()) return null
            
            val lat = convertToDecimal(latRaw, latDir)
            val lon = convertToDecimal(lonRaw, lonDir)
            
            // Accuracy is estimated from HDOP if not provided directly
            // Usually accuracy = HDOP * 5.0 (rough estimate for meters)
            val accuracy = hdop * 5.0f 
            
            return DgpsLocation(
                latitude = lat,
                longitude = lon,
                altitude = altitude,
                accuracy = accuracy,
                fixQuality = fixQuality,
                satellites = satellites
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun convertToDecimal(raw: String, direction: String): Double {
        val degreeCount = if (raw.length == 10) 2 else 3 // DDMM.MMMM or DDDMM.MMMM
        // GGA format is usually DDMM.MMMMM
        // Latitude: DDMM.MMMMM (2 digits for degrees)
        // Longitude: DDDMM.MMMMM (3 digits for degrees)
        
        val dotIndex = raw.indexOf('.')
        val degrees = raw.substring(0, dotIndex - 2).toDouble()
        val minutes = raw.substring(dotIndex - 2).toDouble()
        
        var decimal = degrees + (minutes / 60.0)
        if (direction == "S" || direction == "W") {
            decimal *= -1
        }
        return decimal
    }
}
