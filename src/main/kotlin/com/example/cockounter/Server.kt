package com.example.cockounter

import com.example.cockounter.classes.GameState
import com.example.cockounter.classes.StateCapture
import com.example.cockounter.classes.StateCaptureConverter
import com.google.gson.JsonSyntaxException
import io.ktor.application.Application
import io.ktor.application.call
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
        private const val UPDATE_GAME_STATE = "update_gs/{uuid}"
        private const val CONNECT_TO_SESSION = "connect/{uuid}"
        private const val GET_GAME_SESSION = "get/{uuid}"

        private val storage = Storage()

        @JvmStatic
        fun main(args: Array<String>) {
            System.err.println("Started!")
            embeddedServer(Netty, System.getenv("PORT").toInt()) {
                routing {
                    post(CREATE_SESSION) {
                        val parameters = call.receiveParameters()
                        val captureString = parameters["capture"]
                        System.err.println("capture = $captureString")
                        //call.respond("false")
                        try {
                            val capture: StateCapture = StateCaptureConverter.gson.fromJson(
                                captureString,
                                StateCapture::class.java
                            )
                            storage.add(capture)
                            call.respond("true")
                        } catch (e: JsonSyntaxException) {
                            call.respond("false")
                        }
                    }
                    get(CONNECT_TO_SESSION) {
                        val uuidString = call.parameters["uuid"]
                        System.err.println("uuid = $uuidString")
                        try {
                            val uuid = UUID.fromString(uuidString)
                            val capture = storage.findByUUID(uuid)
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
                            call.respond(StateCaptureConverter.gson.toJson(storage.findByUUID(uuid)!!.state))
                        } catch (e: Exception) {
                            System.err.println("Not found")
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                    post(UPDATE_GAME_STATE) {
                        val uuid = UUID.fromString(call.parameters["uuid"])
                        val version = call.parameters["version"]!!.toInt()
                        val state = StateCaptureConverter.gson.fromJson(
                            call.parameters["state"],
                            GameState::class.java
                        )
                        storage.update(version, uuid, state)
                    }
                }
            }.start(wait = true)
        }
    }
}