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
}

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    val name by Users.name
    val email by Users.email
    val password by Users.password
    val rank by Users.rank
    val modules by Module referrersOn Modules.owner

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is User) return false

        return other.name == this.name && other.email == this.email
    }

    fun public() = PublicUser(id.value, name)
}

data class PublicUser(val id: Int, val name: String)