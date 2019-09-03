package com.chattriggers.website

import com.chattriggers.website.api.voidTransaction
import com.chattriggers.website.data.DB
import com.chattriggers.website.data.Module
import com.overzealous.remark.Remark
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(listOf(configModule))
    }

    DB.setupDB()

    voidTransaction {
        Module.all().forEach {
            println("Doing ${it.name}")
            it.description = convertHTML(it.description)
        }
    }
}

val remark = Remark()

fun convertHTML(html: String): String {
    return remark.convertFragment(html)
}