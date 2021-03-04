package com.starburst.starburst.xbrl

import com.starburst.starburst.xbrl.factbase.XBRLFactParser
import org.junit.jupiter.api.Test

internal class FilingProviderImplTest {

    @Test
    fun getMetaLink() {
        val impl = FilingProviderImpl(
            cik = "0000886982",
            adsh = "0001193125-20-282987"
        )
        impl.metaLink
        val facts = XBRLFactParser(impl).parseFacts()
        facts
    }
}
