# Equity Valuation Server

The purpose of this server is to expose a Model for valuation of companies

## The Three Stages

## The `Model` 

To begin modeling a company, we first create a `Model` which consists of `Item`s

`Item` can either be part of the balance sheet, income statement or non-GAAP. 

`Item`s can explicitly depend on each other via `formulas`

Formulas specified on `Model` items and drivers are `atemporal` in that they define general relationships
between items for example: `GrossProfit = Revenue - CostOfGoodsSold` or `CostOfGoodsSold = MaterialCost+ShippingCost`

Each other the referenced values are either other `Item` or `Driver` - at this point, references are not
resolved to an exact cell such as `CostOfGoodsSold_Period5`. Relative references such as `Prev_CostOfGoodsSold` is possible

## The `Cell`s

At this stage, the fully formed `Model` gets turned into a `List<Cell>`, cell references are fully resolved.
The `Model` gets "expanded", so to speak, into it's realization across periods - with each Driver/Item in 
each period being a `Cell`

## Evaluating `Cells`

This is a depth-first computation graph evaluation algorithm implemented by the `CellEvaluator` class

## Generating the TypeScript SDK

```bash
openapi-generator-cli generate \
  -i http://localhost:8080/v3/api-docs \
  -g typescript-axios \
  -o ~/equity-model-builder-ui/client
```
