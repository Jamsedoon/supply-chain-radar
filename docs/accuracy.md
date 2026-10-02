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