# Equity Valuation Server

The purpose of this server is to expose a Model for valuation of companies

## The Three Stages

## Stage 1 - `Model` to `Model` 

At this stage, the user defines a `Model` which consists of `Item`s

`Item` can either be part of the balance sheet, income statement or none-GAAP. 

`Item`s can either explicitly depend on each otheror they can be composed of drivers - which can also
reference each other

Formulas specified on `Model` items and drivers are `atemporal` in that they define general relationships
between items for example: `GrossProfit = Revenue - CostOfGoodsSold` or `CostOfGoodsSold = MaterialCost+ShippingCost`

Each other the referenced values are either other `Item` or `Driver` - at this point, references are not
resolved to an exact cell such as `CostOfGoodsSold_Period5`. Relative references such as `Prev_CostOfGoodsSold` is possible

## Stage 2 - `Model` to `Cell`

At this stage, the fully formed `Model` gets turned into a `List<Cell>`, cell references are fully resolved.
The `Model` gets "expanded", so to speak, into it's realization across periods - with each Driver/Item in 
each period being a `Cell`

## Stage 3 - `Cells` Evaluation

This is a depth-first computation graph evaluation algorithm implemented by the `CellEvaluator` class
