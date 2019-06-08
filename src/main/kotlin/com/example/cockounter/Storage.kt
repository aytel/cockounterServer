package com.example.cockounter

import com.example.cockounter.core.*
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import xyz.morphia.Morphia
import xyz.morphia.query.Query
import java.util.*

class Storage {
    private val URI = "mongodb://heroku_57qtj3fn:9pfe4cdjuchb46dlunq2g6p2o3@ds235775.mlab.com:35775/heroku_57qtj3fn"
    private val mongoClientURI = MongoClientURI(URI)
    private val mongoClient = MongoClient(mongoClientURI)
    private val morphia = Morphia().mapPackage("com.example.cockounter")
    private val datastore = morphia.createDatastore(mongoClient, "heroku_57qtj3fn")

    init {
        datastore.ensureIndexes()
    }

    private fun findStateCapture(uuid: UUID): Query<StateCapture> {
        val query = datastore.createQuery(StateCapture::class.java)
        query.and(
            query.criteria("uuid").equal(uuid)
        )
        return query
    }

    fun findStateCaptureByUUID(uuid: UUID): StateCapture? = findStateCapture(uuid).get()

    fun update(version: Int, uuid: UUID, state: GameState): GameState {
        val capture = findStateCapture(uuid).get()!!

        if (capture.state.version != version) {
            return capture.state
        }
        val newState = GameState(state.globalParameters, state.roles, version + 1)
        datastore.update(capture, datastore.createUpdateOperations(StateCapture::class.java).set("state", newState))
        return newState
    }

    fun add(capture: StateCapture) {
        System.err.println("trying to save")
        datastore.save(capture)
        System.err.println("saved")
    }

    fun add(clientAddress: ClientAddress) {
        datastore.save(clientAddress)
    }

    fun delete(clientAddress: ClientAddress) {
        val query = datastore.createQuery(ClientAddress::class.java)
        query.and(
            query.criteria("uuid").equal(clientAddress.uuid),
            query.criteria("token").equal(clientAddress.token)
        )
        datastore.delete(query.get())
    }

    fun getAllAdresses(uuid: UUID): MutableList<ClientAddress>? {
        val query = datastore.createQuery(ClientAddress::class.java)
        query.and(
            query.criteria("uuid").equal(uuid)
        )
        return query.asList()
    }
}