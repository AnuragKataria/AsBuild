package com.rbt.survey.dgps

class NmeaParser {
    private var currentLocation = DgpsLocation(0.0, 0.0, 0.0, 0f, 0f, 0f, 0, 0)
    private val satelliteMap = mutableMapOf<Int, SatelliteInfo>()
    private var gsvTotalSentences = 0
    private var gsvCurrentSentence = 0

    fun parse(nmea: String): DgpsLocation? {
        if (!nmea.startsWith("$")) return null
        
        val parts = nmea.split(",")
        if (parts.isEmpty()) return null
        
        val sentenceType = parts[0]
        
        when {
            sentenceType.endsWith("GGA") -> parseGGA(parts)
            sentenceType.endsWith("GST") -> parseGST(parts)
            sentenceType.endsWith("GSV") -> parseGSV(parts)
        }
        
        return currentLocation
    }

    private fun parseGGA(parts: List<String>) {
        if (parts.size < 10) return
        
        try {
            val latRaw = parts[2]
            val latDir = parts[3]
            val lonRaw = parts[4]
            val lonDir = parts[5]
            val fixQuality = parts[6].toIntOrNull() ?: 0
            val satellites = parts[7].toIntOrNull() ?: 0
            val hdop = parts[8].toFloatOrNull() ?: 0f
            val altitude = parts[9].toDoubleOrNull() ?: 0.0
            
            if (latRaw.isEmpty() || lonRaw.isEmpty()) return
            
            val lat = convertToDecimal(latRaw, latDir)
            val lon = convertToDecimal(lonRaw, lonDir)
            
            // If we don't have GST yet, estimate accuracy based on Fix Quality
            val estimatedAccuracy = if (currentLocation.hrms > 0) {
                currentLocation.hrms
            } else {
                when (fixQuality) {
                    4 -> 0.02f // RTK Fixed (approx 2cm)
                    5 -> 0.50f // RTK Float (approx 50cm)
                    2 -> 2.0f  // DGPS
                    1 -> hdop * 5.0f // Standard GPS
                    else -> hdop * 10.0f
                }
            }
            
            currentLocation = currentLocation.copy(
                latitude = lat,
                longitude = lon,
                altitude = altitude,
                accuracy = estimatedAccuracy,
                fixQuality = fixQuality,
                satellites = satellites,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseGST(parts: List<String>) {
        // $GPGST,hhmmss.ss,a,b,c,d,e,f,g*hh
        // e: RMS value of the standard deviation of the latitude error (meters)
        // f: RMS value of the standard deviation of the longitude error (meters)
        // g: RMS value of the standard deviation of the altitude error (meters)
        if (parts.size < 8) return
        
        try {
            val hrmsLat = parts[6].toFloatOrNull() ?: 0f
            val hrmsLon = parts[7].toFloatOrNull() ?: 0f
            val vrms = if (parts.size > 8) parts[8].split("*")[0].toFloatOrNull() ?: 0f else 0f
            
            // HRMS is often calculated as sqrt(lat^2 + lon^2)
            val hrms = Math.sqrt((hrmsLat * hrmsLat + hrmsLon * hrmsLon).toDouble()).toFloat()
            
            if (hrms > 0) {
                currentLocation = currentLocation.copy(
                    hrms = hrms,
                    vrms = vrms,
                    accuracy = hrms
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseGSV(parts: List<String>) {
        // $GPGSV,total,msg_num,total_sats,prn,ele,azi,snr, ... *hh
        if (parts.size < 4) return
        
        try {
            val totalSentences = parts[1].toIntOrNull() ?: 0
            val sentenceNumber = parts[2].toIntOrNull() ?: 0
            
            if (sentenceNumber == 1) {
                // Reset on first sentence of a sequence
                if (gsvCurrentSentence == gsvTotalSentences) {
                    satelliteMap.clear()
                }
            }
            
            gsvTotalSentences = totalSentences
            gsvCurrentSentence = sentenceNumber
            
            // Satellites start at index 4, group of 4 fields per satellite
            for (i in 4 until parts.size - 3 step 4) {
                val prn = parts[i].toIntOrNull() ?: continue
                val elevation = parts[i+1].toIntOrNull() ?: 0
                val azimuth = parts[i+2].toIntOrNull() ?: 0
                val snrPart = parts[i+3].split("*")[0]
                val snr = snrPart.toIntOrNull() ?: 0
                
                satelliteMap[prn] = SatelliteInfo(prn, elevation, azimuth, snr)
            }
            
            if (sentenceNumber == totalSentences) {
                currentLocation = currentLocation.copy(
                    satellitesList = satelliteMap.values.toList()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun convertToDecimal(raw: String, direction: String): Double {
        if (raw.isEmpty()) return 0.0
        
        val dotIndex = raw.indexOf('.')
        if (dotIndex < 2) return 0.0
        
        val degrees = raw.substring(0, dotIndex - 2).toDoubleOrNull() ?: 0.0
        val minutes = raw.substring(dotIndex - 2).toDoubleOrNull() ?: 0.0
        
        var decimal = degrees + (minutes / 60.0)
        if (direction == "S" || direction == "W") {
            decimal *= -1
        }
        return decimal
    }
}
