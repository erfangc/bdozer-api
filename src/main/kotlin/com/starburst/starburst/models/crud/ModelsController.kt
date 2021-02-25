package com.starburst.starburst.models.crud

import com.mongodb.client.MongoClient
import com.starburst.starburst.authn.AuthenticationFilter.Companion.decodeJWT
import com.starburst.starburst.models.Model
import com.starburst.starburst.models.ModelHistory
import com.starburst.starburst.models.builders.ModelBuilder
import com.starburst.starburst.models.builders.SkeletonModel.dropbox
import org.javers.core.JaversBuilder
import org.litote.kmongo.*
import org.springframework.web.bind.annotation.*
import java.lang.Exception
import javax.servlet.http.HttpServletRequest

@CrossOrigin
@RestController
@RequestMapping("api/models")
class ModelsController(
    private val modelBuilder: ModelBuilder,
    mongoClient: MongoClient
) {

    private val collectionName = Model::class.java.simpleName
    private val database = mongoClient.getDatabase("starburst")
    private val collection = database.getCollection<Model>(collectionName)
    private val snapshots = database.getCollection<ModelHistory>("${collectionName}_Snapshots")
    private val javers = JaversBuilder.javers().build()

    @GetMapping("sample")
    fun sample(): Model {
        return modelBuilder.reformulateModel(dropbox)
    }

    @GetMapping
    fun listModels(): List<Model> {
        return collection.find().toList()
    }

    @GetMapping("{id}/history")
    fun getHistory(@PathVariable id: String): List<ModelHistory> {
        return snapshots.find(ModelHistory::modelId eq id).limit(10).toList()
    }

    @GetMapping("{id}")
    fun getModel(@PathVariable id: String): Model {
        return collection.findOneById(id) ?: error("cannot find model $id")
    }

    @PostMapping
    fun saveModel(@RequestBody newModel: Model, request: HttpServletRequest): Model {
        val existingModel = try {
            getModel(newModel._id)
        } catch (e: Exception) {
            null
        }
        val updatedBy = decodeJWT(request).subject
        val obj = newModel.copy(updatedBy = updatedBy)
        collection.save(obj)
        if (existingModel != null) {
            val diff = javers.compare(existingModel, newModel)
            val changeSummary = diff.prettyPrint()
            snapshots.save(
                ModelHistory(
                    changeSummary = changeSummary,
                    model = existingModel,
                    modelId = newModel._id
                )
            )
        }
        return obj
    }

    @DeleteMapping("{id}")
    fun deleteModel(@PathVariable id: String): String {
        collection.deleteOneById(id)
        return id
    }

}
