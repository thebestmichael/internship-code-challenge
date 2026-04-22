package com.logicgate.farm.util;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

/**
 * A {@link DefaultPrettyPrinter} variant that emits {@code "key": "value"}
 * (no space before the colon) instead of Jackson's default {@code "key" : "value"}.
 *
 * <p>This matches the JSON format shown in the challenge's example output,
 * removing slight cosmetic difference, matching example exactly. The JSON is semantically
 * identical either way.
 */
public class CompactPrettyPrinter extends DefaultPrettyPrinter {

    public CompactPrettyPrinter() {
        super();
        // Configure separators so object field colons have a trailing space only:
        //   default : `"key" : "value"`  (space before AND after the colon)
        //   compact : `"key": "value"`  (space after only)
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER);
        this._separators = separators;
        this._objectFieldValueSeparatorWithSpaces =
                separators.getObjectFieldValueSeparator() + " ";
    }

    @Override
    public DefaultPrettyPrinter createInstance() {
        return new CompactPrettyPrinter();
    }
}