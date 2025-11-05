# Root Cause Analysis
Bug ID: TRD-2025-001


## Executive Summary
A critical defect was identified in the cashflow calculation logic for fixed-leg trades. The issue caused cashflows to be approximately 100 times larger than expected, resulting in discrepancies in interest calculations across multiple trades.

For example, a trade with a $10 million notional and a 3.5% fixed rate should produce a quarterly cashflow of approximately $87,500, but the system generated $875,000. Additionally, slight precision deviations (e.g., 87,500.00000000001) were observed.

Using business logic, the service has been updated:

 - To treat all incoming rates as **percent** values, dividing by 100 to get the correct decimal rate.
 - To use `BigDecimal` for all monetary maths with `HALF_EVEN` rounding, eliminating magnitude and precision issues. All incoming rates are now treated as percent values and converted to decimals during calculation.


**Business Impact**
This was a high severity issue because it directly affected trading operations and financial reporting. The overstated fixed-leg cashflows likely led to:

 - Incorrect profit/loss and risk exposure reporting.
 - Mispricing of trades and potential counterparty reconciliation issues.
 - Loss of confidence from front-office and risk teams in system accuracy.


## Technical Investigation
The investigation focused on the `calculateCashflowValue` method within `TradeService.java`, which is responsible for computing periodic cashflows for each trade leg.

**Debugging Methodology**

 - **Test Reproduction:** A controlled test was run using a trade with known inputs: notional = $10,000,000, rate = 3.5%, and a quarterly period (3 months).
 - **Logging:** Logging statements were added to capture all calculation inputs (notional, rate, months) and outputs at runtime.
 - **Observation:** The log output showed:
 inputs: notional=10,000,000, rate(raw)=3.5, months=3.0
 result = 875000.0
This confirmed that the 3.5% rate was being treated as 3.5 rather than 0.035.
 - **Precision Review:** Additional logs showed minor decimal anomalies such
   as 87500.00000000001, indicating floating-point rounding errors due
   to double variable arithmetic.
 - **Cross-checking Formula:** The formula itself  *(notional * rate * months) / 12*  was mathematically sound, but the input handling and data types were incorrect.

**Findings**
- The percentage rate was not converted to a decimal before applying the formula.
- The use of double for monetary values introduced floating-point precision errors.
- Both issues combined caused inflated and inconsistent cashflow results.


## Root Cause Details

**Percentage Handling Bug**
The rate field was interpreted inconsistently across the system. In some cases, users entered 3.5 to represent 3.5%, while the calculation logic assumed it as a decimal form. Because the `calculateCashflowValue` method directly used the rate without normalisation, the rate value was 100 times larger than intended.

As a result, a 3.5% rate applied as 3.5 led to a hundredfold overstatement in the computed cashflow amount.

**Precision Bug (Use of double):**
Monetary values (notional, rate, and result) were handled as double, a binary floating-point type that cannot represent many decimal fractions precisely (e.g., 0.1, 0.035).

As a result, even correct calculations showed small precision errors such as 87500.00000000001. This posed reconciliation risks and made regression testing unreliable due to small rounding differences.

Together, these two issues explain both the magnitude error (incorrect rate conversion) and the precision noise (floating-point representation).


## Proposed Solution (Technical fix approach and alternatives)


**Rate Normalisation**
Introduce logic to automatically convert rates expressed as percentages to decimals.

**Implementation**
```java
// Percent-only assumption: always divide by 100 (supports small and negative percents)
BigDecimal rateDecimal = rawRate.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_EVEN);
```

**Example**  
3.5% → 0.035,  
0.5% → 0.005,  
−0.25% → −0.0025  

This enforces the percent-only input and converts percent values to decimals for calculation.

**Precision Improvement**
Replace all `double` calculations with `BigDecimal` to maintain financial precision.  
Apply explicit rounding to two decimal places for currency values using `RoundingMode.HALF_EVEN`.

**Example fix**
```java
BigDecimal result = notional
    .multiply(rateDecimal)
    .multiply(months.divide(BigDecimal.valueOf(12), 12, RoundingMode.HALF_EVEN))
    .setScale(2, RoundingMode.HALF_EVEN);
```

**Validation and Testing**
 - Added unit tests to validate that:
\$10M @ 3.5% quarterly = \$87,500.00  
Results have no rounding anomalies.  
 - Added integration tests for end-to-end cashflow generation, ensuring both fixed and floating legs behave correctly.

**Alternatives Considered**
**Alternative 1:** Require all rates to be supplied in decimal format only (e.g., 0.035).  
- **Pros:** Simplifies the calculation logic.  
- **Cons:** Breaks existing integrations that currently send percentage rates.  

**Alternative 2:** Introduce a new `RateUnit` field (`PERCENT` or `DECIMAL`) in the data model.  
- **Pros:** Explicitly distinguishes between input formats.  
- **Cons:** Requires database schema and API changes across multiple services.  

**Final Decision**
Enforce a **percent-only** input assumption that always divides by 100 during calculations and replace all `double` maths with `BigDecimal`. This provides correct calculations with consistency. A future enhancement may introduce an explicit `RateUnit` if needed to differentiate between rates parsed as percentages and decimals.