package com.chattriggers.website.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.UUIDTable
import java.util.*

object Releases : UUIDTable() {
    val module = reference("module_id", Modules)
    val releaseVersion = varchar("release_version", 20)
    val modVersion = varchar("mod_version", 20)
    val changelog = text("changelog")
    val downloads = integer("downloads").default(0)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class Release(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Release>(Releases)

    val module by Module referencedOn Releases.module
    val releaseVersion by Releases.releaseVersion
    val modVersion by Releases.modVersion
    val changelog by Releases.changelog
    val downloads by Releases.downloads
    val createdAt by Releases.createdAt
    val updatedAt by Releases.updatedAt
}