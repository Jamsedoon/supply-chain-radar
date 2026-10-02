# Benchmarks

## Matching engine accuracy

Measured by `MatchEngineAccuracyTest` against the 60-row truth set in
`src/test/resources/truth-set.csv`.

### Baseline — hand-estimated weights (SC-05b)

Weights guessed from domain reasoning, before any tuning.

| Metric | Value |
|---|---|
| Precision | FILL IN |
| Recall | FILL IN |
| F1 | FILL IN |
| True positives | FILL IN |
| False positives | FILL IN |
| True negatives | FILL IN |
| False negatives | FILL IN |

Threshold: 9.0 bits.

Failures concentrated in: FILL IN

### After hand tuning (SC-05c)

To be filled in.

### With weights learned from the truth set (SC-31)

To be filled in.
### After adding the family-prefix clue (SC-05c)

Measured twice: once on the original 60 rows, once after three `sibling_suffix`
rows were added to cover a pattern the set had barely tested.

| Metric | Baseline (60 rows) | + family-prefix clue (60 rows) | + harder rows (63 rows) |
|---|---|---|---|
| Precision | 85.3% | 90.3% | 82.4% |
| Recall | 96.7% | 93.3% | 93.3% |
| F1 | 90.6% | 91.8% | 87.5% |
| False positives | 5 | 3 | 6 |
| False negatives | 1 | 2 | 2 |

The drop in the third column is a harder test, not a worse engine. The three
added rows are sibling packages in the same family (`babel-preset-env` against
`babel-preset-react`), a pattern the original set contained only once.

All six remaining failures trace to one limitation — see `docs/accuracy.md`.