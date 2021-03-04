package com.starburst.starburst.xbrl

import org.junit.jupiter.api.Test

internal class FilingProviderImplTest {

    @Test
    fun getMetaLink() {
        val impl = FilingProviderImpl(
            cik = "0000886982",
            adsh = "0001193125-21-049380"
        )
        impl.metaLink
    }
}
