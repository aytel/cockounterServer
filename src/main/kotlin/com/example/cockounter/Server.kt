package com.example.cockounter

import com.example.cockounter.core.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.util.*

class HttpServer {
    companion object {
        private const val CREATE_SESSION = "create"
        private const val UPDATE_GAME_STATE = "update_gs"
        private const val CONNECT_TO_SESSION = "connect/{uuid}"
        private const val GET_GAME_SESSION = "get/{uuid}"
        private const val CHANGE_TOKEN = "/change"

        private val storage = Storage()

        @JvmStatic
        fun main(args: Array<String>) {
            System.err.println("Started!")
            embeddedServer(Netty, System.getenv("PORT").toInt()) {
                routing {
                    post(CREATE_SESSION) {
                        System.err.printf("referrer = %s\n", call.request.headers["Referrer"])
                        System.err.printf("referrer = %s\n", call.request.headers["Referer"])
                        System.err.printf("referrer = %s\n", call.request.headers["referrer"])
                        System.err.printf("referrer = %s\n", call.request.headers["referer"])
                        val parameters = call.receiveParameters()
                        val captureString = parameters["capture"]
                        System.err.println("capture = $captureString")
                        try {
                            val capture: StateCapture = StateCaptureConverter.gson.fromJson(
                                captureString,
                                StateCapture::class.java
                            )
                            System.err.println("built")
                            storage.add(capture)
                            assert(storage.findStateCaptureByUUID(capture.uuid) != null)
                            System.err.println("saved")
                            call.respond("true")
                        } catch (e: Exception) {
                            e.printStackTrace(System.err)
                            call.respond("false")
                        }
                    }
                    get(CONNECT_TO_SESSION) {
                        val uuidString = call.parameters["uuid"]
                        System.err.println("uuid = $uuidString")
                        try {
                            val uuid = UUID.fromString(uuidString)
                            val capture = storage.findStateCaptureByUUID(uuid)
                            if (capture != null) {
                                call.respond(StateCaptureConverter.gson.toJson(capture))
                            } else {
                                call.respond("false")
                            }
                        } catch (e: IllegalArgumentException) {
                            call.respond("false")
                        }

                    }
                    get(GET_GAME_SESSION) {
                        val uuid = UUID.fromString(call.parameters["uuid"])
                        System.err.println("uuid = $uuid")
                        try {
                            call.respond(StateCaptureConverter.gson.toJson(storage.findStateCaptureByUUID(uuid)!!.state))
                        } catch (e: Exception) {
                            System.err.println("Not found")
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    post(UPDATE_GAME_STATE) {
                        try {
                            val parameters = call.receiveParameters()
                            System.err.printf("uuid = %s\n, state = %s\n", parameters["uuid"], parameters["state"])
                            val uuid = UUID.fromString(parameters["uuid"])!!
                            val state = StateCaptureConverter.gson.fromJson(
                                parameters["state"],
                                GameState::class.java
                            )!!
                            val version = state.version
                            val resultState = storage.update(version, uuid, state)
                            val resultJSON = StateCaptureConverter.gson.toJson(resultState)
                            call.respond(resultJSON)

                            storage.getAllAdresses(uuid)?.forEach {
                                val token = it.token
                                val message = Message.builder()
                                    .putData("state", resultJSON)
                                    .setToken(token)
                                    .build()
                                FirebaseMessaging.getInstance().send(message)
                            }

                        } catch (e: Exception) {
                            e.printStackTrace(System.err)
                        }
                    }
                    post(CHANGE_TOKEN) {
                        val parameters = call.receiveParameters()
                        val oldToken = parameters["old"]
                        val newToken = parameters["new"]
                        val uuid = UUID.fromString(parameters["uuid"])!!

                        System.err.println("old = $oldToken, new = $newToken")

                        if (oldToken != null)
                            storage.delete(ClientAddress(uuid, oldToken))
                        if (newToken != null)
                            storage.add(ClientAddress(uuid, newToken))
                    }
                }
            }.start(wait = true)
        }
    }
}