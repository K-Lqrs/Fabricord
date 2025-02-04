package net.ririfa.fabricord.utils

import com.google.gson.*
import java.lang.reflect.Type

class ShortUUIDTypeAdapter : JsonSerializer<ShortUUID>, JsonDeserializer<ShortUUID> {
	override fun serialize(src: ShortUUID?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
		// ShortUUID → 文字列
		return JsonPrimitive(src?.toShortString())
	}

	override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ShortUUID {
		val str = json?.asString ?: throw JsonParseException("ShortUUID string is null")
		return if (ShortUUID.isValidShortString(str)) {
			ShortUUID.fromShortString(str)
		} else {
			ShortUUID.fromString(str)
		}
	}
}