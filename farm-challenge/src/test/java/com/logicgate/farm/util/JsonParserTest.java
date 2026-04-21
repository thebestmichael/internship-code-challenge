package com.logicgate.farm.util;

import com.logicgate.farm.exception.FarmDataException;
import com.logicgate.farm.model.Animal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonParser}.
 *
 * <p>Uses JUnit 5's {@code @TempDir} to create temporary files so that
 * every test is fully isolated and leaves no artifacts on disk.
 */
class JsonParserTest {

    /** JUnit 5 injects a fresh temporary directory for each test class run. */
    @TempDir
    Path tempDir;

    // ── Helper ───────────────────────────────────────────────────────────────

    /** Writes {@code content} to a temp file and returns its absolute path. */
    private String writeTempFile(String filename, String content) throws IOException {
        Path file = tempDir.resolve(filename);
        Files.writeString(file, content);
        return file.toAbsolutePath().toString();
    }

    // ── Happy-path tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid JSON with two animals parses correctly")
    void validJson_ParsesAnimals() throws Exception {
        String path = writeTempFile("valid.json",
                """
                {
                  "livestock": [
                    {"animal": "Goat", "barn": "Green"},
                    {"animal": "Cow",  "barn": "Red"}
                  ]
                }
                """);

        List<Animal> animals = JsonParser.parseAnimals(path);

        assertEquals(2, animals.size());
        assertEquals("Goat",  animals.get(0).getName());
        assertEquals("Green", animals.get(0).getBarnColor());
        assertEquals("Cow",   animals.get(1).getName());
        assertEquals("Red",   animals.get(1).getBarnColor());
    }

    @Test
    @DisplayName("Valid JSON with one animal parses correctly")
    void singleAnimal_ParsesCorrectly() throws Exception {
        String path = writeTempFile("single.json",
                """
                {"livestock": [{"animal": "Duck", "barn": "Blue"}]}
                """);

        List<Animal> result = JsonParser.parseAnimals(path);
        assertEquals(1, result.size());
        assertEquals("Duck", result.get(0).getName());
        assertEquals("Blue", result.get(0).getBarnColor());
    }

    // ── File-not-found tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Non-existent file path throws FarmDataException")
    void missingFile_ThrowsFarmDataException() {
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals("does_not_exist.json"));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // ── Malformed JSON tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Completely invalid JSON throws FarmDataException")
    void malformedJson_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("malformed.json", "{ this is : not : valid json }");
        assertThrows(FarmDataException.class, () -> JsonParser.parseAnimals(path));
    }

    @Test
    @DisplayName("Empty file throws FarmDataException")
    void emptyFile_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("empty.json", "");
        assertThrows(FarmDataException.class, () -> JsonParser.parseAnimals(path));
    }

    @Test
    @DisplayName("JSON with wrong root type (array instead of object) throws FarmDataException")
    void wrongRootType_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("array.json", "[1, 2, 3]");
        assertThrows(FarmDataException.class, () -> JsonParser.parseAnimals(path));
    }

    // ── Missing / empty livestock tests ──────────────────────────────────────

    @Test
    @DisplayName("Missing 'livestock' field throws FarmDataException")
    void missingLivestockField_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("no-livestock.json", "{\"other\": []}");
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals(path));
        assertTrue(ex.getMessage().contains("livestock"));
    }

    @Test
    @DisplayName("Empty 'livestock' array throws FarmDataException")
    void emptyLivestock_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("empty-livestock.json", "{\"livestock\": []}");
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals(path));
        assertTrue(ex.getMessage().toLowerCase().contains("empty"));
    }

    // ── Missing / blank field tests ───────────────────────────────────────────

    @Test
    @DisplayName("Animal entry missing 'animal' field throws FarmDataException")
    void missingAnimalField_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("no-animal.json",
                """
                {"livestock": [{"barn": "Green"}]}
                """);
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals(path));
        assertTrue(ex.getMessage().contains("'animal'"));
    }

    @Test
    @DisplayName("Animal entry missing 'barn' field throws FarmDataException")
    void missingBarnField_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("no-barn.json",
                """
                {"livestock": [{"animal": "Goat"}]}
                """);
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals(path));
        assertTrue(ex.getMessage().contains("'barn'"));
    }

    @Test
    @DisplayName("Blank 'animal' value throws FarmDataException")
    void blankAnimalValue_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("blank-animal.json",
                """
                {"livestock": [{"animal": "  ", "barn": "Green"}]}
                """);
        assertThrows(FarmDataException.class, () -> JsonParser.parseAnimals(path));
    }

    @Test
    @DisplayName("Blank 'barn' value throws FarmDataException")
    void blankBarnValue_ThrowsFarmDataException() throws IOException {
        String path = writeTempFile("blank-barn.json",
                """
                {"livestock": [{"animal": "Goat", "barn": ""}]}
                """);
        assertThrows(FarmDataException.class, () -> JsonParser.parseAnimals(path));
    }

    // ── Error message quality ────────────────────────────────────────────────

    @Test
    @DisplayName("Validation error message includes the offending array index")
    void errorMessageIncludesIndex() throws IOException {
        String path = writeTempFile("bad-index.json",
                """
                {
                  "livestock": [
                    {"animal": "Goat", "barn": "Green"},
                    {"animal": "Cow",  "barn": ""}
                  ]
                }
                """);
        FarmDataException ex = assertThrows(FarmDataException.class,
                () -> JsonParser.parseAnimals(path));
        // Should mention index 1 (the second entry)
        assertTrue(ex.getMessage().contains("1"),
                "Error message should reference the failing index");
    }
}
