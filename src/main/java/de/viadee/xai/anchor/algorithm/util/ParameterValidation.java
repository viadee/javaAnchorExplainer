package de.viadee.xai.anchor.algorithm.util;

import java.util.Collection;

/**
 * Utility class providing some common parameter validation methods
 */
public final class ParameterValidation {
    public final static String NULL_MESSAGE = " must not be null";
    public final static String NOT_PERCENTAGE_MESSAGE = " must be a value between 0 and 1";
    public final static String NEGATIVE_VALUE_MESSAGE = " must not be negative";
    public final static String COLLECTION_EMPTY_MESSAGE = " must not be null or empty";

    private ParameterValidation() {
    }

    /**
     * @param value the value
     * @return true, if value in [0;1]
     */
    public static boolean isPercentage(Number value) {
        return !(value.doubleValue() < 0 || value.doubleValue() > 1);
    }

    /**
     * @param value the value
     * @return true, if value &gt;= 0
     */
    public static boolean isUnsigned(Number value) {
        return value.doubleValue() >= 0;
    }

    /**
     * @param value the value
     * @return true, if value &gt; 0
     */
    public static boolean isPositive(Number value) {
        return value.doubleValue() > 0;
    }

    /**
     * @param collection the collection
     * @return true, if collection not null and no empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
}
