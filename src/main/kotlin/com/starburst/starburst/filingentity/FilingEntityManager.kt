package com.starburst.starburst.filingentity

import com.mongodb.client.MongoDatabase
import com.starburst.starburst.edgar.factbase.FactBase
import com.starburst.starburst.filingentity.dataclasses.Address
import com.starburst.starburst.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.filingentity.internal.SECEntity
import com.starburst.starburst.xml.HttpClientExtensions.readEntity
import com.starburst.starburst.zacks.modelbuilder.ZacksModelBuilder
import org.apache.http.client.HttpClient
import org.litote.kmongo.eq
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.Executors

@Service
class FilingEntityManager(
    mongoDatabase: MongoDatabase,
    private val factBase: FactBase,
    private val zacksModelBuilder: ZacksModelBuilder,
    private val httpClient: HttpClient,
) {

    private val log = LoggerFactory.getLogger(FilingEntityManager::class.java)
    private val executor = Executors.newCachedThreadPool()
    private val col = mongoDatabase.getCollection<FilingEntity>()

    fun getFilingEntity(cik: String): FilingEntity {
        val savedEntity = col.findOneById(cik)
        return savedEntity ?: bootstrapFilingEntity(cik)
    }

    fun bootstrapFilingEntity(cik: String): FilingEntity {
        deleteFilingEntity(cik)

        val paddedCik = (0 until (10 - cik.length)).joinToString("") { "0" } + cik
        val secEntity = httpClient.readEntity<SECEntity>("https://data.sec.gov/submissions/CIK$paddedCik.json")

        val entity = FilingEntity(
            _id = cik,
            cik = secEntity.cik,
            entityType = secEntity.entityType,
            sic = secEntity.sic,
            sicDescription = secEntity.sicDescription,
            insiderTransactionForOwnerExists = secEntity.insiderTransactionForOwnerExists,
            insiderTransactionForIssuerExists = secEntity.insiderTransactionForIssuerExists,
            name = secEntity.name ?: "Unknown",
            tickers = secEntity.tickers,
            tradingSymbol = secEntity.tickers.firstOrNull(),
            exchanges = secEntity.exchanges,
            ein = secEntity.ein,
            description = secEntity.description,
            website = secEntity.website,
            investorWebsite = secEntity.investorWebsite,
            category = secEntity.category,
            fiscalYearEnd = secEntity.fiscalYearEnd,
            stateOfIncorporation = secEntity.stateOfIncorporation,
            stateOfIncorporationDescription = secEntity.stateOfIncorporationDescription,
            businessAddress = Address(
                street1 = secEntity.addresses?.business?.street1,
                street2 = secEntity.addresses?.business?.street2,
                city = secEntity.addresses?.business?.city,
                stateOrCountry = secEntity.addresses?.business?.stateOrCountry,
                zipCode = secEntity.addresses?.business?.zipCode,
                stateOrCountryDescription = secEntity.addresses?.business?.stateOrCountryDescription,
            ),
            phone = secEntity.phone,
            lastUpdated = Instant.now().toString(),
            statusMessage = "Analysis for this entity is underway, we are parsing the internet for data",
        )
        col.save(entity)
        executor.execute {
            try {
                factBase.bootstrapFacts(cik)
                /*
                after the facts are ingested, attempt to build a model using the Zacks model builder
                 */
                val response =
                    zacksModelBuilder.buildModel(entity.tradingSymbol ?: error("no trading symbol found for $cik"))

                val updatedEntity = entity.copy(
                    lastUpdated = Instant.now().toString(),
                    statusMessage = "Completed",
                    model = response.evaluateModelResult.model,
                )

                col.save(updatedEntity)
                log.info("Completed bootstrapping and initial model building cik=${entity.cik}")
            } catch (e: Exception) {
                log.error("Unable to complete bootstrapping and initial model building cik=${entity.cik}", e)
                col.save(entity.copy(lastUpdated = Instant.now().toString(), statusMessage = e.message))
            }
        }

        return entity
    }

    private fun deleteFilingEntity(cik: String) {
        /*
        delete any existing data on this entity
         */
        col.deleteMany(FilingEntity::cik eq cik)
        factBase.deleteAll(cik)
    }
}
