package com.starburst.starburst.modelbuilder.common

import java.net.URI

object GeneralExtensions {
    fun String.fragment(): String = URI(this).fragment
    fun conceptNotFound(href: String): Nothing = throw IllegalStateException("concept $href not found")
}