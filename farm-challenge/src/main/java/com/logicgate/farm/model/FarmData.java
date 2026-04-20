package com.logicgate.farm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the root structure of the animals.json file.
 *
 * <p>The JSON file contains a single top-level object with a "livestock"
 * array. Jackson deserializes the entire file into this wrapper class,
 * which holds the list of {@link Animal} entries.
 *
 * <p>Example input:
 * <pre>{@code
 * {
 *   "livestock": [
 *     {"animal": "Goat", "barn": "Green"},
 *     ...
 *   ]
 * }
 * }</pre>
 */
public class FarmData {

    /** The full list of animals read from the "livestock" JSON array. */
    @JsonProperty("livestock")
    private List<Animal> livestock;

    /** No-arg constructor required by Jackson for deserialization. */
    public FarmData() {}

    public List<Animal> getLivestock()              { return livestock; }
    public void setLivestock(List<Animal> livestock) { this.livestock = livestock; }
}
