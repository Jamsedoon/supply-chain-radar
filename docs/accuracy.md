## The fix: a clue with negative weight

What distinguishes `webpack-cli`/`webpack` from `momnet`/`moment` is containment.
Extending a popular package name is the npm convention for official companion
packages — `react-dom`, `webpack-cli`, `jest-cli`. Squatters rarely do it,
because an extended name is not something a developer types by accident.

In Fellegi-Sunter terms the clue has a low agreement rate for genuine matches
(m = 0.05) and a high one for non-matches (u = 0.60), so log2(m/u) = -3.58. It
argues against a match. This needed no engine change: the model already
expresses evidence-against as a negative weight.

## What it actually achieved

Precision rose from 85.3% to 90.3%; recall fell from 96.7% to 93.3%; F1 rose
from 90.6% to 91.8%.

Two of the five false alarms cleared (`webpack-cli`/`webpack`,
`uuid`/`uuidv4`). Two did not, because their suffix is a single character and
the comparator requires at least two. One (`babel-core`/`babel-cli`) was
mislabelled and is now categorised `sibling_suffix` — neither name contains the
other, so the clue cannot apply.

The clue also introduced a new miss.

## All six failures, and the one thing they share

| Pair | Truth | Shape |
|---|---|---|
| `d3.js` / `d3` | attack | base + suffix |
| `jquery-min` / `jquery` | attack | base + suffix |
| `mysql` / `mysql2` | legitimate | base + 1-char suffix |
| `vue` / `vuex` | legitimate | base + 1-char suffix |
| `babel-core` / `babel-cli` | legitimate | shared prefix, divergent suffixes |
| `eslint-plugin-import` / `eslint-plugin-react` | legitimate | shared prefix, divergent suffixes |

Three structurally different shapes. The same wall in every case: the names do
not contain the information needed to decide.

`webpack-cli` extends `webpack` and is an official companion package.
`jquery-min` extends `jquery` and was an attack. `babel-core` and `babel-cli`
are as similar to each other as `crossenv` is to `cross-env`. No spelling
comparison separates these, because the distinguishing fact is not in the
spelling.

What separates them is popularity. Both `babel-core` and `babel-cli` have
millions of weekly downloads; the fake `jquery-min` had almost none. Checked
against all six rows, a popularity signal resolves every one: two popular
packages are siblings, while one popular package and one brand-new unknown is
an impersonation.

That signal is independent of the name and is applied in the scorer (SC-12b),
which checks download counts before raising an alert.

## The central finding

**Name similarity has a ceiling, and the ceiling sits exactly where the name
stops containing the answer.**

This was not assumed at the start. It arrived as a measured result, three times,
from three unrelated structural patterns. Each could have been patched with
another name-based clue; none of those patches would have generalised, because
the limitation is not in the clues.

This is also why real alerting systems combine independent signals rather than
refining a single one. Adding a seventh name comparator would have resolved four
of these six rows while remaining undefendable outside this truth set.

All six failures are left in place deliberately. A documented failure is more
useful than a hidden one, and these six are the argument for the next stage of
the pipeline.

## Changes considered and declined

**Lowering `MIN_SUFFIX_LENGTH` from 2 to 1** would clear `mysql`/`mysql2` and
`vue`/`vuex`, raising precision to roughly 88%. The four real squats affected
(`typescrip`, `prettie`, `expres`, `vue-router2`) would each lose 3.58 bits and
still clear the threshold.

Declined. The floor protects the dropped-letter attack pattern: at 1, the clue
would argue against genuine squats such as `mysq` against `mysql`. This truth
set contains no case where that is fatal, but the pattern is real, and changing
a parameter to gain two rows on a 63-row sample fits the test set rather than
the problem.

**A seventh comparator for the sibling-suffix pattern** would need to separate
`eslintplugin` + {`import`, `react`} — different words — from `loda` + {`hs`,
`sh`} — rearranged letters. The rule would be tuned against four rows, and would
leave the two base-plus-suffix misses untouched. Declined on the same grounds,
and because the popularity signal resolves all six rather than four.

## Tuning parameters chosen by judgement

- `MIN_BASE_LENGTH = 3` — almost any name starts with a given two-character
  string, so a shorter floor would fire on coincidence.
- `MIN_SUFFIX_LENGTH = 2` — see above.

## Limits of these figures

63 rows, one annotator, weighted toward family-prefix lookalikes. Sound enough
to tune against and to compare before and after; not enough to support
confidence intervals, and not production accuracy.

## Estimating the parameters from data (SC-31)

The m and u values were initially hand-estimated from domain reasoning.
`WeightEstimator` computes them instead by counting, for every comparator and
level, how often it fires across the squat rows (m) and the legitimate rows (u).

### Smoothing

Five comparators never fire across the legitimate rows, giving u = 0 and an
undefined log2(m/u). Counts are smoothed with a Jeffreys prior, adding 0.5 to
each hit count and 1 to each total. Zero observations in 33 does not mean
impossible; it means rarer than one in 33, with no information about how much
rarer. The correction shrinks automatically as the sample grows.

