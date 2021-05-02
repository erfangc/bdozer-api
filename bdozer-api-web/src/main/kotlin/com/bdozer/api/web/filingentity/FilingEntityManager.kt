package com.bdozer.api.web.filingentity

import com.bdozer.api.factbase.core.extensions.HttpClientExtensions.readEntity
import com.bdozer.api.web.filingentity.dataclasses.Address
import com.bdozer.api.web.filingentity.dataclasses.FilingEntity
import com.bdozer.api.web.filingentity.internal.SECEntity
import com.mongodb.client.MongoDatabase
import org.apache.http.client.HttpClient
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FilingEntityManager(
    mongoDatabase: MongoDatabase,
    private val httpClient: HttpClient,
) {

    companion object {
        const val Completed = "Completed"
        const val Created = "Created"
        const val Bootstrapping = "Bootstrapping"
    }

    private val col = mongoDatabase.getCollection<FilingEntity>()

    fun getFilingEntity(cik: String): FilingEntity? {
        return col.findOneById(cik.padStart(10, '0'))
    }

    fun saveFilingEntity(filingEntity: FilingEntity) {
        col.save(filingEntity)
    }

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
            statusMessage = Created,
        )
        col.save(entity)
        return entity
    }

}
