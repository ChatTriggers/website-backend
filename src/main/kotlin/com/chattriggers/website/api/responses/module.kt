package com.chattriggers.website.api.responses

import com.chattriggers.website.data.PublicModule

data class ModuleResponse(val meta: ModuleMeta, val modules: List<PublicModule>)

data class ModuleMeta(val limit: Int, val offset: Int, val total: Int)