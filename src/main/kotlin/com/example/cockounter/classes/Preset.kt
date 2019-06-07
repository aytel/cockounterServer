package com.example.cockounter.classes

//import androidx.lifecycle.LiveData
//import androidx.room.*
import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import arrow.core.toOption
import com.google.gson.*
import java.io.Serializable
import java.lang.reflect.Type

//@Entity
//@TypeConverters(PresetConverter::class)
data class PresetInfo(
    //@PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val description: String,
    val preset: Preset
) : Serializable

data class Preset(
    val globalParameters: Map<String, Parameter> = emptyMap(),
    val roles: Map<String, Role> = emptyMap(),
    val libraries: List<Library> = emptyList(),
    val actionsStubs: List<PresetScript> = emptyList()
) : Serializable {
    val actionButtons: List<ActionButtonModel>
        get() = buildActionButtons()
}

/*@Dao
interface PresetInfoDao {
    @Query("SELECT * FROM presetInfo")
    fun getAll(): LiveData<List<PresetInfo>>

    @Query("SELECT * FROM presetInfo WHERE id = :id")
    fun getById(id: Int): PresetInfo

    @Insert
    fun insert(preset: PresetInfo)

    @Delete
    fun delete(preset: PresetInfo)

    @Query("DELETE FROM presetInfo")
    fun nukeTable()
}*/

class InterfaceAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T> {
    override fun serialize(src: T, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        val member = JsonObject()
        member.addProperty("type", src.javaClass.name)
        member.add("data", context!!.serialize(src))
        return member
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): T {
        val member = json as JsonObject
        val actualType = Class.forName(member.get("type").asString)
        return context!!.deserialize(member.get("data"), actualType)
    }

}

/*
class PresetConverter {
    companion object {
        val gson = GsonBuilder().registerTypeAdapter(Parameter::class.java, InterfaceAdapter<Parameter>()).create()
    }

    @TypeConverter
    fun fromPreset(preset: Preset): String =
        gson.toJson(preset)

    fun fromPresetInfo(presetInfo: PresetInfo): String =
        gson.toJson(presetInfo)

    @TypeConverter
    fun toPreset(data: String): Preset =
        gson.fromJson(data, Preset::class.java)

    fun toPresetInfo(data: String): PresetInfo =
        gson.fromJson(data, PresetInfo::class.java)

}
*/

data class Role(
    val name: String = "",
    val sharedParameters: Map<String, Parameter> = emptyMap(),
    val privateParameters: Map<String, Parameter> = emptyMap(),
    val actionsStubs: List<PresetScript> = emptyList()
) : Serializable {
    companion object
}

sealed class Parameter : Serializable {
    abstract val name: String
    abstract val visibleName: String
    abstract fun initialValueString(): String
    abstract fun typeString(): String
    abstract val actionsStubs: List<PresetScript>
    companion object
}

data class IntegerParameter(
    override val name: String,
    override val visibleName: String,
    val initialValue: Int,
    override val actionsStubs: List<PresetScript>
) :
    Parameter(), Serializable {
    override fun typeString(): String = typeName

    companion object {
        const val typeName: String = "Integer"
    }

    override fun initialValueString(): String = initialValue.toString()
}

data class DoubleParameter(
    override val name: String,
    override val visibleName: String,
    val initialValue: Double,
    override val actionsStubs: List<PresetScript>
) :
    Parameter(), Serializable {
    override fun typeString(): String = typeName

    companion object {
        const val typeName: String = "Double"
    }

    override fun initialValueString(): String = initialValue.toString()
}

data class StringParameter(
    override val name: String,
    override val visibleName: String,
    val initialValue: String,
    override val actionsStubs: List<PresetScript>
) :
    Parameter(), Serializable {
    override fun typeString(): String = typeName

    companion object {
        const val typeName: String = "String"
    }

    override fun initialValueString(): String = initialValue
}

data class BooleanParameter(
    override val name: String,
    override val visibleName: String,
    val initialValue: Boolean,
    override val actionsStubs: List<PresetScript>
) :
    Parameter(), Serializable {
    override fun typeString(): String = typeName

    companion object {
        const val typeName: String = "Boolean"
    }

    override fun initialValueString(): String = initialValue.toString()
}

data class Library(val name: String, val script: String) : Serializable {
    companion object
}

operator fun Preset.get(rolePointer: RolePointer): Role = roles.getValue(rolePointer.role)

