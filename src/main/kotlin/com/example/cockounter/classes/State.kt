package com.example.cockounter.classes

//import androidx.room.*
import arrow.core.Option
import com.example.cockounter.classes.*
import com.github.andrewoma.dexx.kollection.ImmutableMap
import com.github.andrewoma.dexx.kollection.immutableMapOf
import com.github.andrewoma.dexx.kollection.toImmutableMap
import com.google.gson.GsonBuilder
import xyz.morphia.annotations.Entity
import xyz.morphia.annotations.Id
import xyz.morphia.annotations.Indexed
import java.io.Serializable
import java.util.*

@Entity
data class StateCapture(
    val id: Int = 0,
    val name: String = "",
    val state: GameState = GameState(),
    val preset: Preset = Preset(),
    val players: List<PlayerDescription> = emptyList(),
    val date: Date? = null,
    @Id val uuid: UUID = UUID.randomUUID()
)

data class GameState(
    val globalParameters: Map<String, GameParameter> = emptyMap(),
    val roles: Map<String, GameRole> = emptyMap(),
    val version: Int = 0
) :
    Serializable {
    companion object
}

class StateCaptureConverter {
    companion object {
        val gson = GsonBuilder().registerTypeAdapter(GameParameter::class.java, InterfaceAdapter<GameParameter>())
            .registerTypeAdapter(Parameter::class.java, InterfaceAdapter<Parameter>()).create()

        data class PlayersWrapper(val players: List<PlayerDescription>)
    }

    fun fromGameState(gameState: GameState): String =
        gson.toJson(gameState)

    fun fromPreset(preset: Preset): String =
        gson.toJson(preset)

    fun fromPlayers(players: List<PlayerDescription>): String =
        gson.toJson(PlayersWrapper(players))

    fun fromDate(date: Date): String =
        gson.toJson(date)

    fun toGameState(data: String): GameState =
        gson.fromJson(data, GameState::class.java)

    fun toPreset(data: String): Preset =
        gson.fromJson(data, Preset::class.java)

    fun toPlayers(data: String): List<PlayerDescription> =
        gson.fromJson(data, PlayersWrapper::class.java).players

    fun toDate(data: String): Date =
        gson.fromJson(data, Date::class.java)
}

val dummyState = GameState(immutableMapOf(), immutableMapOf())

data class GameRole(
    val name: String = "",
    val sharedParameters: Map<String, GameParameter> = emptyMap(),
    val players: Map<String, Player> = emptyMap()
) : Serializable {
    companion object
}

data class Player(
    val name: String = "",
    val privateParameters: Map<String, GameParameter> = emptyMap()
) : Serializable {
    companion object
}

sealed class GameParameter : Serializable {
    abstract val name: String
    abstract val visibleName: String
    abstract fun valueString(): String
}

data class IntegerGameParameter(override val name: String, override val visibleName: String, val value: Int) :
    GameParameter() {
    override fun valueString() = "Integer: $value"
}

data class StringGameParameter(override val name: String, override val visibleName: String, val value: String) :
    GameParameter() {
    override fun valueString() = "String: $value"
}

data class DoubleGameParameter(override val name: String, override val visibleName: String, val value: Double) :
    GameParameter() {
    override fun valueString() = "Double: $value"
}

data class BooleanGameParameter(override val name: String, override val visibleName: String, val value: Boolean) :
    GameParameter() {
    override fun valueString() = "Boolean: $value"
}

operator fun GameState.get(role: String) = roles!!.getValue(role)
operator fun GameState.get(description: PlayerDescription) = get(description.role)[description.name]
operator fun GameState.get(gameParameterPointer: GameParameterPointer) = when (gameParameterPointer) {
    is GameParameterPointer.Global -> this.globalParameters!![gameParameterPointer.name]!!
    is GameParameterPointer.Shared -> roles!![gameParameterPointer.role]!!.sharedParameters[gameParameterPointer.name]!!
    is GameParameterPointer.Private -> get(gameParameterPointer.player).privateParameters[gameParameterPointer.name]!!
}

fun <V> ImmutableMap<String, V>.modify(key: String, f: (V) -> V): ImmutableMap<String, V> {
    val oldValue = getValue(key)
    return put(key, f(oldValue))
}

operator fun GameState.set(gameParameterPointer: GameParameterPointer, parameter: GameParameter): GameState =
    when (gameParameterPointer) {
        is GameParameterPointer.Global -> this.copy(
            globalParameters = globalParameters.toImmutableMap().put(
                gameParameterPointer.name,
                parameter
            )
        )
        is GameParameterPointer.Shared -> copy(roles = roles.toImmutableMap().modify(gameParameterPointer.role) {
            it.copy(sharedParameters = it.sharedParameters.toImmutableMap().modify(gameParameterPointer.name) {parameter})
        })
        is GameParameterPointer.Private -> copy(roles = roles.toImmutableMap().modify(gameParameterPointer.player.role) {
            it.copy(players = it.players.toImmutableMap().modify(gameParameterPointer.player.name) {
                it.copy(privateParameters = it.privateParameters.toImmutableMap().modify(gameParameterPointer.name) { parameter })
            })
        })
    }

operator fun GameRole.get(player: String) = players.getValue(player)

data class PlayerDescription(val name: String = "", val role: String = "")

sealed class GameParameterPointer {
    data class Global(val name: String) : GameParameterPointer()
    data class Shared(val role: String, val name: String) : GameParameterPointer()
    data class Private(val player: PlayerDescription, val name: String) : GameParameterPointer()
}


fun buildGameParameter(parameter: Parameter) = when (parameter) {
    is IntegerParameter -> IntegerGameParameter(parameter.name, parameter.visibleName, parameter.initialValue)
    is StringParameter -> StringGameParameter(parameter.name, parameter.visibleName, parameter.initialValue)
    is DoubleParameter -> DoubleGameParameter(parameter.name, parameter.visibleName, parameter.initialValue)
    is BooleanParameter -> BooleanGameParameter(parameter.name, parameter.visibleName, parameter.initialValue)
}

fun buildPlayer(role: Role, playerName: PlayerDescription) =
    Player(playerName.name, role.privateParameters.mapValues { buildGameParameter(it.value) }.toImmutableMap())

fun buildState(preset: Preset, players: List<PlayerDescription>): GameState {
    val globalParameters = preset.globalParameters.mapValues { buildGameParameter(it.value) }.toImmutableMap()
    val byRole = players.map { Pair(it.role, buildPlayer(preset.roles.getValue(it.role), it)) }.groupBy { it.first }
        .mapValues { it.value.map { it.second } }
    val roles = preset.roles.mapValues { (key, v) ->
        GameRole(
            key,
            v.sharedParameters.mapValues { buildGameParameter(it.value) }.toImmutableMap(),
            byRole.getValue(key).map { Pair(it.name, it) }.toImmutableMap()
        )
    }
    return GameState(globalParameters, roles.toImmutableMap())
}



sealed class ActionButtonDescription {
    data class Attached(val parameter: GameParameterPointer, val index: Int) : ActionButtonDescription()
    data class Global(val index: Int) : ActionButtonDescription()
    data class Role(val role: String, val index: Int) : ActionButtonDescription()
}

sealed class ScriptContext {
    object None : ScriptContext()
    data class SingleParameter(val parameter: GameParameterPointer) : ScriptContext()
    data class PlayerOnly(val player: PlayerDescription) : ScriptContext()
    data class Full(val player: PlayerDescription) : ScriptContext()
}

data class ActionButton(val visibleName: String, val functionName: Option<String>, val script: String, val context: ScriptContext) :
    Serializable {
    companion object
}