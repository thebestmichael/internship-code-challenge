package com.logicgate.farm.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logicgate.farm.exception.FarmDataException;
import com.logicgate.farm.model.Animal;
import com.logicgate.farm.model.FarmData;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Utility class that reads and validates the animals.json input file.
 *
 * <p>All parsing is handled by Jackson's {@link ObjectMapper}. After the
 * deserialization, a strict validation pass ensures that every required field
 * is present and non-blank before the data is handed to the service layer.
 *
 * <p>This class is stateless and all methods are static.
 */
public class JsonParser {

    /** Shared, thread-safe ObjectMapper instance (Jackson is safe to reuse). */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Private constructor: utility class, never instantiated. */
    private JsonParser() {}

    /**
     * Parses the file at {@code filePath} and returns a validated list of animals.
     *
     * <p>Validation checks performed (in order):
     * <ol>
     *   <li>File exists on disk</li>
     *   <li>JSON is syntactically valid and maps to {@link FarmData}</li>
     *   <li>{@code "livestock"} field is present and non-null</li>
     *   <li>{@code "livestock"} array is non-empty</li>
     *   <li>Every entry has a non-blank {@code "animal"} field</li>
     *   <li>Every entry has a non-blank {@code "barn"} field</li>
     * </ol>
     *
     * @param filePath path to the animals.json file
     * @return validated, immutable-view list of {@link Animal} objects
     * @throws FarmDataException if the file is missing, malformed, or fails validation
     */
    public static List<Animal> parseAnimals(String filePath) throws FarmDataException {
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FarmDataException("Input file not found: " + filePath);
        }

        FarmData farmData;
        try {
            farmData = MAPPER.readValue(file, FarmData.class);
        } catch (IOException e) {
            // Jackson throws IOException for both I/O and JSON syntax errors.
            throw new FarmDataException("Failed to parse JSON file '" + filePath
                    + "': " + e.getMessage(), e);
        }

        validate(farmData);
        return farmData.getLivestock();
    }

    /**
     * Validates a deserialized {@link FarmData} object.
     *
     * <p>Checks for null/empty lists and walks every animal to ensure
     * the "animal" and "barn" fields are present and non-blank.
     *
     * @param farmData the object to validate
     * @throws FarmDataException if any validation rule is violated
     */
    private static void validate(FarmData farmData) throws FarmDataException {
        if (farmData == null) {
            throw new FarmDataException("The JSON root object is null or empty.");
        }
        if (farmData.getLivestock() == null) {
            throw new FarmDataException("Missing required field: 'livestock'.");
        }
        if (farmData.getLivestock().isEmpty()) {
            throw new FarmDataException("The 'livestock' array is empty — nothing to process.");
        }

        List<Animal> animals = farmData.getLivestock();
        for (int i = 0; i < animals.size(); i++) {
            Animal a = animals.get(i);

            if (a == null) {
                throw new FarmDataException(
                        "Null entry in 'livestock' array at index " + i + ".");
            }
            if (a.getName() == null || a.getName().isBlank()) {
                throw new FarmDataException(
                        "Missing or blank 'animal' field at index " + i + ".");
            }
            if (a.getBarnColor() == null || a.getBarnColor().isBlank()) {
                throw new FarmDataException(
                        "Missing or blank 'barn' field at index " + i + ".");
            }
        }
    }
}
