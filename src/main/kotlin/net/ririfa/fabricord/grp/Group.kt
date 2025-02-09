package net.ririfa.fabricord.grp

import net.ririfa.fabricord.utils.ShortUUID
import java.util.UUID

data class Group(
	val id: ShortUUID,
	val name: String,
	var owner: UUID,
	val members: MutableSet<UUID>,
	val open: Boolean,
	val joinRequests: MutableSet<UUID> = mutableSetOf()
) {
	fun addMember(uuid: UUID) = members.add(uuid)
	fun removeMember(uuid: UUID) = members.remove(uuid)
	fun addJoinRequest(uuid: UUID) = joinRequests.add(uuid)
	fun removeJoinRequest(uuid: UUID) = joinRequests.remove(uuid)
	fun approveJoinRequest(uuid: UUID) {
		joinRequests.remove(uuid)
		members.add(uuid)
	}
	fun denyJoinRequest(uuid: UUID) = joinRequests.remove(uuid)
	fun open() = open
	fun close() = !open
	fun changeOwner(uuid: UUID) {
		owner = uuid
	}
}