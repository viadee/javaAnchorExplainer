package de.goerke.tobias.anchorj.base.coverage;

import org.junit.jupiter.api.Test;

import java.util.AbstractSet;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the {@link DisabledCoverageIdentification} to return correct values for respective input size sets
 */
class DisabledCoverageIdentificationTest {

    @Test
    void calculateCoverage() {
        DisabledCoverageIdentification disabledCoverageIdentification = new DisabledCoverageIdentification();
        final Number[][] expectedReturnValues = new Number[][]{{0, 1}, {1, 0.5}, {2, 0.25}, {3, 0.125}};
        for (final Number[] number : expectedReturnValues) {
            assertEquals(disabledCoverageIdentification.calculateCoverage(new AbstractSet<Integer>() {
                @Override
                public Iterator<Integer> iterator() {
                    return null;
                }

                @Override
                public int size() {
                    return number[0].intValue();
                }
            }), number[1].doubleValue());
        }
    }
}