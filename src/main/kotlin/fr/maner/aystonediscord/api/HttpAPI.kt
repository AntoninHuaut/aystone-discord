package fr.maner.aystonediscord.api

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object HttpAPI {

    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun get(url: String, bearerToken: String? = null): String {
        val request = Request.Builder().url(url)

        bearerToken?.let {
            request.addHeader("Authorization", "Bearer $it")
        }

        client.newCall(request.build()).execute().use { response ->
            return response.body.string()
        }
    }

    @Throws(IOException::class)
    fun postFormData(url: String, params: Map<String, String>): String {
        val formBuilder = FormBody.Builder()
        params.forEach { (key, value) ->
            formBuilder.add(key, value)
        }

        val request = Request.Builder()
            .url(url)
            .post(formBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            return response.body.string()
        }
    }

}
