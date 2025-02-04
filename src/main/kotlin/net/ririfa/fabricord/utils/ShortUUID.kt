package net.ririfa.fabricord.utils

import java.util.*

/**
 * A utility class for handling UUIDs and their shorter string representations.
 * This class provides methods to generate, convert, and validate UUIDs and their short string forms.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class ShortUUID private constructor(
	private val uuid: UUID?,
	private val shortString: String?
) : Comparable<ShortUUID> {

	companion object {
		/**
		 * Generates a new ShortUUID with a random UUID.
		 * @return A new ShortUUID instance.
		 */
		fun randomUUID(): ShortUUID {
			val uuid = UUID.randomUUID()
			val shortUUID = ShortUUID(uuid, null).toShortString()
			return ShortUUID(uuid, shortUUID)
		}

		/**
		 * Creates a ShortUUID from a standard UUID string.
		 * @param uuid The UUID string.
		 * @return A new ShortUUID instance.
		 */
		fun fromString(uuid: String): ShortUUID {
			return ShortUUID(UUID.fromString(uuid), null)
		}

		/**
		 * Creates a ShortUUID from a short string representation.
		 * @param shortString The short string representation of the UUID.
		 * @return A new ShortUUID instance.
		 */
		fun fromShortString(shortString: String): ShortUUID {
			return ShortUUID(null, shortString)
		}

		/**
		 * Validates if a given short string is a valid short UUID.
		 * @param shortString The short string to validate.
		 * @return True if the short string is valid, false otherwise.
		 */
		fun isValidShortString(shortString: String): Boolean {
			return try {
				val bytes = Base64.getUrlDecoder().decode(shortString)
				bytes.size == 16
			} catch (e: IllegalArgumentException) {
				false
			}
		}

		/**
		 * Creates a ShortUUID from a byte array.
		 * @param byteArray The byte array representing the UUID.
		 * @return A new ShortUUID instance.
		 */
		fun fromByteArray(byteArray: ByteArray): ShortUUID {
			val bb = java.nio.ByteBuffer.wrap(byteArray)
			val high = bb.long
			val low = bb.long
			return ShortUUID(UUID(high, low))
		}

		/**
		 * Creates a ShortUUID from various types of input.
		 * @param value The input value, which can be a String, UUID, or ShortUUID.
		 * @return A new ShortUUID instance or null if the input type is not supported.
		 */
		fun fromAny(value: Any): ShortUUID? {
			return when (value) {
				is String -> if (isValidShortString(value)) fromShortString(value) else fromString(value)
				is UUID -> ShortUUID(value, null)
				is ShortUUID -> value
				else -> null
			}
		}
	}

	private constructor(uuid: UUID) : this(uuid, null)

	/**
	 * Converts the ShortUUID to a standard UUID.
	 * @return The UUID or null if conversion is not possible.
	 */
	fun toUUID(): UUID? {
		if (uuid != null) {
			return uuid
		}
		return shortString?.let {
			val bytes = Base64.getUrlDecoder().decode(it)
			val bb = java.nio.ByteBuffer.wrap(bytes)
			val high = bb.long
			val low = bb.long
			return UUID(high, low)
		}
	}

	/**
	 * Converts the ShortUUID to its short string representation.
	 * @return The short string representation of the UUID.
	 */
	fun toShortString(): String {
		if (shortString != null) {
			return shortString
		}
		val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
		bb.putLong(uuid!!.mostSignificantBits)
		bb.putLong(uuid.leastSignificantBits)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bb.array())
	}

	/**
	 * Converts the ShortUUID to a byte array.
	 * @return The byte array representation of the UUID.
	 */
	fun toByteArray(): ByteArray {
		val bb = java.nio.ByteBuffer.wrap(ByteArray(16))
		bb.putLong(uuid!!.mostSignificantBits)
		bb.putLong(uuid.leastSignificantBits)
		return bb.array()
	}

	/**
	 * Returns the string representation of the ShortUUID.
	 * @return The string representation of the UUID or short string.
	 */
	override fun toString(): String {
		return uuid?.toString() ?: shortString ?: ""
	}

	/**
	 * Checks if this ShortUUID is equal to another object.
	 * @param other The object to compare with.
	 * @return True if the objects are equal, false otherwise.
	 */
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is ShortUUID) return false

		return this.toUUID() == other.toUUID()
	}

	/**
	 * Returns the hash code of the ShortUUID.
	 * @return The hash code.
	 */
	override fun hashCode(): Int {
		return toUUID()?.hashCode() ?: shortString.hashCode()
	}

	/**
	 * Compares this ShortUUID with another ShortUUID.
	 * @param other The other ShortUUID to compare with.
	 * @return A negative integer, zero, or a positive integer as this ShortUUID is less than,
	 * equal to, or greater than the specified ShortUUID.
	 */
	override fun compareTo(other: ShortUUID): Int {
		return this.toUUID()?.compareTo(other.toUUID()) ?: 0
	}
}