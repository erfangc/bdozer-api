package com.bdozer.api.web.authn

import org.springframework.stereotype.Service

@Service
class RequestIdProvider {
    private val threadLocal = ThreadLocal<String>()
    fun set(requestId: String) = threadLocal.set(requestId)
    fun get() = threadLocal.get() ?: error("Unable to determine requestId")
}