package com.rwpiri.uptime.data

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.URI
import java.net.URISyntaxException

interface UptimeApi {
    @GET("targets")
    suspend fun getTargets(): List<TargetWithChecks>

    @POST("auth/verify")
    suspend fun verifyToken(@Header("Authorization") authorization: String): Unit

    @POST("targets")
    suspend fun createTarget(
        @Header("Authorization") authorization: String,
        @Body request: CreateTargetRequest,
    ): Target

    @PUT("target/{id}")
    suspend fun updateTarget(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: UpdateTargetRequest,
    ): Target

    @DELETE("target/{id}")
    suspend fun deleteTarget(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
    )
}

object UptimeApiFactory {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun create(serverUrl: String): UptimeApi? {
        val baseUrl = normalizeBaseUrl(serverUrl) ?: return null
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UptimeApi::class.java)
    }

    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        val uri = try { URI(trimmed) } catch (_: URISyntaxException) { return null }
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return null
        if (uri.path.orEmpty().isNotEmpty() && uri.path != "/api/v1") return null
        return "${uri.scheme}://${uri.rawAuthority}/api/v1/"
    }
}
