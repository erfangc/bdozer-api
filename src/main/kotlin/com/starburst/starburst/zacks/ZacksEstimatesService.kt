package com.starburst.starburst.zacks

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.vhl.blackmo.grass.dsl.grass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File

@ExperimentalStdlibApi
@Service
class ZacksEstimatesService {

    private val fileName = "/Users/erfangchen/Downloads/ZACKS-SE.csv"
    private val log = LoggerFactory.getLogger(ZacksEstimatesService::class.java)
    private val zacksSalesEstimates: List<ZacksSalesEstimates>

    init {
        val csvContents = csvReader().readAllWithHeader(File(fileName))
        zacksSalesEstimates = grass<ZacksSalesEstimates> { dateFormat = "M/d/yy" }.harvest(csvContents)
        log.info("Loaded ${zacksSalesEstimates.size} ${zacksSalesEstimates.javaClass.simpleName} from $fileName")
    }

    fun getZacksSaleEstimates(ticker: String): List<ZacksSalesEstimates> {
        return zacksSalesEstimates.filter { z -> z.ticker == ticker }
    }

}