package com.bdozer.api.web.stockanalysis.support.zacks

import com.bdozer.api.web.stockanalysis.models.ZacksDerivedAnalytics

class TagGenerator {

    fun generateTags(zacksDerivedAnalytics: ZacksDerivedAnalytics): List<ZacksDerivedTag>{
        
        val zacksDerivedTags = mutableListOf<ZacksDerivedTag>()

        if (zacksDerivedAnalytics.earnings.isIncreasing) {
            zacksDerivedTags.add(ZacksDerivedTag.EARNINGS_IMPROVING)
        }

        if ((zacksDerivedAnalytics.latestMetrics.netIncome ?: 0.0) > 0.0) {
            zacksDerivedTags.add(ZacksDerivedTag.POSITIVE_EARNINGS)
        }

        if (zacksDerivedAnalytics.sales.isIncreasing) {
            zacksDerivedTags.add(ZacksDerivedTag.REVENUE_GROWING)
        }

        val latestMetrics = zacksDerivedAnalytics.latestMetrics
        val perShareMetrics = zacksDerivedAnalytics.perShareMetrics
        val price = zacksDerivedAnalytics.price
        val bookValPerShare = perShareMetrics.bookValPerShare

        if ((latestMetrics.debtToAsset ?: 0.0) > 0.5) {
            zacksDerivedTags.add(ZacksDerivedTag.HIGHLY_LEVERED)
        }

        if (bookValPerShare != null && price != null) {
            if (bookValPerShare > price) {
                zacksDerivedTags.add(ZacksDerivedTag.BELOW_BOOK_VALUE)
            }
        }
        
        return zacksDerivedTags
    }
}