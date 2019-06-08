package com.example.cockounter

import com.example.cockounter.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.*

//import org.junit.jupiter.api.Assertions.*

internal class StorageTest {
    val json: String = "{\"date\":\"Jan 1, 1970 3:00:00 AM\",\"id\":0,\"name\":\"\",\"players\":[{\"name\":\"aa\",\"role\":\"a\"}],\"preset\":{\"actionsStubs\":[],\"globalParameters\":{},\"libraries\":[],\"roles\":{\"a\":{\"actionsStubs\":[],\"name\":\"a\",\"privateParameters\":{},\"sharedParameters\":{}}}},\"state\":{\"globalParameters\":{},\"roles\":{\"a\":{\"name\":\"a\",\"players\":{\"aa\":{\"name\":\"aa\",\"privateParameters\":{}}},\"sharedParameters\":{}}},\"version\":0},\"uuid\":\"95e33b05-0fd1-4874-9a9b-0f4b89e1b90e\"}"
    val uuid = UUID.fromString("95e33b05-0fd1-4874-9a9b-0f4b89e1b90e");

    val storage = Storage()

    @Test
    fun testAdd() {
        val capture = StateCaptureConverter.gson.fromJson(json, StateCapture::class.java)!!
        storage.add(capture)
        assertNotNull(storage.findStateCaptureByUUID(uuid))
    }

    @Test
    fun testFind() {
        val capture = storage.findStateCaptureByUUID(uuid)
        assertNotNull(capture)
        print(capture!!.uuid.toString())
    }

    @Test
    fun testUpdate() {
        val capture = storage.findStateCaptureByUUID(uuid)!!
        val prev = capture.state.version
        val curUUID = UUID.fromString(uuid.toString())
        storage.update(capture.state.version, uuid, capture.state)
        assertEquals(prev + 1, storage.findStateCaptureByUUID(curUUID)!!.state.version)
    }

    @Test
    fun testMain() {
        HttpServer.main(emptyArray())
    }
}