package com.starburst.starburst.unrelated

import com.mongodb.client.MongoClient
import org.litote.kmongo.getCollection
import org.litote.kmongo.save
import org.springframework.web.bind.annotation.*

@CrossOrigin
@RestController
@RequestMapping("public/register-email")
class RegisterEmailController(mongo: MongoClient) {

    private val database = mongo.getDatabase("ease-wealth")
    private val col = database.getCollection<RegisteredEmail>()

    @PostMapping
    fun register(
        @RequestParam email: String,
        @RequestParam(required = false) stock: String? = null
    ): Unit {
        col.save(
            RegisteredEmail(
                email = email,
                stock = stock
            )
        )
    }

}