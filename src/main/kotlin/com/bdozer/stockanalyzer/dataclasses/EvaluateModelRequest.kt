package com.bdozer.stockanalyzer.dataclasses

import com.bdozer.models.dataclasses.Model
import io.swagger.v3.oas.annotations.media.Schema

@Schema
data class EvaluateModelRequest(

    @Schema(
        description = """
        The _id of the stock analysis. This value will be copied
        to the resulting stock analysis object, if left blank
        a random ID will be generated
    """
    )
    val _id: String? = null,

    @Schema(
        description = """
        The name of the stock analysis. This value will be copied
        to the resulting stock analysis object
    """
    )
    val name: String? = null,

    @Schema(
        description = """
        The description of the stock analysis. This value will be copied
        to the resulting stock analysis object
    """
    )
    val description: String? = null,

    @Schema(
        description = """
        This is the main object required for evaluating the model
        into a stock analysis
    """
    )
    val model: Model,

)