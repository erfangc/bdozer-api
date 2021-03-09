package com.starburst.starburst.edgar.factbase.modelbuilder

import com.starburst.starburst.models.Model

class ModelFormulaBuilder {
    /**
     * Takes as input model that is already linked via the calculationArcs
     * and with historical values for the items populated
     *
     * This is the master method for which we begin to populate formulas and move them beyond
     * simply 0.0 or repeating historical
     */
    fun buildModelFormula(model: Model): Model {
        return model
    }
}