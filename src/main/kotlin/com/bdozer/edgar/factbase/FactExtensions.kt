package com.bdozer.edgar.factbase

import com.bdozer.edgar.factbase.dataclasses.Dimension
import com.bdozer.edgar.factbase.dataclasses.Fact

object FactExtensions {

    fun List<Fact>.filterForDimensions(dimensions: List<Dimension>): List<Fact> {
        return filter { fact ->
            val explicitMembers = fact.explicitMembers
            // every declared dimension from the StatementTable prologue must be matched
            // by the declared explicit members of the fact for the fact to be counted
            explicitMembers.size == dimensions.size && dimensions.all { dimension ->
                val dimensionConcept = dimension.dimensionConcept
                explicitMembers
                    .any { explicitMember ->
                        explicitMember.dimension == dimensionConcept && dimension.memberConcepts.contains(
                            explicitMember.value
                        )
                    }
            }
        }
    }

    fun List<Fact>.filterForDimensionsWithFallback(dimensions: List<Dimension>): List<Fact> {
        val ret =  filter { fact ->
            val explicitMembers = fact.explicitMembers
            // every declared dimension from the StatementTable prologue must be matched
            // by the declared explicit members of the fact for the fact to be counted
            explicitMembers.size == dimensions.size && dimensions.all { dimension ->
                val dimensionConcept = dimension.dimensionConcept
                explicitMembers
                    .any { explicitMember ->
                        explicitMember.dimension == dimensionConcept && dimension.memberConcepts.contains(
                            explicitMember.value
                        )
                    }
            }
        }
        return ret.ifEmpty {
            filter { fact -> fact.explicitMembers.isEmpty() }
        }
    }

    fun Fact.dimensions(): List<Dimension> {
        return explicitMembers.map { explicitMember ->
            Dimension(
                dimensionConcept = explicitMember.dimension,
                memberConcepts = setOf(explicitMember.value)
            )
        }
    }

    fun List<Fact>.dimensions(): List<Dimension> {
        return flatMap { fact ->
            fact.explicitMembers.map {
                it.dimension to listOf(it.value)
            }
        }.groupBy {
            // first == dimension
            it.first
        }.map { (dimension, values) ->
            Dimension(dimensionConcept = dimension, memberConcepts = values.flatMap { it.second }.toSet())
        }
    }
}
