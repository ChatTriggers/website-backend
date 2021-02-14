package com.chattriggers.website.data

import com.chattriggers.website.Auth
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Users : IntIdTable() {
    val name = varchar("name", length = 191)
    val email = varchar("email", length = 191)
    val password = varchar("password", length = 191)
    val rank = enumerationByName("rank", 10, Auth.Roles::class)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var name by Users.name
    var email by Users.email
    var password by Users.password
    var rank by Users.rank
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt
    val modules by Module referrersOn Modules.owner

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is User) return false

        return other.name == this.name && other.email == this.email
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + email.hashCode()
        return result
    }

    fun public() = PublicUser(id.value, name, rank)

    fun personal() = PersonalPublicUser(id.value, name, rank, email)
}

data class PublicUser(val id: Int, val name: String, val rank: Auth.Roles)

data class PersonalPublicUser(val id: Int, val name: String, val rank: Auth.Roles, val email: String)
