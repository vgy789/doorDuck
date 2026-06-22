package io.github.vgy789.doorDuck.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface RocketChatApi {
    @GET("me")
    suspend fun getMe(): MeResponse

    @POST("dm.create")
    suspend fun createDirectMessage(@Body body: DmCreateRequest): DmCreateResponse

    @POST("commands.run")
    suspend fun runSlashCommand(@Body body: RunCommandRequest): RunCommandResponse

    @POST("chat.sendMessage")
    suspend fun sendMessage(@Body body: SendMessageRequest): SendMessageResponse

    @GET("users.list")
    suspend fun getUsers(
        @Query("query") query: String,
        @Query("fields") fields: String = """{"username":1,"type":1}""",
        @Query("count") count: Int = 20,
    ): UsersListResponse

    @GET("im.messages")
    suspend fun getMessages(
        @Query("roomId") roomId: String,
        @Query("count") count: Int = 30,
    ): ImMessagesResponse
}

@Serializable
data class DmCreateRequest(
    val username: String,
)

@Serializable
data class RunCommandRequest(
    val command: String,
    val roomId: String,
    val params: String? = null,
)

@Serializable
data class RunCommandResponse(
    val success: Boolean = false,
)

@Serializable
data class MeResponse(
    val success: Boolean? = null,
)

@Serializable
data class DmCreateResponse(
    val success: Boolean = false,
    val room: RoomDto? = null,
)

@Serializable
data class RoomDto(
    val rid: String? = null,
    @SerialName("_id") val id: String? = null,
)

@Serializable
data class SendMessageRequest(
    val message: SendMessagePayload,
)

@Serializable
data class SendMessagePayload(
    val rid: String,
    val msg: String,
)

@Serializable
data class SendMessageResponse(
    val success: Boolean = false,
)

@Serializable
data class ImMessagesResponse(
    val success: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
)

@Serializable
data class MessageDto(
    @SerialName("_id") val id: String? = null,
    val msg: String? = null,
    val u: MessageUserDto? = null,
)

@Serializable
data class MessageUserDto(
    @SerialName("_id") val id: String? = null,
    val username: String? = null,
)

@Serializable
data class UsersListResponse(
    val success: Boolean = false,
    val users: List<UserDto> = emptyList(),
)

@Serializable
data class UserDto(
    @SerialName("_id") val id: String? = null,
    val username: String? = null,
    val type: String? = null,
)
