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

### Parameters learned from the truth set (SC-31)

At each weight set's own best threshold, hand-estimated and learned parameters
scored identically: 100% precision, 83.3% recall, F1 90.9%. The hand estimates
were kept. See `docs/accuracy.md` for the analysis and for why the F1-optimal
threshold was not adopted.

## Blocking performance

Query: find reference packages similar to `lodahs` (a typosquat of `lodash`),
across 5,000 reference packages. Measured with EXPLAIN ANALYZE on Postgres 16.

| | Execution time | Rows examined | Plan |
|---|---|---|---|
| Sequential scan | 36.119 ms | 5,000 | `Seq Scan`, 4,996 removed by filter |
| GIN trigram index | **1.109 ms** | 12 | `Bitmap Index Scan`, 9 heap blocks |

**32.6x faster.** Both return the same 4 candidates, `lodash` ranked first.

The index is `gin (normalized_name gin_trgm_ops)` from the `pg_trgm` extension.
Candidate selection runs in the database rather than in application code, so the
six comparators execute on ~4 candidates instead of 5,000 per package scored.

## Blocking performance

Postgres 16, 5,000 reference packages, GIN index on `normalized_name` using
`gin_trgm_ops` from the `pg_trgm` extension.

| Query | Plan | Rows examined | Execution time |
|---|---|---|---|
| `lodahs`, no index | Seq Scan | 5,000 | 36.1 ms |
| `lodahs`, indexed | Bitmap Index Scan | 12 | **1.1 ms** |
| `expres`, indexed (worst case) | Bitmap Index Scan | 171 | 6.8 ms |

Candidate selection runs in the database, so the six comparators execute on
4–50 candidates instead of 5,000 per package scored.

### Blocking recall: a silent failure found by probing

The similarity floor was initially 0.3. Probing with known squats from the truth
set showed `momnet` (a transposition of `moment`) returning **zero** candidates:
its trigram similarity is 0.273, just under the floor.

This failure is invisible to the engine's own metrics. A squat discarded at the
blocking stage never reaches the matching engine, so it does not register as a
miss — end-to-end recall was lower than the reported 93.3%, with nothing
indicating it.

Floor lowered to 0.2 and verified against ten known squats, all returning the
correct reference package. Worst-case query time rose from 1.1 ms to 6.8 ms,
still 5x faster than a sequential scan.

The mechanism behind the cost is visible in the plan: at 0.2 the index returns
171 rows and Postgres rechecks and discards 132. A looser floor makes the index
less selective, which is the real trade-off rather than the row count alone.