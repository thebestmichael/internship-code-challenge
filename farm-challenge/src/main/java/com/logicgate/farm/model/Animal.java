package com.logicgate.farm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single animal entry from the animals.json file.
 *
 * <p>Each animal has a name (e.g., "Goat") and a preferred barn color
 * (e.g., "Green"). Jackson uses the {@code @JsonProperty} annotations
 * to map the JSON field names "animal" and "barn" to these Java fields.
 */
public class Animal {

    /** The animal's species/name as it appears in the JSON (field: "animal"). */
    @JsonProperty("animal")
    private String name;

    /** The barn color this animal prefers (field: "barn"). */
    @JsonProperty("barn")
    private String barnColor;

    /** No-arg constructor required by Jackson for deserialization. */
    public Animal() {}

    /**
     * Convenience constructor for use in tests and service logic.
     *
     * @param name      the animal species name
     * @param barnColor the preferred barn color
     */
    public Animal(String name, String barnColor) {
        this.name = name;
        this.barnColor = barnColor;
    }

    public String getName()      { return name; }
    public String getBarnColor() { return barnColor; }

    public void setName(String name)           { this.name = name; }
    public void setBarnColor(String barnColor) { this.barnColor = barnColor; }

    @Override
    public String toString() {
        return "Animal{name='" + name + "', barnColor='" + barnColor + "'}";
    }
}
