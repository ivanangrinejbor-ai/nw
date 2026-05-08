package org.catrobat.catroid.utils.git

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubActionsApi {

    @POST("repos/{owner}/{repo}/forks")
    suspend fun createFork(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<ResponseBody>

    @GET("user")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFile(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String
    ): Response<ResponseBody>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFile(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("repos/{owner}/{repo}/actions/workflows/ai_feature_build.yml/dispatches")
    suspend fun triggerWorkflow(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: RequestBody
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/git/ref/heads/{branch}")
    suspend fun getRef(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<ResponseBody>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}
