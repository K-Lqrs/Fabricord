package net.rk4z.fabricord.util

object Utils {
    /**
     * Retrieves a nullable string value from the map associated with the given key.
     * Returns null if the value is null, or if the value is a blank string.
     *
     * @param key The key used to retrieve the value from the map.
     * @return The nullable string value associated with the given key, or null if the value is null or blank.
     */
    private fun Map<String, Any>.getNullableString(key: String): String? =
        this[key]?.toString()?.takeIf { it.isNotBlank() }
}