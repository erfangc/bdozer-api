package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.stockanalysis.models.ZacksDerivedAnalytics

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
    
    fun generateTags(zacksDerivedAnalytics: ZacksDerivedAnalytics): List<String>{
        
        val tags = mutableListOf<Tag>()

        if (zacksDerivedAnalytics.earnings.isIncreasing) {
            tags.add(Tag.EARNINGS_IMPROVING)
        }

        if ((zacksDerivedAnalytics.latestMetrics.netIncome ?: 0.0) > 0.0) {
            tags.add(Tag.POSITIVE_EARNINGS)
        }

        if (zacksDerivedAnalytics.sales.isIncreasing) {
            tags.add(Tag.REVENUE_GROWING)
        }

        val latestMetrics = zacksDerivedAnalytics.latestMetrics
        val perShareMetrics = zacksDerivedAnalytics.perShareMetrics
        val price = zacksDerivedAnalytics.price
        val bookValPerShare = perShareMetrics.bookValPerShare

        if ((latestMetrics.debtToAsset ?: 0.0) > 0.5) {
            tags.add(Tag.HIGHLY_LEVERED)
        }

        if (bookValPerShare != null && price != null) {
            if (bookValPerShare > price) {
                tags.add(Tag.BELOW_BOOK_VALUE)
            }
        }
        
        return tags.map { it.name }
    }
}