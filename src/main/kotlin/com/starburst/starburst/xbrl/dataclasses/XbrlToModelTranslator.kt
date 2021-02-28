package com.starburst.starburst.xbrl.dataclasses

import com.starburst.starburst.xbrl.XbrlToModelTranslator
import java.io.File
import java.io.FileInputStream

private const val basedir = "/Users/erfangchen/XBRLs/0001467623-21-000012-xbrl"

fun main() {

    val usGaapXsdFile: File = File(basedir, "../us-gaap-2020-01-31.xsd")
    val instanceFile: File = File(basedir, "dbx-10k-20201231_htm.xml")
    val extensionXsdFile: File = File(basedir, "dbx-20201231.xsd")
    val calculationFile: File = File(basedir, "dbx-20201231_cal.xml")
    val labelFile: File = File(basedir, "dbx-20201231_lab.xml")
    val definitionFile: File = File(basedir, "dbx-20201231_def.xml")

    val translator = XbrlToModelTranslator(
        usGaapXsdStream =  FileInputStream(usGaapXsdFile),
        instanceStream = FileInputStream(instanceFile),
        extensionXsdStream = FileInputStream(extensionXsdFile),
        calculationStream = FileInputStream(calculationFile),
        labelStream = FileInputStream(labelFile),
        definitionStream = FileInputStream(definitionFile),
    )

    val data = translator.translate()
    data
}

