package com.rbt.survey.data.remote

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.rbt.survey.data.local.UserPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitClient {
    private const val BASE_URL = "https://webgis.rbt-ltd.com/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun getUnsafeOkHttpClient(context: Context, preferences: UserPreferences? = null): OkHttpClient {
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(logging)
                .addInterceptor(ChuckerInterceptor.Builder(context).build())

            if (preferences != null) {
                builder.addInterceptor(AuthInterceptor(preferences))
                // Attach the TokenAuthenticator specifically for this authenticated client
                builder.authenticator(TokenAuthenticator(preferences, getAuthApi(context)))
            }

            return builder.build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Method to get the basic AuthApi (Login/Refresh) - uses basic OkHttpClient without Authenticator
    fun getAuthApi(context: Context): AuthApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getBasicUnsafeOkHttpClient(context)) // No Interceptors or Authenticators
            .build()
            .create(AuthApi::class.java)
    }

    private fun getBasicUnsafeOkHttpClient(context: Context): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(logging)
            .addInterceptor(ChuckerInterceptor.Builder(context).build())
            .build()
    }

    // Method to get an authenticated API instance
    fun getAuthenticatedApi(context: Context, preferences: UserPreferences): AuthApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getUnsafeOkHttpClient(context, preferences))
            .build()
            .create(AuthApi::class.java)
    }

    fun getGeoApi(context: Context, preferences: UserPreferences): GeoApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(getUnsafeOkHttpClient(context, preferences))
            .build()
            .create(GeoApi::class.java)
    }
}
