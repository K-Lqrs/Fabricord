package net.ririfa.fabricord.utils

import com.google.gson.*
import java.lang.reflect.Type
import java.util.UUID

class UUIDTypeAdapter : JsonSerializer<UUID>, JsonDeserializer<UUID> {
	override fun serialize(src: UUID?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
		return JsonPrimitive(src?.toString())
	}

	override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): UUID {
		return UUID.fromString(json?.asString)
	}
}