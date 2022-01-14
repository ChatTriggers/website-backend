package com.chattriggers.website.data

import org.jetbrains.exposed.dao.*

object TrackedUsers : IntIdTable() {
    val hash = varchar("hash", length = 100)
}

class TrackedUser(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TrackedUser>(TrackedUsers)

    var hash by TrackedUsers.hash

    override fun equals(other: Any?): Boolean {
        return other is TrackedUser && hash == other.hash
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }
}
