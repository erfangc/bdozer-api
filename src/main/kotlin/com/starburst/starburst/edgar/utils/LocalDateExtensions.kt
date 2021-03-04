package com.starburst.starburst.edgar.utils

import org.w3c.dom.Node
import java.time.LocalDate

object LocalDateExtensions {
    fun Node.toLocalDate(): LocalDate? {
        return LocalDate.parse(this.textContent)
    }
}
