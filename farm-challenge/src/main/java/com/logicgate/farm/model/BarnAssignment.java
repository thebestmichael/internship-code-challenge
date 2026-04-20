package com.logicgate.farm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a single barn entry in the program's JSON output.
 *
 * <p>Each barn has a name (e.g., "Barn_Green_1") and the list of animal
 * names assigned to it. Jackson serializes these fields using the
 * {@code @JsonProperty} annotations to produce the exact output format
 * required by the challenge.
 *
 * <p>Example output entry:
 * <pre>{@code
 * {"barn": "Barn_Green_1", "animals": ["Duck", "Goat", "Chicken"]}
 * }</pre>
 *
 * <p>The special barn name {@code "Unassigned"} is used to collect any
 * animals that could not be placed due to barn capacity limits.
 */
public class BarnAssignment {

    /** Barn identifier, e.g., "Barn_Green_1" or "Unassigned". */
    @JsonProperty("barn")
    private final String barnName;

    /** Ordered list of animal names assigned to this barn. */
    @JsonProperty("animals")
    private final List<String> animals;

    /**
     * Constructs a BarnAssignment with a name and its animal roster.
     *
     * @param barnName the barn's display name
     * @param animals  the animals housed in this barn
     */
    public BarnAssignment(String barnName, List<String> animals) {
        this.barnName = barnName;
        this.animals  = animals;
    }

    public String       getBarnName() { return barnName; }
    public List<String> getAnimals()  { return animals; }
}
