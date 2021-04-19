package com.bdozer.filingentity

import com.bdozer.sec.factbase.core.FactBase
import com.bdozer.filingentity.dataclasses.Address
import com.bdozer.filingentity.dataclasses.FilingEntity
import com.bdozer.filingentity.dataclasses.ModelTemplate
import com.bdozer.filingentity.internal.SECEntity
import com.bdozer.xml.HttpClientExtensions.readEntity
import org.apache.http.client.HttpClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.Executors

@Service
class FilingEntityBootstrapper(
    private val httpClient: HttpClient,
    private val filingEntityManager: FilingEntityManager,
    private val factBase: FactBase,
) {
    private val log = LoggerFactory.getLogger(FilingEntityManager::class.java)
    private val executor = Executors.newCachedThreadPool()

    /**
     * Create a filing entity in the system by using
     * SEC data (but do not parse the filings for facts)
     */
    fun createFilingEntity(cik: String): FilingEntity {
        val paddedCik = cik.padStart(10, '0')
        val secEntity = httpClient.readEntity<SECEntity>("https://data.sec.gov/submissions/CIK$paddedCik.json")
        val entity = FilingEntity(
            _id = paddedCik,
            cik = paddedCik,
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
            statusMessage = FilingEntityManager.Created,
            modelTemplate = ModelTemplate(
                name = "Normal",
                template = "Normal",
            ),
        )
        filingEntityManager.saveFilingEntity(entity)
        log.info("Created filing entity cik=${entity.cik}")
        return entity
    }

    /**
     * Bootstraps a filing entity by creating the entity,
     * saving it and then parsing & storing facts by crawling through the SEC's website
     */
    fun bootstrapFilingEntity(cik: String): FilingEntity {
        filingEntityManager.deleteFilingEntity(cik)
        val entity = createFilingEntity(cik).copy(statusMessage = FilingEntityManager.Bootstrapping)
        filingEntityManager.saveFilingEntity(entity)
        executor.execute {
            try {
                factBase.bootstrapFacts(cik)
                val updatedEntity =
                    entity.copy(lastUpdated = Instant.now().toString(), statusMessage = FilingEntityManager.Completed)
                filingEntityManager.saveFilingEntity(updatedEntity)
                log.info("Completed bootstrapping and initial model building cik=${entity.cik}")
            } catch (e: Exception) {
                log.error("Unable to complete bootstrapping and initial model building cik=${entity.cik}", e)
                filingEntityManager.saveFilingEntity(
                    entity.copy(
                        lastUpdated = Instant.now().toString(),
                        statusMessage = e.message
                    )
                )
            }
        }
        return entity
    }

    /**
     * Bootstraps a filing entity by creating the entity,
     * saving it and then parsing & storing facts by crawling through the SEC's website
     */
    fun bootstrapFilingEntitySync(cik: String): FilingEntity {
        filingEntityManager.deleteFilingEntity(cik)
        val entity = createFilingEntity(cik).copy(statusMessage = FilingEntityManager.Bootstrapping)
        filingEntityManager.saveFilingEntity(entity)
        try {
            factBase.bootstrapFacts(cik)
            val updatedEntity =
                entity.copy(lastUpdated = Instant.now().toString(), statusMessage = FilingEntityManager.Completed)
            filingEntityManager.saveFilingEntity(updatedEntity)
            log.info("Completed bootstrapping and initial model building cik=${entity.cik}")
        } catch (e: Exception) {
            log.error("Unable to complete bootstrapping and initial model building cik=${entity.cik}", e)
            filingEntityManager.saveFilingEntity(
                entity.copy(
                    lastUpdated = Instant.now().toString(),
                    statusMessage = e.message
                )
            )
        }
        return entity
    }
}