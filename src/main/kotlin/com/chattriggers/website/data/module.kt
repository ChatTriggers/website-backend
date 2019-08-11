package com.chattriggers.website.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Modules : IntIdTable() {
    val owner = reference("user_id", Users)
    val name = varchar("name", 20)
    val description = text("description")
    val image = varchar("image", 50).nullable()
    val downloads = integer("downloads").default(0)
    val hidden = bool("hidden")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class Module(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Module>(Modules)

    val owner by User referencedOn Modules.owner
    val name by Modules.name
    val description by Modules.description
    val image by Modules.image
    val downloads by Modules.downloads
    val hidden by Modules.hidden
    val createdAt by Modules.createdAt
    val updatedAt by Modules.updatedAt

    fun public() = PublicModule(
        id.value,
        owner.public(),
        name,
        description,
        image,
        downloads
    )
}

data class PublicModule (
    val id: Int,
    val owner: PublicUser,
    val name: String,
    val description: String,
    val image: String?,
    val downloads: Int
)