package co.bdozer.libraries.wikipedia

import co.bdozer.libraries.CompanyText
import co.bdozer.libraries.utils.HashGenerator.hash
import co.bdozer.libraries.utils.HtmlToPlainText.plainText
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

object WikipediaCrawler {

    private val log = LoggerFactory.getLogger(WikipediaCrawler::class.java)
    private val doc = Jsoup.connect("https://en.wikipedia.org/wiki/Russell_1000_Index").get()
    private val element = doc.select("#mw-content-text > div.mw-parser-output > table")[2]!!

    private val wikiLinkLookup = element
        .select("tr")
        .mapNotNull { tr ->
            if (tr.select("td").size == 2) {
                val tds = tr.select("td")
                val company = tds[0]
                val a = company.select("a")
                val href = a.attr("href")
                val link = "https://en.wikipedia.org$href"
                val ticker = tds[1].text().trim()
                if (href.isNotBlank()) {
                    ticker to link
                } else {
                    null
                }
            } else {
                null
            }
        }.toMap()

    init {
        if (wikiLinkLookup.isEmpty()) {
            error("Cannot construct table of Wikipedia article to company ticker")
        }
        log.info("Built ticker to Wikipedia link for ${wikiLinkLookup.size} entries")
    }

    fun getAllTickers(): List<String> {
        return wikiLinkLookup.keys.toList()
    }

    fun getCompanyText(ticker: String): CompanyText? {
        val link = wikiLinkLookup[ticker] ?: return null
        val text = getText(link)
        return if (text == null) {
            null
        } else {
            CompanyText(
                id = hash(ticker, link),
                ticker = ticker,
                text = text,
                url = link,
                source = "wikipedia",
            )
        }
    }

    private fun getText(url: String): String? {
        val doc = Jsoup.connect(url).get()
        val paragraphs = doc.select("#mw-content-text p")
        if (paragraphs.size == 0) {
            return null
        }
        val text = paragraphs.joinToString("\n\n") { plainText(it, printLinks = false) }
        if (text.trim().isBlank()) {
            return null
        }
        return text
    }

}