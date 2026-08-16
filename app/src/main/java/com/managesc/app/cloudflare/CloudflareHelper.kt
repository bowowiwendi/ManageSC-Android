package com.managesc.app.cloudflare

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ---- Data classes Cloudflare ----
data class CfResult<T>(val success: Boolean, val errors: List<CfError>, val result: T?, val result_info: CfResultInfo?)
data class CfError(val code: Int, val message: String)
data class CfResultInfo(val page: Int, val per_page: Int, val total_count: Int)

data class CfZone(val id: String, val name: String, val status: String, val paused: Boolean)
data class CfDnsRecord(
    val id: String? = null,
    val type: String,          // A, AAAA, CNAME, TXT, MX, etc
    val name: String,          // subdomain.domain.com atau domain.com
    val content: String,       // IP atau target
    val ttl: Int = 1,          // 1 = automatic
    val proxied: Boolean = false,
    val zone_name: String? = null
)

interface CloudflareApi {
    @retrofit2.http.GET("client/v4/zones")
    suspend fun listZones(
        @Query("name") name: String? = null,
        @Query("status") status: String = "active",
        @Query("per_page") perPage: Int = 50
    ): Response<CfResult<List<CfZone>>>

    @retrofit2.http.GET("client/v4/zones/{zone_id}/dns_records")
    suspend fun listDns(
        @retrofit2.http.Path("zone_id") zoneId: String,
        @Query("per_page") perPage: Int = 100
    ): Response<CfResult<List<CfDnsRecord>>>

    @retrofit2.http.POST("client/v4/zones/{zone_id}/dns_records")
    suspend fun createDns(
        @retrofit2.http.Path("zone_id") zoneId: String,
        @Body body: CfDnsRecord
    ): Response<CfResult<CfDnsRecord>>

    @retrofit2.http.PUT("client/v4/zones/{zone_id}/dns_records/{id}")
    suspend fun updateDns(
        @retrofit2.http.Path("zone_id") zoneId: String,
        @retrofit2.http.Path("id") id: String,
        @Body body: CfDnsRecord
    ): Response<CfResult<CfDnsRecord>>

    @retrofit2.http.DELETE("client/v4/zones/{zone_id}/dns_records/{id}")
    suspend fun deleteDns(
        @retrofit2.http.Path("zone_id") zoneId: String,
        @retrofit2.http.Path("id") id: String
    ): Response<CfResult<Any>>
}

object CloudflareClient {
    fun api(email: String, key: String): CloudflareApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("X-Auth-Email", email)
                    .addHeader("X-Auth-Key", key)
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(req)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://api.cloudflare.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudflareApi::class.java)
    }
}
