package com.starburst.starburst.edgar.factbase.modelbuilder.formula.extensions

import com.starburst.starburst.edgar.factbase.modelbuilder.formula.ModelFormulaBuilderContext
import com.starburst.starburst.models.dataclasses.Item

object ElementSemanticsExtensions {

    /**
     * Is flow item
     */
    fun ModelFormulaBuilderContext.isFlowItem(item: Item): Boolean {
        val elementDefinition = elementDefinitionMap[item.name]
        val type = elementDefinition?.type
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        return ( abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }

    /**
     * Debit flow items are costs / expenses (those that are typically expected to be outflows_
     */
    fun ModelFormulaBuilderContext.isDebtFlowItem(item: Item): Boolean {
        // we find the element definition
        val elementDefinition = elementDefinitionMap[item.name]
        val balance = elementDefinition?.balance
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        val type = elementDefinition?.type

        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        return (balance == "debit"
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }

    /**
     * Credit flow items are revenues / incomes (those that are typically expected to be inflows_
     */
    fun ModelFormulaBuilderContext.isCreditFlowItem(item: Item): Boolean {
        // we find the element definition
        val elementDefinition = elementDefinitionMap[item.name]
        val balance = elementDefinition?.balance
        val abstract = elementDefinition?.abstract
        val periodType = elementDefinition?.periodType
        val type = elementDefinition?.type

        /*
        A non cash expense is one that
        1. is a non-abstract monetary debit item as defined by the company's XSD or us-gaap
        2. starts or ends with the correct keywords
         */
        return (balance == "credit"
                // != true is not the same as == false, as abstract could also be null
                && abstract != true
                && periodType == "duration"
                && type?.endsWith("monetaryItemType") == true)
    }

}