package com.chattriggers.website.data

import com.chattriggers.website.api.toVersion
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Modules : IntIdTable() {
    val owner = reference("user_id", Users)
    val name = varchar("name", 64)
    val description = text("description")
    val image = varchar("image", 50).nullable()
    val downloads = integer("downloads").default(0)
    val tags = varchar("tags", 2000).default("")
    val hidden = bool("hidden")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class Module(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Module>(Modules)

    var owner by User referencedOn Modules.owner
    var name by Modules.name
    var description by Modules.description
    var image by Modules.image
    var downloads by Modules.downloads
    var tags by Modules.tags
    var hidden by Modules.hidden
    var createdAt by Modules.createdAt
    var updatedAt by Modules.updatedAt
    val releases by Release referrersOn Releases.module

    fun authorized(): PublicModule = AuthorizedModule(
        id.value,
        owner.public(),
        name,
        description,
        image,
        downloads,
        tags.split(",").filter { it.isNotBlank() },
        releases.map(Release::authorized),
        hidden
    )

    fun public() = PublicModule(
        id.value,
        owner.public(),
        name,
        description,
        image,
        downloads,
        tags.split(",").filter { it.isNotBlank() },
        releases.filter { it.verified }.sortedByDescending { it.releaseVersion.toVersion() }.distinctBy { it.modVersion }.map(Release::public)
    )
}

open class PublicModule(
    val id: Int,
    val owner: PublicUser,
    val name: String,
    val description: String,
    val image: String?,
    val downloads: Int,
    val tags: List<String>,
    val releases: List<PublicRelease>
)

class AuthorizedModule(
    id: Int, owner: PublicUser, name: String, description: String, image: String?,
    downloads: Int, tags: List<String>, releases: List<PublicRelease>, val flagged: Boolean
) : PublicModule(
    id, owner, name,
    description, image, downloads, tags, releases
)
