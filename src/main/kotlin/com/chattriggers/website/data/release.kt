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

    var module by Module referencedOn Releases.module
    var releaseVersion by Releases.releaseVersion
    var modVersion by Releases.modVersion
    var changelog by Releases.changelog
    var downloads by Releases.downloads
    var createdAt by Releases.createdAt
    var updatedAt by Releases.updatedAt

    fun public() = PublicRelease(id.value, releaseVersion, modVersion, changelog, downloads)
}

data class PublicRelease(
    val id: UUID,
    val releaseVersion: String,
    val modVersion: String,
    val changelog: String,
    val downloads: Int
)