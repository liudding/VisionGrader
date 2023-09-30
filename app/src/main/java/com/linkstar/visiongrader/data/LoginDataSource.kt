package com.linkstar.visiongrader.data

import android.util.Log
import com.linkstar.visiongrader.data.model.LoggedInUser
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.use
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException


/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {
    private val MEDIA_TYPE_JSON: MediaType = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient()

    @Throws(IOException::class)
    fun post(url: String, json: String): String {
        Log.d("post", json)
        val request: Request = Request.Builder()
            .url(url)
            .post(json.toRequestBody(MEDIA_TYPE_JSON))
            .build()
        client.newCall(request).execute().use {
                response ->

                Log.d("post", "" + response.code + " " + response.body?.string())
                if (!response.isSuccessful) {
                    throw IOException("error")
                }

                return response.body!!.string()

        }

//        val resp = client.newCall(request).execute()
//        Log.d("resp", "" + resp.code + " " + (resp.body?.string() ?: ""))

//        client.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                // Handle this
//                Log.d("post.resp", "error")
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                // Handle this
//                Log.d("post.resp", "success: " + response.code + " " + response.body.toString())
//            }
//        })

//        throw Exception("hi")
//        return ""
    }



    fun login(username: String, password: String): Result<LoggedInUser> {


        try {
            // TODO: handle loggedInUser authentication

            val url = "https://ps.edu.purvar.com/cjn-sso/api/v3/sso/security/getAccessToken"
            val params = mapOf("loginId" to username, "password" to password, "roleTypeUuid" to "ROLE_TYPE_001")

            Log.d("LoginDS", "call api")
            val result = post(url, params.toString())
            Log.d("LoginDS", result)

            val json = JSONTokener(result).nextValue() as JSONObject
            val data = json.getJSONObject("operativeDto")
            val school = json.getJSONObject("school")


            val user = LoggedInUser(
                username,
                password,
                json.getString("accessToken"),
                data.getString("userUuid"),
                data.getString("fullName"),
                data.getString("schUuid"),
                school.getString("schCode"),
                school.getString("schName"),
                school.getString("schShortName"))
            return Result.Success(user)
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in: " + e.message, e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}