operator fun Preset.get(parameterPointer: ParameterPointer): Parameter = when (parameterPointer) {
    is ParameterPointer.Global -> globalParameters.getValue(parameterPointer.name)
    is ParameterPointer.Shared -> this[parameterPointer.rolePointer].sharedParameters.getValue(parameterPointer.name)
    is ParameterPointer.Private -> this[parameterPointer.rolePointer].privateParameters.getValue(parameterPointer.name)
}

data class RolePointer(val role: String) : Serializable

sealed class ParameterPointer : Serializable {
    data class Global(val name: String) : ParameterPointer()
    data class Shared(val rolePointer: RolePointer, val name: String) : ParameterPointer()
    data class Private(val rolePointer: RolePointer, val name: String) : ParameterPointer()
}

data class PresetScript(
    val visibleName: String,
    val functionName: String?,
    val script: String,
    val context: ScriptContextDescription
) : Serializable {
    companion object
}

fun Preset.globalParameterPointers(): List<ParameterPointer> =
    globalParameters.values.map { ParameterPointer.Global(it.name) }

fun Role.sharedParameterPointers(): List<ParameterPointer> =
    sharedParameters.values.map { ParameterPointer.Shared(RolePointer(name), it.name) }

fun Role.privateParameterPointers(): List<ParameterPointer> =
    privateParameters.values.map { ParameterPointer.Shared(RolePointer(name), it.name) }


enum class ScriptContextDescription : Serializable {
    NONE, SINGLE_PARAMETER, PLAYER_ONLY, ALL
}

sealed class ActionButtonModel : Serializable {
    abstract val script: PresetScript
    data class Global(override val script: PresetScript) : ActionButtonModel()
    data class Role(val rolePointer: RolePointer, override val script: PresetScript) : ActionButtonModel()
    data class Attached(val parameterPointer: ParameterPointer, override val script: PresetScript) : ActionButtonModel()
}

fun Preset.buildActionButtons(): List<ActionButtonModel> {
    val global = actionsStubs.map { ActionButtonModel.Global(it) }
    val shared =
        roles.values.flatMap { role -> role.actionsStubs.map { ActionButtonModel.Role(RolePointer(role.name), it) } }
    val attachedGlobal = globalParameters.values.flatMap { parameter ->
        parameter.actionsStubs.map {
            ActionButtonModel.Attached(ParameterPointer.Global(parameter.name), it)
        }
    }
    val attachedShared = roles.values.flatMap { role ->
        role.sharedParameters.values.flatMap { parameter ->
            parameter.actionsStubs.map { ActionButtonModel.Attached(ParameterPointer.Shared(RolePointer(role.name), parameter.name), it) }
        }
    }
    val attachedPrivate = roles.values.flatMap { role ->
        role.privateParameters.values.flatMap { parameter ->
            parameter.actionsStubs.map { ActionButtonModel.Attached(ParameterPointer.Private(RolePointer(role.name), parameter.name), it) }
        }
    }
    return global + shared + attachedGlobal + attachedPrivate + attachedShared
}

enum class ParameterType {
    INTEGER {
        override fun toString(): String = "Integer"
    },
    STRING {
        override fun toString(): String = "String"
    },
    DOUBLE {
        override fun toString(): String = "Fractional"
    },
    BOOLEAN {
        override fun toString(): String = "true/false"
    }
}

fun toParameter(
    type: ParameterType,
    name: String,
    visibleName: String,
    defaultValue: String,
    actionsStubs: List<PresetScript>
): Either<String, Parameter> =
    when (type) {
        ParameterType.INTEGER -> defaultValue.toIntOrNull().toOption().fold(
            { Left("$defaultValue is not an integer") },
            {
                Right(
                    IntegerParameter(
                        name = name,
                        visibleName = visibleName,
                        initialValue = it,
                        actionsStubs = actionsStubs
                    )
                )
            })
        ParameterType.STRING -> Right(
            StringParameter(
                name = name,
                visibleName = visibleName,
                initialValue = defaultValue,
                actionsStubs = actionsStubs
            )
        )
        ParameterType.DOUBLE -> defaultValue.toDoubleOrNull().toOption().fold(
            { Left("$defaultValue is not a double") },
            {
                Right(
                    DoubleParameter(
                        name = name,
                        visibleName = visibleName,
                        initialValue = it,
                        actionsStubs = actionsStubs
                    )
                )
            })
        ParameterType.BOOLEAN -> Right(
            BooleanParameter(
                name = name,
                visibleName = visibleName,
                initialValue = defaultValue.toBoolean(),
                actionsStubs = actionsStubs
            )
        )
    }

