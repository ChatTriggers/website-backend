package com.chattriggers.website.data

import org.jetbrains.exposed.dao.*
import java.util.*

object TrackedUsers : IntIdTable() {
    val hash = varchar("hash", length = 100)
    val version = varchar("version", length = 20)
}

class TrackedUser(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TrackedUser>(TrackedUsers)

    var hash by TrackedUsers.hash
    var version by TrackedUsers.version

    override fun equals(other: Any?): Boolean {
        return other is TrackedUser && hash == other.hash && version == other.version
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, version)
    }
}
