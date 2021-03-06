package com.starburst.starburst.edgar.factbase.filingentity

import com.mongodb.client.MongoClient
import com.starburst.starburst.edgar.bootstrapper.FilingEntityBootstrapper
import com.starburst.starburst.edgar.explorer.EdgarExplorer
import com.starburst.starburst.edgar.factbase.filingentity.dataclasses.Address
import com.starburst.starburst.edgar.factbase.filingentity.dataclasses.FilingEntity
import com.starburst.starburst.edgar.factbase.filingentity.internal.SECEntity
import com.starburst.starburst.edgar.factbase.modelbuilder.ModelBuilder
import com.starburst.starburst.edgar.utils.HttpClientExtensions.readEntity
import com.starburst.starburst.models.Model
import org.apache.http.client.HttpClient
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.Executors

@Service
class FilingEntityManager(
    mongoClient: MongoClient,
    private val bootstrapper: FilingEntityBootstrapper,
    private val httpClient: HttpClient,
    private val modelBuilder: ModelBuilder,
    private val edgarExplorer: EdgarExplorer
) {

    private val log = LoggerFactory.getLogger(FilingEntityManager::class.java)
    private val executor = Executors.newCachedThreadPool()
    private val col = mongoClient
        .getDatabase("starburst")
        .getCollection<FilingEntity>()

    fun getFilingEntity(cik: String): FilingEntity {
        val savedEntity = col.findOneById(cik)
        return savedEntity ?: // initiate the sequence of actions to build this entity
        bootstrapNewFilingEntity(cik)
    }

    fun viewLatest10kModel(cik: String): Model {
        val adsh = edgarExplorer
            .searchFilings(cik)
            .sortedByDescending { it.period_ending }
            .find { it.form == "10-K" }
            ?.adsh ?: error("no 10-K filings found for $cik")
        return modelBuilder.buildModelForFiling(cik, adsh)
    }

    fun rerunModel(cik: String): Model {
        val entity = getFilingEntity(cik)
        val model = viewLatest10kModel(cik)
        col.save(entity.copy(proFormaModel = model))
        return model
    }

    private fun bootstrapNewFilingEntity(cik: String): FilingEntity {

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
                bootstrapper.bootstrapFilingEntity(cik)
                val model = viewLatest10kModel(cik)

                val updated = entity.copy(
                    proFormaModel = model,
                    lastUpdated = Instant.now().toString(),
                    statusMessage = "Completed"
                )
                col.save(updated)

                log.error("Completed bootstrapping and initial model building cik=${entity.cik}")
            } catch (e: Exception) {
                log.error("Unable to complete bootstrapping and initial model building cik=${entity.cik}")
            }
        }

        return entity
    }
}

