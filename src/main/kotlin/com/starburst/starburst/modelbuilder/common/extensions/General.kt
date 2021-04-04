package com.starburst.starburst.modelbuilder.common.extensions

import java.net.URI

object General {
    fun String.fragment(): String = URI(this).fragment
    fun conceptNotFound(href: String): Nothing = throw IllegalStateException("concept $href not found")
}