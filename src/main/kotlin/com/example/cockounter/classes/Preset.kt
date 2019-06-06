package com.example.cockounter.classes

//import androidx.room.*
import arrow.core.*
import com.google.gson.*
import java.io.Serializable
import java.lang.reflect.Type

//@Entity
//@TypeConverters(PresetConverter::class)
data class PresetInfo(
    val id: Int,
    val name: String,
    val description: String,
    val preset: Preset
) : Serializable

data class Preset(
    val globalParameters: Map<String, Parameter>,
    val roles: Map<String, Role>,
    val actionButtons: List<ActionButtonModel>,
    val libraries: List<Library>
) :
    Serializable

class InterfaceAdapter<T: Any>: JsonSerializer<T>, JsonDeserializer<T> {
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

class PresetConverter {
    companion object {
        val gson = GsonBuilder().registerTypeAdapter(Parameter::class.java, InterfaceAdapter<Parameter>()).create()
    }

    fun fromPreset(preset: Preset): String =
        gson.toJson(preset)

    fun fromPresetInfo(presetInfo: PresetInfo): String =
        gson.toJson(presetInfo)

    fun toPreset(data: String): Preset =
        gson.fromJson(data, Preset::class.java)

    fun toPresetInfo(data: String): PresetInfo =
        gson.fromJson(data, PresetInfo::class.java)

}

data class Role(
    val name: String,
    val sharedParameters: Map<String, Parameter>,
    val privateParameters: Map<String, Parameter>
) : Serializable

sealed class Parameter : Serializable {
    abstract val name: String
    abstract val visibleName: String
    abstract fun initialValueString(): String
    abstract fun typeString(): String
}

data class IntegerParameter(
    override val name: String, override val visibleName: String, val initialValue: Int
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
    val initialValue: Double
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
    val initialValue: String
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
    val initialValue: Boolean
) :
    Parameter(), Serializable {
    override fun typeString(): String = typeName

    companion object {
        const val typeName: String = "Boolean"
    }

    override fun initialValueString(): String = initialValue.toString()
}

//TODO sealed class maybe
data class Library(val name: String, val script: String)

operator fun Preset.get(rolePointer: RolePointer): Role = roles.getValue(rolePointer.role)

operator fun Preset.get(parameterPointer: ParameterPointer): Parameter = when(parameterPointer) {
    is ParameterPointer.Global -> globalParameters.getValue(parameterPointer.name)
    is ParameterPointer.Shared -> this[parameterPointer.rolePointer].sharedParameters.getValue(parameterPointer.name)
    is ParameterPointer.Private -> this[parameterPointer.rolePointer].privateParameters.getValue(parameterPointer.name)
}

data class RolePointer(val role: String)

sealed class ParameterPointer {
    data class Global(val name: String) : ParameterPointer()
    data class Shared(val rolePointer: RolePointer, val name: String) : ParameterPointer()
    data class Private(val rolePointer: RolePointer, val name: String) : ParameterPointer()
}

data class PresetScript(val visibleName: String, val functionName: Option<String>, val script: String, val context: ScriptContextDescription) : Serializable

fun Preset.globalParameterPointers(): List<ParameterPointer> = globalParameters.values.map { ParameterPointer.Global(it.name) }
fun Role.sharedParameterPointers(): List<ParameterPointer> = sharedParameters.values.map { ParameterPointer.Shared(RolePointer(name), it.name) }
fun Role.privateParameterPointers(): List<ParameterPointer> = privateParameters.values.map { ParameterPointer.Shared(RolePointer(name), it.name) }


enum class ScriptContextDescription {
    NONE, SINGLE_PARAMETER, PLAYER_ONLY, ALL
}

sealed class ActionButtonModel : Serializable {
    data class Global(val script: PresetScript) : ActionButtonModel()
    data class Role(val rolePointer: RolePointer, val script: PresetScript) : ActionButtonModel()
    data class Attached(val parameterPointer: ParameterPointer, val script: PresetScript) : ActionButtonModel()
}

/*fun toParameter(x: Any, name: String, visibleName: String, defaultValue: String): Either<String, Parameter> =
    when (x.toString()) {
        IntegerParameter.typeName -> defaultValue.toIntOrNull().toOption().fold(
            { Left("$defaultValue is not an integer") },
            { Right(IntegerParameter(name = name, visibleName = visibleName, initialValue = it)) })
        StringParameter.typeName -> Right(StringParameter(
            name = name,
            visibleName = visibleName,
            initialValue = defaultValue
        ))
        DoubleParameter.typeName -> defaultValue.toDoubleOrNull().toOption().fold(
            { Left("$defaultValue is not a double") },
            { Right(DoubleParameter(name = name, visibleName = visibleName, initialValue = it)) })
        BooleanParameter.typeName -> Right(BooleanParameter(
            name = name,
            visibleName = visibleName,
            initialValue = defaultValue.toBoolean()
        ))
        else -> Left("Unknown type")
    }*/

