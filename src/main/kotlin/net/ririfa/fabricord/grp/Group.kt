package net.ririfa.fabricord.grp

import net.ririfa.fabricord.utils.ShortUUID
import java.util.UUID

data class Group(
	val id: ShortUUID,
	val name: String,
	val owner: UUID,
	val members: MutableSet<UUID>,
	val open: Boolean,
	val joinRequests: MutableSet<UUID> = mutableSetOf()
)
