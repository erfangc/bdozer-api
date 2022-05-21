package co.bdozer.libraries.clientcredentialsgrant

data class ClientCredentialsGrantResponse(
    val access_token: String,
    val scope: String? = null,
    val expires_in: Long,
    val token_type:String,
)