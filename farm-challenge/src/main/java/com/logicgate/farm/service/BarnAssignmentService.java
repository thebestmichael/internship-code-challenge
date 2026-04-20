package com.logicgate.farm.service;

import com.logicgate.farm.model.Animal;
import com.logicgate.farm.model.BarnAssignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core service that distributes animals from the livestock list into
 * colored barns, enforcing capacity limits, even distribution, and overflow.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li><b>Color grouping</b> - Animals are grouped by their preferred barn color.
 *       Color matching is case-insensitive; output barn names use Title Case.</li>
 *   <li><b>Barn capacity</b> - Each barn holds at most {@code barnCapacity} animals
 *       (default: 4).</li>
 *   <li><b>Barn limit per color</b> - At most {@code maxBarnsPerColor} barns are
 *       created per color (default: 3).</li>
 *   <li><b>Even distribution (bonus)</b> - Animals are spread as evenly as possible
 *       across barns of the same color. The size difference between any two barns of
 *       the same color is at most 1. Larger barns come first.</li>
 *   <li><b>Overflow (bonus)</b> - Animals that exceed total barn capacity for their
 *       color ({@code barnCapacity × maxBarnsPerColor}) are collected in a special
 *       {@code "Unassigned"} entry appended at the end of the output.</li>
 * </ol>
 *
 * <h2>Time Complexity</h2>
 * <ul>
 *   <li>Grouping: O(n) - single pass over the livestock list.</li>
 *   <li>Distribution: O(n) - each animal is sliced into a barn exactly once.</li>
 *   <li>Sorting: O(k log k) where k = number of distinct barns created.</li>
 *   <li><b>Overall: O(n + k log k)</b> - effectively O(n) for realistic datasets
 *       where k ≪ n.</li>
 * </ul>
 */
public class BarnAssignmentService {

    /** Maximum number of animals each barn can hold (configurable). */
    private final int barnCapacity;

    /** Maximum number of barns allowed per color group (configurable). */
    private final int maxBarnsPerColor;

    /**
     * Creates a service with production defaults:
     * {@code barnCapacity = 4}, {@code maxBarnsPerColor = 3}.
     */
    public BarnAssignmentService() {
        this(4, 3);
    }

    /**
     * Creates a service with custom limits, useful for testing or
     * deployment configurations.
     *
     * @param barnCapacity     maximum animals per barn; must be ≥ 1
     * @param maxBarnsPerColor maximum barns per color; must be ≥ 1
     * @throws IllegalArgumentException if either parameter is less than 1
     */
    public BarnAssignmentService(int barnCapacity, int maxBarnsPerColor) {
        if (barnCapacity < 1) {
            throw new IllegalArgumentException("barnCapacity must be >= 1, got: " + barnCapacity);
        }
        if (maxBarnsPerColor < 1) {
            throw new IllegalArgumentException(
                    "maxBarnsPerColor must be >= 1, got: " + maxBarnsPerColor);
        }
        this.barnCapacity     = barnCapacity;
        this.maxBarnsPerColor = maxBarnsPerColor;
    }

    /**
     * Assigns every animal in the list to a barn, returning the complete
     * barn roster as an ordered list of {@link BarnAssignment} objects.
     *
     * <p>The returned list is sorted alphabetically by barn name, with the
     * optional {@code "Unassigned"} entry always last.
     *
     * @param animals the livestock to distribute; must not be null
     * @return sorted list of barn assignments (never null, may be empty)
     */
    public List<BarnAssignment> assignAnimals(List<Animal> animals) {
        // Step 1: Group animal names by their normalized color key
        Map<String, List<String>> animalsByColor = groupByColor(animals);

        List<BarnAssignment> assignments  = new ArrayList<>();
        List<String>         allOverflow  = new ArrayList<>();

        // Step 2: Process each color group independently
        for (Map.Entry<String, List<String>> entry : animalsByColor.entrySet()) {
            String       colorKey    = entry.getKey();           // normalized lowercase
            List<String> animalNames = entry.getValue();

            int totalCapacity = barnCapacity * maxBarnsPerColor;

            // Split into animals that fit and those that overflow.
            List<String> fitsInBarn = animalNames.subList(0, Math.min(animalNames.size(), totalCapacity));
            List<String> overflow   = animalNames.subList(fitsInBarn.size(), animalNames.size());

            allOverflow.addAll(overflow);

            // Step 3: Distribute fitting animals evenly across barns
            String capitalizedColor = capitalize(colorKey);
            assignments.addAll(distributeEvenly(capitalizedColor, fitsInBarn));
        }

        // Step 4: Sort all regular barn entries alphabetically
        assignments.sort(Comparator.comparing(BarnAssignment::getBarnName));

        // Step 5: Append overflow entry at the very end (if any)
        if (!allOverflow.isEmpty()) {
            assignments.add(new BarnAssignment("Unassigned", allOverflow));
        }

        return assignments;
    }

