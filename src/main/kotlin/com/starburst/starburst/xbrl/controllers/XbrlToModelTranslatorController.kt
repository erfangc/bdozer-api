package com.starburst.starburst.xbrl.controllers

import com.starburst.starburst.models.Model
import com.starburst.starburst.xbrl.XbrlToModelTranslator
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream


@RestController
@CrossOrigin
@RequestMapping("api/xbrl-to-model-translator")
class XbrlToModelTranslatorController(private val http: HttpClient) {

    //
    // at launch create the us-gaap XSD
    //
    private val gaapXsdLink = "http://xbrl.fasb.org/us-gaap/2020/elts/us-gaap-2020-01-31.xsd"
    private val usGaapXsdBytes = readLink(gaapXsdLink)

    @GetMapping("{symbol}")
    fun parseModel(@PathVariable cik: String): Model {
        // TODO
        val basedir = "/Users/erfangchen/XBRLs/0001467623-21-000012-xbrl"
        val instanceFile = File(basedir, "dbx-10k-20201231_htm.xml")
        val extensionXsdFile = File(basedir, "dbx-20201231.xsd")
        val calculationFile = File(basedir, "dbx-20201231_cal.xml")
        val labelFile = File(basedir, "dbx-20201231_lab.xml")
        val definitionFile = File(basedir, "dbx-20201231_def.xml")

        val translator = XbrlToModelTranslator(
            usGaapXsdStream = ByteArrayInputStream(usGaapXsdBytes),
            instanceStream = FileInputStream(instanceFile),
            extensionXsdStream = FileInputStream(extensionXsdFile),
            calculationStream = FileInputStream(calculationFile),
            labelStream = FileInputStream(labelFile),
            definitionStream = FileInputStream(definitionFile),
        )

        return translator.translate()
    }

    private fun readLink(link: String): ByteArray? {
        return http.execute(HttpGet(link))
            .entity
            .content
            .readAllBytes()
    }
}

