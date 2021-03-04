package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.factbase.XbrlFactParser
import org.junit.jupiter.api.Test

internal class FilingProviderImplTest {

    @Test
    fun getMetaLink() {
        val impl = FilingProviderImpl(
            cik = "0000886982",
            adsh = "0001193125-20-282987"
        )
        val facts = XbrlFactParser(impl).parseFacts()
    }
}
