package com.rbt.survey.data.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import okhttp3.ResponseBody

fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun savePdf(
    context: Context,
    body: ResponseBody,
    fileName: String
): Pair<Uri, String>? {

    val values = ContentValues().apply {

        put(
            MediaStore.Downloads.DISPLAY_NAME,
            fileName
        )

        put(
            MediaStore.Downloads.MIME_TYPE,
            "application/pdf"
        )

        put(
            MediaStore.Downloads.RELATIVE_PATH,
            Environment.DIRECTORY_DOWNLOADS
        )
    }

    val uri =
        context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: return null

    context.contentResolver
        .openOutputStream(uri)
        ?.use { output ->

            body.byteStream().use { input ->

                input.copyTo(output)
            }
        }

    var actualFileName = fileName

    context.contentResolver.query(
        uri,
        arrayOf(MediaStore.Downloads.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            actualFileName =
                cursor.getString(
                    cursor.getColumnIndexOrThrow(
                        MediaStore.Downloads.DISPLAY_NAME
                    )
                )
        }
    }

    return Pair(
        uri,
        actualFileName
    )
}


fun showDownloadNotification(
    context: Context,
    fileName: String,
    pdfUri: Uri
) {

    val manager =
        context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channel = NotificationChannel(
            "downloads",
            "Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannel(channel)
    }

    val intent = Intent(
        Intent.ACTION_VIEW
    ).apply {

        setDataAndType(
            pdfUri,
            "application/pdf"
        )

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val pendingIntent =
        PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

    val notification =
        NotificationCompat.Builder(
            context,
            "downloads"
        )
            .setSmallIcon(
                android.R.drawable.stat_sys_download_done
            )
            .setContentTitle("Download Complete")
            .setContentText(fileName)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

    manager.notify(
        System.currentTimeMillis().toInt(),
        notification
    )
}