package com.chattriggers.website

import com.chattriggers.website.api.METADATA_NAME
import com.chattriggers.website.api.SCRIPTS_NAME
import com.chattriggers.website.api.saveModuleToFolder
import com.chattriggers.website.api.voidTransaction
import com.chattriggers.website.data.DB
import com.chattriggers.website.data.Module
import com.chattriggers.website.data.Release
import com.overzealous.remark.Remark
import io.javalin.http.BadRequestResponse
import io.javalin.http.UploadedFile
import org.joda.time.DateTime
import org.koin.core.context.startKoin
import java.io.File
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

fun main() {
    startKoin {
        modules(listOf(configModule))
    }

    DB.setupDB()

    voidTransaction {
        Module.all().forEach {
            println("Create release for ${it.name}")

            val changelog = "### Initial Release" +
                    "" +
                    "_Note: This release was automatically created from the pre-existing script files, and was assumed " +
                    "to be for mod version 0.18.4._"

            val oldFolder = File("/var/www/laravel/public/storage/zips/${it.name}/")

            println("\tOld folder: ${oldFolder.absolutePath}")

            val release = Release.new {
                this.module = it
                this.releaseVersion = "1.0.0"
                this.modVersion = "0.18.4"
                this.changelog = changelog
                this.createdAt = DateTime(oldFolder.lastModified())
                this.updatedAt = DateTime(oldFolder.lastModified())
            }

            val newFolder = File("storage/${it.name.toLowerCase()}/${release.id.value}/")

            println("\tNew folder: ${newFolder.absolutePath}\n")

            try {
                val zipToSave = File(newFolder, SCRIPTS_NAME)
                zipToSave.writeBytes(File(oldFolder, "scripts.zip").readBytes())

                try {
                    ZipFile(zipToSave).close()
                } catch (e: Exception) {
//                    zipToSave.delete()
                    throw BadRequestResponse("Module is not a valid zip file!")
                }

                Files.copy(File(oldFolder, "metadata.json").inputStream(), File(newFolder, METADATA_NAME).toPath())

            } catch (e: Exception) {
//                release.delete()
//                folder.deleteRecursively()
                throw e
            }

        }

//        Module.all().forEach {
//            println("Doing ${it.name}")
//            it.description = convertHTML(it.description)
//        }
    }
}

val remark = Remark()

fun convertHTML(html: String): String {
    return remark.convertFragment(html)
}