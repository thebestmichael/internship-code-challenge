package com.logicgate.farm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.logicgate.farm.exception.FarmDataException;
import com.logicgate.farm.model.Animal;
import com.logicgate.farm.model.BarnAssignment;
import com.logicgate.farm.service.BarnAssignmentService;
import com.logicgate.farm.util.CompactPrettyPrinter;
import com.logicgate.farm.util.JsonParser;

import java.util.List;

/**
 * Entry point for the LogicGate Farm Barn Assignment program.
 *
 * <h2>Usage</h2>
 * <pre>
 *   # Default (reads the animals.json from the current directory)
 *   java -jar farm-challenge.jar
 *
 *   # Custom file path
 *   java -jar farm-challenge.jar /path/to/animals.json
 * </pre>
 *
 * <h2>Output</h2>
 * Prints a pretty-printed JSON array of barn assignment objects to stdout,
 * using {@link CompactPrettyPrinter} so the output matches the spec's
 * {@code "key": "value"} spacing. All error messages go to stderr so stdout
 * can be piped cleanly.
 *
 * <h2>Exit Codes</h2>
 * <ul>
 *   <li>{@code 0} – success</li>
 *   <li>{@code 1} – data error (file not found, malformed JSON, validation failure)</li>
 *   <li>{@code 2} – unexpected runtime error</li>
 * </ul>
 */
public class Main {

    /** Fallback input file used when no CLI argument is provided. */
    private static final String DEFAULT_INPUT_FILE = "animals.json";

    /**
     * Application entry point.
     *
     * @param args optional: args[0] is the path to the animals.json file
     */
    public static void main(String[] args) {
        String inputFile = (args.length > 0) ? args[0] : DEFAULT_INPUT_FILE;

        try {
            // 1. Parse and validate the input JSON file.
            List<Animal> animals = JsonParser.parseAnimals(inputFile);

            // 2. Distribute animals into barns using production defaults.
            BarnAssignmentService service = new BarnAssignmentService();
            List<BarnAssignment> assignments = service.assignAnimals(animals);

            // 3. Serialize the result using the spec-matching pretty printer.
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writer(new CompactPrettyPrinter());
            System.out.println(writer.writeValueAsString(assignments));

        } catch (FarmDataException e) {
            System.err.println("[ERROR] Farm data problem: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected failure: " + e.getMessage());
            System.exit(2);
        }
    }
}