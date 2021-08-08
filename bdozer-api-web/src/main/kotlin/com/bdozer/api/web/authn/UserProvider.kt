package com.bdozer.api.web.authn

import com.auth0.json.auth.UserInfo
import org.springframework.stereotype.Service

@Service
class UserProvider {
    private val threadLocal = ThreadLocal<UserInfo>()
    fun set(user: UserInfo) = threadLocal.set(user)
    fun get() = threadLocal.get() ?: error("Unable to determine user")
}