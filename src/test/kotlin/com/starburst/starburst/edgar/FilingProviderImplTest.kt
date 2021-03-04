package com.starburst.starburst.edgar

import com.starburst.starburst.edgar.factbase.FaceBaseFilingParser
import com.starburst.starburst.edgar.provider.FilingProviderImpl
import org.junit.jupiter.api.Test

internal class FilingProviderImplTest {

    @Test
    fun getMetaLink() {
        val impl = FilingProviderImpl(
            cik = "0000886982",
            adsh = "0001193125-20-282987"
        )
        val facts = FaceBaseFilingParser(impl).parseFacts()
    }
}
