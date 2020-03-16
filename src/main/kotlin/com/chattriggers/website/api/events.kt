package com.chattriggers.website.api

import com.chattriggers.website.data.PublicModule
import com.chattriggers.website.data.PublicRelease
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.websocket.WsContext
import io.javalin.websocket.WsHandler

fun eventRoutes() {
    ws("events", EventHandler::configure)
}

object EventHandler {
    private val listeners = mutableListOf<WsContext>()

    fun configure(handler: WsHandler) {
        handler.onConnect { ctx ->
            // The number 100 is arbitrary, can be modified as needed
            if (listeners.size > 100) {
                ctx.session.close(503, "Too many clients already connected...")
                return@onConnect
            }

            // We never want to time out the listener...
            ctx.session.idleTimeout = -1
            listeners.add(ctx)
        }

        handler.onClose { ctx ->
            listeners.remove(ctx)
        }
    }

    fun postEvent(message: Event) {
        listeners.removeIf { !it.session.isOpen }

        listeners.forEach { ctx ->
            ctx.send(message)
        }
    }
}

sealed class Event(val type: String) {
    class ModuleCreated(val module: PublicModule) : Event("module_created")
    class ModuleDeleted(val module: PublicModule) : Event("module_deleted")
    class ReleaseCreated(val release: PublicRelease): Event("release_created")
}