package com.logicgate.farm.service;

import com.logicgate.farm.exception.FarmDataException;
import com.logicgate.farm.model.Animal;
import com.logicgate.farm.model.BarnAssignment;
import com.logicgate.farm.util.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BarnAssignmentService}.
 *
 * <p>Tests are organized into nested classes by feature area:
 * <ul>
 *   <li>{@link GroupingTests} – color-based grouping</li>
 *   <li>{@link CapacityTests} – barn capacity limits</li>
 *   <li>{@link EvenDistributionTests} – even spread across barns</li>
 *   <li>{@link OverflowTests} – animals that exceed total capacity (opt-in cap)</li>
 *   <li>{@link SortingTests} – alphabetical output ordering</li>
 *   <li>{@link NormalizationTests} – case-insensitive color handling</li>
 *   <li>{@link ConfigurationTests} – custom capacity / barn limits</li>
 *   <li>{@link ScalingTests} – regression tests for the unbounded default</li>
 *   <li>{@link IntegrationTests} – full animals.json dataset, loaded from disk</li>
 * </ul>
 */
class BarnAssignmentServiceTest {

    /** Default service: barnCapacity=4, maxBarnsPerColor=Integer.MAX_VALUE. */
    private BarnAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new BarnAssignmentService();
    }

    // Helpers

    /** Creates a list of n animals all preferring the given color. */
    private List<Animal> nAnimals(int n, String color) {
        List<Animal> animals = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            animals.add(new Animal("Animal" + i, color));
        }
        return animals;
    }

    /** Finds the BarnAssignment with the given name, or null if absent. */
    private BarnAssignment findBarn(List<BarnAssignment> assignments, String name) {
        return assignments.stream()
                .filter(b -> b.getBarnName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /** Builds a service with the historical 3-barns-per-color cap for overflow tests. */
    private BarnAssignmentService cappedService() {
        return new BarnAssignmentService(4, 3);
    }

    @Nested
    @DisplayName("Grouping Tests")
    class GroupingTests {

        @Test
        @DisplayName("Single animal produces exactly one barn")
        void singleAnimalCreatesOneBarn() {
            List<BarnAssignment> result = service.assignAnimals(
                    List.of(new Animal("Goat", "Green")));

            assertEquals(1, result.size());
            assertEquals("Barn_Green_1", result.get(0).getBarnName());
            assertEquals(List.of("Goat"), result.get(0).getAnimals());
        }

        @Test
        @DisplayName("Animals with different colors create separate barns")
        void differentColorsProduceSeparateBarns() {
            List<Animal> animals = List.of(
                    new Animal("Goat", "Green"),
                    new Animal("Cow",  "Red")
            );
            List<BarnAssignment> result = service.assignAnimals(animals);

            assertEquals(2, result.size());
            assertNotNull(findBarn(result, "Barn_Green_1"));
            assertNotNull(findBarn(result, "Barn_Red_1"));
        }

        @Test
        @DisplayName("Animals with the same color are placed in the same barn group")
        void sameColorGoesToSameBarnGroup() {
            List<Animal> animals = List.of(
                    new Animal("Goat",    "Green"),
                    new Animal("Duck",    "Green"),
                    new Animal("Chicken", "Green")
            );
            List<BarnAssignment> result = service.assignAnimals(animals);

            // All 3 should fit in a single Green barn
            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getAnimals().size());
        }

        @Test
        @DisplayName("Animals retain their insertion order within a barn")
        void animalOrderIsPreservedWithinBarn() {
            List<Animal> animals = List.of(
                    new Animal("Cow",    "Red"),
                    new Animal("Pig",    "Red"),
                    new Animal("Rabbit", "Red")
            );
            List<BarnAssignment> result = service.assignAnimals(animals);

            assertEquals(List.of("Cow", "Pig", "Rabbit"), result.get(0).getAnimals());
        }
    }

    @Nested
    @DisplayName("Capacity Tests")
    class CapacityTests {

        @Test
        @DisplayName("Exactly 4 animals fills one barn completely")
        void exactlyFourAnimals_OneBarnFull() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(4, "Blue"));

            assertEquals(1, result.size());
            assertEquals(4, result.get(0).getAnimals().size());
        }

        @Test
        @DisplayName("5 animals creates 2 barns (one spills over)")
        void fiveAnimals_TwoBarns() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(5, "Blue"));

            assertEquals(2, result.size());
            assertEquals("Barn_Blue_1", result.get(0).getBarnName());
            assertEquals("Barn_Blue_2", result.get(1).getBarnName());
        }

        @Test
        @DisplayName("8 animals fills exactly 2 barns")
        void eightAnimals_TwoFullBarns() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(8, "Red"));

            assertEquals(2, result.size());
            assertEquals(4, result.get(0).getAnimals().size());
            assertEquals(4, result.get(1).getAnimals().size());
        }

        @Test
        @DisplayName("12 animals fills exactly 3 barns (no overflow)")
        void twelveAnimals_ThreeFullBarns() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(12, "Green"));

            assertEquals(3, result.size());
            result.forEach(b -> assertEquals(4, b.getAnimals().size()));
        }
    }

    @Nested
    @DisplayName("Even Distribution Tests")
    class EvenDistributionTests {

        @Test
        @DisplayName("10 animals → 3 barns of 4, 3, 3 (challenge example)")
        void tenAnimals_EvenDistribution_4_3_3() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(10, "Blue"));

            assertEquals(3, result.size());
            assertEquals(4, result.get(0).getAnimals().size());
            assertEquals(3, result.get(1).getAnimals().size());
            assertEquals(3, result.get(2).getAnimals().size());
        }

        @Test
        @DisplayName("9 animals → 3 barns of 3, 3, 3")
        void nineAnimals_EvenDistribution_3_3_3() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(9, "Red"));

            assertEquals(3, result.size());
            result.forEach(b -> assertEquals(3, b.getAnimals().size()));
        }

        @Test
        @DisplayName("7 animals → 2 barns of 4, 3")
        void sevenAnimals_EvenDistribution_4_3() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(7, "Black"));

            assertEquals(2, result.size());
            assertEquals(4, result.get(0).getAnimals().size());
            assertEquals(3, result.get(1).getAnimals().size());
        }

        @Test
        @DisplayName("Barn size difference is never greater than 1 for any input")
        void barnSizeDifferenceAtMostOne() {
            BarnAssignmentService flexible = new BarnAssignmentService(5, 4);

            for (int n = 1; n <= 20; n++) {
                List<BarnAssignment> result = flexible.assignAnimals(nAnimals(n, "Brown"));
                List<Integer> sizes = result.stream()
                        .filter(b -> !b.getBarnName().equals("Unassigned"))
                        .map(b -> b.getAnimals().size())
                        .toList();

                if (sizes.size() > 1) {
                    int max = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);
                    int min = sizes.stream().mapToInt(Integer::intValue).min().orElse(0);
                    assertTrue(max - min <= 1,
                            "n=" + n + ": barn size diff was " + (max - min));
                }
            }
        }

        @Test
        @DisplayName("Larger barns always come before smaller barns")
        void largerBarnsFirst() {
            // 11 animals → expected: 4, 4, 3
            List<BarnAssignment> result = service.assignAnimals(nAnimals(11, "Green"));

            List<Integer> sizes = result.stream()
                    .map(b -> b.getAnimals().size())
                    .toList();

            for (int i = 0; i < sizes.size() - 1; i++) {
                assertTrue(sizes.get(i) >= sizes.get(i + 1),
                        "Barn " + (i + 1) + " should be >= barn " + (i + 2));
            }
        }
    }

    @Nested
    @DisplayName("Overflow / Unassigned Tests (opt-in via capped service)")
    class OverflowTests {

        private BarnAssignmentService capped;

        @BeforeEach
        void setUpCapped() {
            capped = cappedService();  // barnCapacity=4, maxBarnsPerColor=3
        }

        @Test
        @DisplayName("13 animals under a 3-barn cap → 1 goes to Unassigned")
        void thirteenAnimals_OneOverflow() {
            List<BarnAssignment> result = capped.assignAnimals(nAnimals(13, "Black"));

            BarnAssignment unassigned = findBarn(result, "Unassigned");
            assertNotNull(unassigned, "Unassigned barn should exist");
            assertEquals(1, unassigned.getAnimals().size());
        }

        @Test
        @DisplayName("15 animals under a 3-barn cap → 3 overflow animals")
        void fifteenAnimals_ThreeOverflow() {
            List<BarnAssignment> result = capped.assignAnimals(nAnimals(15, "Blue"));

            BarnAssignment unassigned = findBarn(result, "Unassigned");
            assertNotNull(unassigned);
            assertEquals(3, unassigned.getAnimals().size());
        }

        @Test
        @DisplayName("Unassigned entry is always the last element")
        void unassignedEntryIsAlwaysLast() {
            List<BarnAssignment> result = capped.assignAnimals(nAnimals(14, "Red"));

            assertEquals("Unassigned",
                    result.get(result.size() - 1).getBarnName());
        }

        @Test
        @DisplayName("No Unassigned entry when all animals fit under the cap")
        void noUnassignedWhenAllFit() {
            List<BarnAssignment> result = capped.assignAnimals(nAnimals(12, "Green"));

            boolean hasUnassigned = result.stream()
                    .anyMatch(b -> b.getBarnName().equals("Unassigned"));
            assertFalse(hasUnassigned);
        }

        @Test
        @DisplayName("Overflow from multiple colors are all collected in one Unassigned entry")
        void overflowFromMultipleColors_SingleUnassigned() {
            List<Animal> animals = new ArrayList<>();
            // 13 Green → 1 overflow; 13 Red → 1 overflow
            animals.addAll(nAnimals(13, "Green"));
            animals.addAll(nAnimals(13, "Red"));

            List<BarnAssignment> result = capped.assignAnimals(animals);

            long unassignedCount = result.stream()
                    .filter(b -> b.getBarnName().equals("Unassigned"))
                    .count();
            assertEquals(1, unassignedCount, "Only one Unassigned entry should exist");

            BarnAssignment unassigned = findBarn(result, "Unassigned");
            assertNotNull(unassigned);
            assertEquals(2, unassigned.getAnimals().size()); // 1 from each color
        }
    }

    @Nested
    @DisplayName("Sorting Tests")
    class SortingTests {

        @Test
        @DisplayName("Barns are ordered alphabetically by barn name")
        void barnsAreSortedAlphabetically() {
            List<Animal> animals = List.of(
                    new Animal("Pig",  "Red"),
                    new Animal("Duck", "Blue"),
                    new Animal("Goat", "Green")
            );
            List<BarnAssignment> result = service.assignAnimals(animals);

            assertEquals("Barn_Blue_1",  result.get(0).getBarnName());
            assertEquals("Barn_Green_1", result.get(1).getBarnName());
            assertEquals("Barn_Red_1",   result.get(2).getBarnName());
        }

        @Test
        @DisplayName("Numbered barns within a color are ordered 1, 2, 3")
        void numberedBarnsWithinColorAreOrdered() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(12, "Red"));

            assertEquals("Barn_Red_1", result.get(0).getBarnName());
            assertEquals("Barn_Red_2", result.get(1).getBarnName());
            assertEquals("Barn_Red_3", result.get(2).getBarnName());
        }
    }

    @Nested
    @DisplayName("Color Normalization Tests")
    class NormalizationTests {

        @ParameterizedTest
        @DisplayName("Color normalization produces correct Title-Case barn name")
        @CsvSource({
            "GREEN,  Barn_Green_1",
            "green,  Barn_Green_1",
            "blue,   Barn_Blue_1",
            "rEd,    Barn_Red_1",
            "gReEn,  Barn_Green_1"
        })
        void colorNormalizationIsCaseInsensitive(String color, String expectedBarn) {
            List<BarnAssignment> result = service.assignAnimals(
                    List.of(new Animal("Cat", color)));
            assertEquals(expectedBarn.trim(), result.get(0).getBarnName());
        }

        @Test
        @DisplayName("'green' and 'GREEN' animals are grouped into the same barns")
        void caseInsensitiveGrouping() {
            List<Animal> animals = List.of(
                    new Animal("Goat",    "green"),
                    new Animal("Duck",    "GREEN"),
                    new Animal("Chicken", "Green")
            );
            List<BarnAssignment> result = service.assignAnimals(animals);

            assertEquals(1, result.size());
            assertEquals(3, result.get(0).getAnimals().size());
        }
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        @Test
        @DisplayName("Custom barnCapacity=2 splits 3 animals into 2 barns (2, 1)")
        void customBarnCapacity() {
            BarnAssignmentService custom = new BarnAssignmentService(2, 5);
            List<BarnAssignment> result = custom.assignAnimals(nAnimals(3, "Blue"));

            assertEquals(2, result.size());
            assertEquals(2, result.get(0).getAnimals().size());
            assertEquals(1, result.get(1).getAnimals().size());
        }

        @Test
        @DisplayName("maxBarnsPerColor=1 limits to a single barn; extras go Unassigned")
        void maxBarnsPerColorOne() {
            BarnAssignmentService single = new BarnAssignmentService(4, 1);
            List<BarnAssignment> result = single.assignAnimals(nAnimals(6, "Red"));

            BarnAssignment unassigned = findBarn(result, "Unassigned");
            assertNotNull(unassigned);
            assertEquals(2, unassigned.getAnimals().size());
        }

        @Test
        @DisplayName("Default service reports Integer.MAX_VALUE for maxBarnsPerColor")
        void defaultMaxBarnsIsUnbounded() {
            assertEquals(4, service.getBarnCapacity());
            assertEquals(Integer.MAX_VALUE, service.getMaxBarnsPerColor());
        }

        @Test
        @DisplayName("Accessors return the configured values")
        void accessorsReturnCorrectValues() {
            BarnAssignmentService custom = new BarnAssignmentService(6, 5);
            assertEquals(6, custom.getBarnCapacity());
            assertEquals(5, custom.getMaxBarnsPerColor());
        }

        @Test
        @DisplayName("barnCapacity=0 throws IllegalArgumentException")
        void zeroBarnCapacityThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BarnAssignmentService(0, 3));
        }

        @Test
        @DisplayName("maxBarnsPerColor=0 throws IllegalArgumentException")
        void zeroMaxBarnsThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BarnAssignmentService(4, 0));
        }

        @Test
        @DisplayName("Negative barnCapacity throws IllegalArgumentException")
        void negativeBarnCapacityThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> new BarnAssignmentService(-1, 3));
        }
    }

    @Nested
    @DisplayName("Scaling Tests (unbounded default)")
    class ScalingTests {

        @Test
        @DisplayName("Default service scales to 25 barns for 100 same-color animals")
        void defaultScalesTo100Animals() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(100, "Green"));

            // 100 / 4 = 25 barns, no overflow
            long barnCount = result.stream()
                    .filter(b -> !b.getBarnName().equals("Unassigned"))
                    .count();
            assertEquals(25, barnCount);
            assertFalse(result.stream().anyMatch(b -> b.getBarnName().equals("Unassigned")),
                    "Default service must never produce an Unassigned entry");
        }

        @Test
        @DisplayName("Default service never produces Unassigned, regardless of input size")
        void defaultNeverOverflows() {
            for (int n : new int[]{1, 4, 13, 50, 201, 1_000}) {
                List<BarnAssignment> result = service.assignAnimals(nAnimals(n, "Purple"));
                assertFalse(
                        result.stream().anyMatch(b -> b.getBarnName().equals("Unassigned")),
                        "n=" + n + " should not produce Unassigned under the unbounded default");
            }
        }

        @Test
        @DisplayName("All barns under default cap respect per-barn capacity of 4")
        void defaultHonorsBarnCapacity() {
            List<BarnAssignment> result = service.assignAnimals(nAnimals(50, "Yellow"));

            for (BarnAssignment b : result) {
                assertTrue(b.getAnimals().size() <= 4,
                        b.getBarnName() + " exceeded capacity with " + b.getAnimals().size());
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests (full animals.json dataset, loaded from disk)")
    class IntegrationTests {

        /** Loads the shipped animals.json file via the real parser. */
        private List<Animal> loadDataset() throws FarmDataException {
            return JsonParser.parseAnimals("animals.json");
        }

        @Test
        @DisplayName("Shipped dataset parses without error")
        void datasetParses() throws FarmDataException {
            List<Animal> animals = loadDataset();
            assertFalse(animals.isEmpty(), "animals.json must not be empty");
        }

        @Test
        @DisplayName("Total animals assigned equals input size (no animals lost)")
        void noAnimalsLost() throws FarmDataException {
            List<Animal> dataset = loadDataset();
            List<BarnAssignment> result = service.assignAnimals(dataset);

            int totalAssigned = result.stream()
                    .mapToInt(b -> b.getAnimals().size())
                    .sum();
            assertEquals(dataset.size(), totalAssigned);
        }

        @Test
        @DisplayName("Default service produces no Unassigned for shipped dataset")
        void noOverflowUnderDefault() throws FarmDataException {
            List<BarnAssignment> result = service.assignAnimals(loadDataset());
            assertFalse(result.stream().anyMatch(b -> b.getBarnName().equals("Unassigned")));
        }

        @Test
        @DisplayName("Every barn in the output respects the per-barn capacity limit")
        void everyBarnRespectsCapacity() throws FarmDataException {
            List<BarnAssignment> result = service.assignAnimals(loadDataset());

            for (BarnAssignment b : result) {
                if (!b.getBarnName().equals("Unassigned")) {
                    assertTrue(b.getAnimals().size() <= 4,
                            b.getBarnName() + " has " + b.getAnimals().size() + " animals");
                }
            }
        }

        @Test
        @DisplayName("Barns of the same color differ in size by at most 1")
        void evenDistributionInvariantHoldsForShippedData() throws FarmDataException {
            List<BarnAssignment> result = service.assignAnimals(loadDataset());

            // Group sizes by color prefix, e.g. "Barn_Red_1", "Barn_Red_2" → [3, 3, 3]
            java.util.Map<String, List<Integer>> sizesByColor = new java.util.HashMap<>();
            for (BarnAssignment b : result) {
                if (b.getBarnName().equals("Unassigned")) continue;
                // Barn names look like "Barn_<Color>_<n>"; strip off the trailing "_<n>"
                String colorPrefix = b.getBarnName().replaceAll("_\\d+$", "");
                sizesByColor.computeIfAbsent(colorPrefix, k -> new ArrayList<>())
                            .add(b.getAnimals().size());
            }

            for (var entry : sizesByColor.entrySet()) {
                List<Integer> sizes = entry.getValue();
                int max = sizes.stream().mapToInt(Integer::intValue).max().orElse(0);
                int min = sizes.stream().mapToInt(Integer::intValue).min().orElse(0);
                assertTrue(max - min <= 1,
                        entry.getKey() + " size diff was " + (max - min) + " (sizes: " + sizes + ")");
            }
        }

        @Test
        @DisplayName("Output is sorted alphabetically by barn name")
        void outputIsSorted() throws FarmDataException {
            List<BarnAssignment> result = service.assignAnimals(loadDataset());

            List<String> names = result.stream()
                    .map(BarnAssignment::getBarnName)
                    .filter(n -> !n.equals("Unassigned"))
                    .toList();
            List<String> sorted = new ArrayList<>(names);
            java.util.Collections.sort(sorted);
            assertEquals(sorted, names, "Barns must be alphabetically sorted");
        }
    }
}