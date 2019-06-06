package com.example.cockounter

import com.example.cockounter.classes.GameState
import com.example.cockounter.classes.StateCapture
import com.mongodb.MongoClient
import com.mongodb.QueryBuilder
import xyz.morphia.Datastore
import xyz.morphia.Morphia
import xyz.morphia.annotations.Entity
import xyz.morphia.annotations.Id
import xyz.morphia.annotations.Indexed
import xyz.morphia.query.Query
import java.util.*

class Storage() {
    private val morphia = Morphia().mapPackage("com.example.cockounter")
    private val datastore = morphia.createDatastore(MongoClient(), "commonStorage")

    init {
        datastore.ensureIndexes()
    }

    private fun find(uuid: UUID): Query<StateCapture> {
        val query = datastore.createQuery(StateCapture::class.java)
        query.and(
            query.criteria("uuid").equal(uuid)
        )
        return query
    }

    fun findByUUID(uuid: UUID): StateCapture? = find(uuid).get()

    fun update(version: Int, uuid: UUID, state: GameState): Pair<Boolean, GameState> {
        val capture = find(uuid).get()
        if (capture.state.version != version) {
            return (false to capture.state)
        }
        datastore.createUpdateOperations(StateCapture::class.java).set("state", state)
        datastore.createUpdateOperations(StateCapture::class.java).set("version", version + 1)
        return (true to state)
    }

    fun add(capture: StateCapture) {
        datastore.save(capture)
    }
}