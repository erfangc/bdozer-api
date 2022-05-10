package co.bdozer.libraries.master

import co.bdozer.libraries.master.models.CompanyMasterRecord

class TagGenerator {
    
    enum class Tag {
        EARNINGS_IMPROVING,
        REVENUE_GROWING,
        POSITIVE_EARNINGS,
        HIGHLY_LEVERED,
        BELOW_BOOK_VALUE,
        HIGH_GROSS_MARGIN,
        HIGH_NET_MARGIN,
        FUNDAMENTALS_STABLE,
        RECENTLY_CRASHED,
    }
    
    fun generateTags(companyMasterRecord: CompanyMasterRecord): List<String>{
        
        val tags = mutableListOf<Tag>()

        if (companyMasterRecord.earnings.isIncreasing) {
            tags.add(Tag.EARNINGS_IMPROVING)
        }

        if ((companyMasterRecord.latestMetrics.netIncome ?: 0.0) > 0.0) {
            tags.add(Tag.POSITIVE_EARNINGS)
        }

        if (companyMasterRecord.sales.isIncreasing) {
            tags.add(Tag.REVENUE_GROWING)
        }

        val latestMetrics = companyMasterRecord.latestMetrics
        val perShareMetrics = companyMasterRecord.perShareMetrics
        val price = companyMasterRecord.price
        
        if ((latestMetrics.debtToAsset ?: 0.0) > 0.5) {
            tags.add(Tag.HIGHLY_LEVERED)
        }

        if ((perShareMetrics.bookValPerShare?: 0.0) / (price ?: 0.0) < 1.0) {
            tags.add(Tag.BELOW_BOOK_VALUE)
        }

        return tags.map { it.name }
    }
}