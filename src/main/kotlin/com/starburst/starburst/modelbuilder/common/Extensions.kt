package com.starburst.starburst.modelbuilder.common

import java.net.URI

object Extensions {
    fun String.fragment(): String = URI(this).fragment
}