    /**
     * Groups animal names by their barn color, preserving the encounter order
     * of both colors and animals within each color.
     *
     * <p>Color keys are normalized to lowercase so that "Green", "GREEN", and
     * "green" all map to the same group. A {@link LinkedHashMap} preserves the
     * first-seen ordering of colors, which keeps the output deterministic.
     *
     * <p>Time complexity: O(n).
     *
     * @param animals the full livestock list
     * @return map of lowercase color -> ordered list of animal names
     */
    private Map<String, List<String>> groupByColor(List<Animal> animals) {
        Map<String, List<String>> grouped = new LinkedHashMap<>();

        for (Animal animal : animals) {
            String colorKey = animal.getBarnColor().toLowerCase();
            // computeIfAbsent avoids a separate containsKey check (O(1) amortized).
            grouped.computeIfAbsent(colorKey, k -> new ArrayList<>())
                   .add(animal.getName());
        }

        return grouped;
    }

    /**
     * Distributes {@code animalNames} evenly across the minimum number of barns
     * required, capped at {@code maxBarnsPerColor}.
     *
     * <h3>Even-distribution algorithm</h3>
     * <pre>
     *   numBarns  = min( ceil(total / barnCapacity), maxBarnsPerColor )
     *   base      = total / numBarns          (integer division)
     *   remainder = total % numBarns
     *
     *   Barn i (1-indexed):
     *     size = base + 1   if i <= remainder
     *          = base        otherwise
     * </pre>
     * This guarantees the maximum difference between any two barn sizes is 1,
     * and that larger barns always appear first (matching the challenge example).
     *
     * <p>Time complexity: O(m) where m = number of fitting animals.
     *
     * @param color       Title-Case color name used in barn labels (e.g., "Green")
     * @param animalNames animals guaranteed to fit within total barn capacity
     * @return list of BarnAssignment objects for this color group
     */
    private List<BarnAssignment> distributeEvenly(String color, List<String> animalNames) {
        int total    = animalNames.size();
        int numBarns = Math.min(
                (int) Math.ceil((double) total / barnCapacity),
                maxBarnsPerColor
        );

        int baseCount = total / numBarns;  // minimum animals every barn receives
        int remainder = total % numBarns;  // this many barns receive one extra

        List<BarnAssignment> barns = new ArrayList<>(numBarns);
        int cursor = 0;

        for (int barnIndex = 1; barnIndex <= numBarns; barnIndex++) {
            // First `remainder` barns get baseCount + 1; the rest get baseCount.
            int sliceSize = baseCount + (barnIndex <= remainder ? 1 : 0);

            List<String> slice = new ArrayList<>(
                    animalNames.subList(cursor, cursor + sliceSize));

            barns.add(new BarnAssignment("Barn_" + color + "_" + barnIndex, slice));
            cursor += sliceSize;
        }

        return barns;
    }

    /**
     * Converts the first character of {@code s} to uppercase and the rest to
     * lowercase, producing a consistent Title-Case color name for barn labels.
     *
     * <p>Examples: {@code "green" → "Green"}, {@code "RED" → "Red"}.
     *
     * @param s the string to capitalize; must not be null or empty
     * @return Title-Case version of the input
     */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // Accessors ( will be used in tests to verify configuration)

    /** Returns the configured maximum capacity per barn. */
    public int getBarnCapacity()     { return barnCapacity; }

    /** Returns the configured maximum number of barns per color. */
    public int getMaxBarnsPerColor() { return maxBarnsPerColor; }
}
