package com.bdozer.api.web.factbase

import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin
@RequestMapping("public/process-secfiling-request-publisher")
class ProcessSECFilingRequestPublisherController(
    val processSECFilingRequestPublisher: ProcessSECFilingRequestPublisher
) {
    @PostMapping("publish-request")
    fun publishRequest(@RequestParam cik: String, @RequestParam adsh: String) {
        processSECFilingRequestPublisher.publishRequest(cik, adsh)
    }
}