### Estimated against hand-estimated weights

| Comparator | Level | Hand | Learned | Squat hits | Legit hits |
|---|---|---|---|---|---|
| edit_distance | STRONG | 9.45 | 5.09 | 15/30 | 0/33 |
| edit_distance | WEAK | 2.74 | 0.69 | 12/30 | 8/33 |
| jaro_winkler | STRONG | 7.32 | **0.13** | 29/30 | 29/33 |
| keyboard_adjacency | STRONG | 11.14 | 4.38 | 9/30 | 0/33 |
| homoglyph | STRONG | 10.97 | 2.94 | 3/30 | 0/33 |
| delimiter | STRONG | 10.55 | 3.30 | 4/30 | 0/33 |
| family_prefix | STRONG | -3.58 | **-3.77** | 1/30 | 22/33 |

### Finding 1: Jaro-Winkler is nearly information-free here

It fires on 29 of 30 squats and 29 of 33 legitimate pairs. It fires on almost
everything, so its firing carries almost no information: 0.13 bits against a
hand estimate of 7.32, a 56x overestimate.

This was the single largest source of false alarms. Every false positive in the
SC-05c analysis scored 10.06 bits, of which 7.32 came from this clue.

The caveat is that every row in the truth set is already a similar-looking pair
by construction. Against randomly chosen package names, Jaro-Winkler would fire
far less often and u would be much lower. So 0.13 bits is its value *among
already-similar candidates* — which is precisely the population the blocking
stage (SC-08) hands to the scorer, so the figure is the relevant one.

### Finding 2: the family-prefix reasoning was confirmed

In SC-05c the negative weight for `family_prefix` was argued from first
principles and set by hand to m = 0.05, u = 0.60, giving -3.58 bits. Counted
from the data: m = 0.048, u = 0.662, giving -3.77 bits — a difference of 0.19
bits.

Eleven of twelve hand estimates were substantially wrong. The one that was
nearly exact was the one reasoned about carefully rather than guessed. It also
rests on a single squat observation, so the agreement is less strong evidence
than it appears.

### Finding 3: the threshold is on the same scale as the weights

The first comparison held the threshold at 9.0 for both weight sets, and recall
collapsed from 93.3% to 3.3%. That was a measurement error, not a result: every
learned weight is smaller, so a typical squat totals around 5 bits against an
unchanged bar of 9.

The threshold is not independent of the weights; it is expressed in the same
unit. `ThresholdSweepTest` therefore sweeps every distinct total the rows produce
and gives each weight set its own optimum.

### Fair comparison

| | Hand-estimated | Learned |
|---|---|---|
| Best threshold | 14.61 | 3.76 |
| Precision | 100% | 100% |
| Recall | 83.3% | 83.3% |
| F1 | 90.9% | 90.9% |

Identical. Learning the parameters changed the scale of the weights
substantially but barely changed the ordering of rows, and classification
depends only on ordering plus threshold.

Not quite perfectly: the hand weights produce 14 distinct totals across 63 rows,
the learned weights 12. Two pairs of rows that were separable now tie. The
compression cost a little resolution, though not at the optimum.

### What was adopted, and why

The hand-estimated weights were kept.

1. Equal measured performance, so adoption buys nothing.
2. The learned u values measure the wrong population. The truth set is a
   balanced 30/33 split; real npm traffic has legitimate pairs outnumbering
   squats by many orders of magnitude. Estimating "how often does this fire on
   non-matches" from a curated balanced set does not describe the wild.
3. Four parameters rest on fewer than five observations.
4. The learned set lost two distinctions.

The analysis is retained regardless. The Jaro-Winkler finding is the most useful
output of this work and is independent of which parameter set ships.

### Why the F1-optimal threshold was NOT adopted

The sweep identifies 14.61 as F1-optimal for the shipped weights, against the
configured 9.0. It was not adopted, for two reasons.

**The optimum is in-sample.** It was found by trying every value on the same 63
rows it is scored against. Reporting 90.9% as a clean measurement would be
dishonest. A held-out set would be needed, and 63 rows cannot support a split
that leaves either half meaningful.

**F1 is the wrong objective for this system.** F1 weights a false alarm and a
missed attack equally. They are not equal: a false alarm costs a developer
seconds, a missed attack means malicious code executing. The F1 optimum trades
three additional missed attacks for six fewer false alarms.

| Threshold | Precision | Recall | Attacks caught | False alarms |
|---|---|---|---|---|
| 7.32 | 80.6% | 96.7% | 29 of 30 | 7 |
| 9.00 (shipped) | 82.4% | 93.3% | 28 of 30 | 6 |
| 14.61 (best F1) | 100% | 83.3% | 25 of 30 | 0 |

There is also an architectural reason. Name matching is the first of two stages;
the popularity check in SC-12b is the second. A first stage should favour recall,
because anything it discards cannot be recovered downstream, while the second
stage can add precision. Tuning stage one for precision defeats the design.

The full curve is reported instead of a single optimum, since the right operating
point depends on what the downstream stage removes.