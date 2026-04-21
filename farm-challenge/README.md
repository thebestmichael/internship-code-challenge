# LogicGate Farm — Barn Assignment Challenge

A plain-Java solution to the [LogicGate Software Engineering Internship Challenge](https://github.com/thebestmichael/internship-code-challenge).  
Animals are read from `animals.json`, distributed evenly into color-coded barns, and the result is written to stdout as a JSON array.

---

## Project Structure

```
farm-challenge/
├── animals.json                          ← Input data
├── pom.xml                               ← Maven build (Jackson + JUnit 5)
├── README.md
└── src/
    ├── main/java/com/logicgate/farm/
    │   ├── Main.java                     ← Entry point
    │   ├── exception/
    │   │   └── FarmDataException.java    ← Checked exception for data errors
    │   ├── model/
    │   │   ├── Animal.java               ← Deserializes one livestock entry
    │   │   ├── FarmData.java             ← Deserializes the root JSON object
    │   │   └── BarnAssignment.java       ← One output barn entry
    │   ├── service/
    │   │   └── BarnAssignmentService.java← Grouping, distribution, overflow
    │   └── util/
    │       └── JsonParser.java           ← Parsing + validation
    └── test/java/com/logicgate/farm/
        ├── service/
        │   └── BarnAssignmentServiceTest.java
        └── util/
            └── JsonParserTest.java
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+

### 1. Compile and package
```bash
cd farm-challenge
mvn clean package -q
```

### 2. Run with the default `animals.json`
```bash
java -jar target/farm-challenge.jar
```

### 3. Run with a custom file path
```bash
java -jar target/farm-challenge.jar /path/to/your/animals.json
```

### 4. Run the test suite
```bash
mvn test
```

---

## Example Output

Running against the provided `animals.json` produces:

```json
[ {
  "barn" : "Barn_Black_1",
  "animals" : [ "Calf", "Cat", "Goat", "Cat" ]
}, {
  "barn" : "Barn_Blue_1",
  "animals" : [ "Rabbit", "Chicken", "Duck", "Horse" ]
}, {
  "barn" : "Barn_Brown_1",
  "animals" : [ "Donkey", "Dog", "Rooster", "Dog" ]
}, {
  "barn" : "Barn_Green_1",
  "animals" : [ "Goat", "Duck", "Chicken", "Pig" ]
}, {
  "barn" : "Barn_Red_1",
  "animals" : [ "Cow", "Pig", "Rabbit" ]
}, {
  "barn" : "Barn_Red_2",
  "animals" : [ "Donkey", "Goat", "Pig" ]
}, {
  "barn" : "Barn_Red_3",
  "animals" : [ "Chicken", "Duck", "Cat" ]
} ]
```

Green has exactly 4 animals (Goat, Duck, Chicken, Pig) — one full barn.  
Red has 9 animals, evenly distributed across 3 barns as 3 / 3 / 3.

If any animals exceed barn capacity limits, an `"Unassigned"` entry is appended:

```json
{
  "barn": "Unassigned",
  "animals": ["Animal13", "Animal14"]
}
```

---

## Approach & Design Decisions

### Single-pass grouping (O(n))
`BarnAssignmentService.groupByColor()` makes one pass over the livestock list
using `computeIfAbsent` on a `LinkedHashMap`. The linked map preserves
first-seen color order during grouping; the final barn list is then sorted
alphabetically by barn name so the output is deterministic regardless of input
order, with the optional `"Unassigned"` entry always appended last.

### Even-distribution arithmetic (O(n))
Rather than repeatedly filling barns in a loop, the number of barns and the size
of each barn are calculated with two integer operations:

```
numBarns  = min(ceil(total / barnCapacity), maxBarnsPerColor)
base      = total / numBarns
remainder = total % numBarns

barn[i] gets (base + 1) animals if i ≤ remainder, else base
```

This guarantees any two barns of the same color differ by at most 1, with larger
barns listed first — matching the challenge's bonus example exactly.

### Overflow / Unassigned
A configurable `maxBarnsPerColor` (default: 3) caps how many barns can be created
per color. Animals beyond `barnCapacity × maxBarnsPerColor` are collected into a
single `"Unassigned"` entry appended at the end of the output. This directly
satisfies the challenge's edge-case requirement.

### Validation strategy
`JsonParser` validates the input before it reaches the service layer:
- File existence check (before Jackson opens the file)
- Jackson `IOException` is caught and re-thrown as `FarmDataException`
- Structural validation walks every animal entry, reporting the index of the
  first bad record

This keeps the service layer free of null checks and parsing concerns.

### Color normalization
Color keys are lowercased before grouping and capitalized when writing barn names.
This means `"green"`, `"GREEN"`, and `"Green"` all route to the same barn group
and produce consistent `"Barn_Green_N"` labels.

---

## Time Complexity Summary

| Step | Complexity |
|------|-----------|
| JSON parsing | O(n) |
| Grouping by color | O(n) |
| Even distribution | O(n) |
| Sort barn names | O(k log k), k = distinct barn count |
| **Total** | **O(n + k log k)** ≈ O(n) in practice |

For a very large dataset (e.g., 1 million animals across 100 colors), the
dominant cost is O(n) grouping. The sort over k barns is negligible since
k ≤ colors × maxBarnsPerColor, which is bounded by the configuration, not n.

---

## Bonus Features Implemented

| Bonus | Implementation |
|-------|---------------|
| Even distribution | Arithmetic slice in `distributeEvenly()` — difference ≤ 1 |
| Unit tests (~90% coverage) | 52 tests across two test classes; normalization cases use `@ParameterizedTest` with `@CsvSource` for concise multi-color, multi-casing coverage |
| Malformed JSON handling | `JsonParser` catches `IOException` and rethrows as `FarmDataException` |
| Missing fields | Field-level null/blank checks with index-aware error messages |
| Empty dataset | Explicit check in `validate()` |
| Overflow / Unassigned | `maxBarnsPerColor` cap + `"Unassigned"` entry in output |

---

## What I Would Do With More Time

- **JaCoCo coverage report** — add the JaCoCo Maven plugin to generate an HTML
  report and verify the ~90% target numerically.
- **Parameterized tests** — added `@CsvSource`-driven normalization tests; could extend to
  even-distribution edge cases (n = 1 through 20).
- **CLI flags** — add `--capacity` and `--max-barns` flags via a lightweight
  args parser so the barn limits can be changed at runtime without recompiling.
- **Logging** — replace `System.err` with SLF4J + Logback for structured logging
  at appropriate levels (INFO for normal flow, WARN for overflow, ERROR for data
  problems).
- **Stream-based JSON output** — for datasets too large to hold in memory,
  Jackson's `JsonGenerator` could stream barn entries directly to stdout without
  building the full list first.
