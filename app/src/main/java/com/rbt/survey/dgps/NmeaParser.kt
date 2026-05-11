package com.rbt.survey.dgps

class NmeaParser {
    private var currentLocation = DgpsLocation(0.0, 0.0, 0.0, 0f, 0f, 0f, 0, 0)
    private val satelliteMap = mutableMapOf<Int, SatelliteInfo>()
    private val usedSatellites = mutableSetOf<Int>()
    private var gsvTotalSentences = 0
    private var gsvCurrentSentence = 0

    fun parse(nmea: String): DgpsLocation? {
        if (!nmea.startsWith("$")) return null
        
        val parts = nmea.split(",")
        if (parts.isEmpty()) return null
        
        val sentenceType = parts[0]
        
        when {
            sentenceType.endsWith("GGA") -> parseGGA(parts)
            sentenceType.endsWith("GSA") -> parseGSA(parts)
            sentenceType.endsWith("GST") -> parseGST(parts)
            sentenceType.endsWith("GSV") -> parseGSV(parts)
            sentenceType.endsWith("RMC") -> parseRMC(parts)
            sentenceType.endsWith("VTG") -> parseVTG(parts)
            sentenceType.endsWith("ZDA") -> parseZDA(parts)
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
            val utcTime = parts.getOrNull(1).orEmpty()
            val ageSeconds = parts.getOrNull(13)?.toFloatOrNull()
            val baseStationId = parts.getOrNull(14)?.split("*")?.firstOrNull()?.takeIf { it.isNotBlank() }
            
            if (latRaw.isEmpty() || lonRaw.isEmpty()) return
            
            val lat = convertToDecimal(latRaw, latDir)
            val lon = convertToDecimal(lonRaw, lonDir)
            
            // If we don't have GST yet, estimate accuracy based on Fix Quality
            val estimatedAccuracy = if (currentLocation.hrms > 0) {
                currentLocation.hrms
            } else {
                when (fixQuality) {
                    4 -> 0.01f // RTK Fixed (approx 1cm)
                    5 -> 0.15f // RTK Float (approx 15cm)
                    2 -> 0.40f // DGPS (approx 40cm)
                    1 -> hdop * 3.0f // Standard GPS (approx 3m)
                    else -> hdop * 5.0f
                }
            }
            
            currentLocation = currentLocation.copy(
                latitude = lat,
                longitude = lon,
                altitude = altitude,
                accuracy = estimatedAccuracy,
                fixQuality = fixQuality,
                satellites = satellites,
                hdop = hdop,
                ageSeconds = ageSeconds,
                utcDateTime = mergeDateTime(currentLocation.utcDateTime, utcTime = utcTime),
                baseStationId = baseStationId,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseGSA(parts: List<String>) {
        if (parts.size < 17) return

        try {
            usedSatellites.clear()
            for (i in 3..14) {
                parts.getOrNull(i)?.toIntOrNull()?.let { usedSatellites.add(it) }
            }

            val pdop = parts.getOrNull(15)?.toFloatOrNull()
            val hdop = parts.getOrNull(16)?.toFloatOrNull()
            val vdop = parts.getOrNull(17)?.split("*")?.firstOrNull()?.toFloatOrNull()

            satelliteMap.replaceAll { _, sat ->
                sat.copy(usedInFix = sat.prn in usedSatellites)
            }

            currentLocation = currentLocation.copy(
                pdop = pdop,
                hdop = hdop ?: currentLocation.hdop,
                vdop = vdop,
                satellitesList = satelliteMap.values.toList()
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
            val constellation = constellationFromSentence(parts[0])
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
                
                satelliteMap[prn] = SatelliteInfo(
                    prn = prn,
                    elevation = elevation,
                    azimuth = azimuth,
                    snr = snr,
                    constellation = constellation,
                    usedInFix = prn in usedSatellites
                )
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

    private fun parseRMC(parts: List<String>) {
        if (parts.size < 10) return

        try {
            val speedKnots = parts.getOrNull(7)?.toFloatOrNull() ?: 0f
            val heading = parts.getOrNull(8)?.toFloatOrNull() ?: 0f
            val date = parts.getOrNull(9).orEmpty()
            val time = parts.getOrNull(1).orEmpty()

            currentLocation = currentLocation.copy(
                speedMps = speedKnots * 0.514444f,
                headingDegrees = heading,
                utcDateTime = mergeDateTime(currentLocation.utcDateTime, date, time)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseVTG(parts: List<String>) {
        if (parts.size < 8) return

        try {
            val heading = parts.getOrNull(1)?.toFloatOrNull() ?: currentLocation.headingDegrees
            val speedKph = parts.getOrNull(7)?.split("*")?.firstOrNull()?.toFloatOrNull()
            currentLocation = currentLocation.copy(
                headingDegrees = heading,
                speedMps = speedKph?.div(3.6f) ?: currentLocation.speedMps
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseZDA(parts: List<String>) {
        if (parts.size < 5) return

        try {
            val time = parts.getOrNull(1).orEmpty()
            val day = parts.getOrNull(2).orEmpty().padStart(2, '0')
            val month = parts.getOrNull(3).orEmpty().padStart(2, '0')
            val year = parts.getOrNull(4).orEmpty()
            val date = if (day.isNotBlank() && month.isNotBlank() && year.isNotBlank()) "$day$month${year.takeLast(2)}" else ""

            currentLocation = currentLocation.copy(
                utcDateTime = mergeDateTime(currentLocation.utcDateTime, date, time)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun constellationFromSentence(sentenceType: String): SatelliteConstellation {
        return when {
            sentenceType.startsWith("\$GP") -> SatelliteConstellation.GPS
            sentenceType.startsWith("\$GL") -> SatelliteConstellation.GLONASS
            sentenceType.startsWith("\$GA") -> SatelliteConstellation.GALILEO
            sentenceType.startsWith("\$GB") || sentenceType.startsWith("\$BD") -> SatelliteConstellation.BEIDOU
            sentenceType.startsWith("\$GQ") -> SatelliteConstellation.QZSS
            sentenceType.startsWith("\$GI") || sentenceType.startsWith("\$IR") -> SatelliteConstellation.IRNSS
            sentenceType.startsWith("\$SB") -> SatelliteConstellation.SBAS
            else -> SatelliteConstellation.UNKNOWN
        }
    }

    private fun mergeDateTime(existing: String?, date: String? = null, utcTime: String): String? {
        val cleanTime = utcTime.takeIf { it.isNotBlank() }?.split(".")?.firstOrNull() ?: return existing
        val formattedTime = formatTime(cleanTime)
        val formattedDate = when {
            !date.isNullOrBlank() && date.length >= 6 -> {
                val day = date.substring(0, 2)
                val month = date.substring(2, 4)
                val year = "20${date.substring(4, 6)}"
                "$year-$month-$day"
            }
            !existing.isNullOrBlank() && existing.contains(' ') -> existing.substringBefore(' ')
            else -> null
        }
        return if (formattedDate != null) "$formattedDate $formattedTime" else formattedTime
    }

    private fun formatTime(raw: String): String {
        if (raw.length < 6) return raw
        val hh = raw.substring(0, 2)
        val mm = raw.substring(2, 4)
        val ss = raw.substring(4, 6)
        return "$hh:$mm:$ss"
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
