package de.goerke.tobias.anchorj.util;

import java.lang.reflect.Array;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Provides some basic functions to handle arrays.
 */
public final class ArrayUtils {

    private ArrayUtils() {
    }

    /**
     * Appends a column to an existing table
     *
     * @param values the initial table
     * @param column the column to be appended
     * @param <T>    the type of the table
     * @return the new table
     */
    public static <T> T[][] appendColumn(T[][] values, T[] column) {
        T[][] result = create2dTypedArray(values, values.length);
        for (int i = 0; i < result.length; i++) {
            T[] subResult = createTypedArray(values, values[i].length + 1);
            for (int j = 0; j < values[i].length; j++) {
                subResult[j] = values[i][j];
            }
            subResult[values[i].length] = column[i];
            result[i] = subResult;
        }
        return result;
    }

    /**
     * Reads all values from a specific table column
     *
     * @param values the table
     * @param column index of the column to be read
     * @param <T>    the type of the table
     * @return the column extracted
     */
    public static <T> T[] extractColumn(T[][] values, int column) {
        T[] result = createTypedArray(values, values.length);
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i][column];
        }
        return result;
    }

    /**
     * Reads all values from a specific table column
     *
     * @param values the table
     * @param column index of the column to be read
     * @return the column extracted
     */
    public static Integer[] extractIntegerColumn(Object[][] values, int column) {
        Integer[] result = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (Integer) values[i][column];
        }
        return result;
    }

    /**
     * Removes a specific column index from a table
     *
     * @param values the table
     * @param column the column index to be removed
     * @return the table without the removed column
     */
    public static Object[][] removeColumn(Object[][] values, int column) {
        Object[][] result = new Object[values.length][];
        for (int i = 0; i < result.length; i++) {
            Object[] subResult = new Object[values[i].length - 1];
            int currentIndex = 0;
            for (int j = 0; j < values[i].length; j++) {
                if (j != column)
                    subResult[currentIndex] = values[i][j];
                currentIndex++;
            }
            result[i] = subResult;
        }
        return result;
    }


    /**
     * Tries to convert an object table containing integer values only to an integer table
     *
     * @param values the object table
     * @return the {@link Object} table
     */
    public static Object[][] transformToIntArray(Object[][] values) {
        Object[][] result = new Object[values.length][];
        for (int i = 0; i < values.length; i++) {
            Object[] row = values[i];
            Object[] intRow = new Object[row.length];
            for (int j = 0; j < row.length; j++) {
                Object cell = row[j];
                if (cell instanceof Integer)
                    intRow[j] = cell;
                else {
                    try {
                        intRow[j] = Integer.valueOf((String) cell);
                    } catch (Exception e) {
                        // Just store the string
                        intRow[j] = cell;
                        //throw new IllegalArgumentException("Cell is not convertable");
                    }
                }
            }
            result[i] = intRow;
        }
        return result;
    }

    /**
     * Unboxes an array
     *
     * @param array the array
     * @return the "unboxed" array
     */
    public static int[] toPrimitiveArray(Integer[] array) {
        return Stream.of(array).mapToInt(i -> i).toArray();
    }

    /**
     * Unboxes an array
     *
     * @param array the array
     * @return the "unboxed" array
     */
    public static double[] toPrimitiveArray(Double[] array) {
        return Stream.of(array).mapToDouble(i -> i).toArray();
    }

    /**
     * Boxes an array
     *
     * @param array the array
     * @return the "boxed" array
     */
    public static Integer[] toBoxedArray(int[] array) {
        return IntStream.of(array).boxed().toArray(Integer[]::new);
    }

    /**
     * Boxes an array
     *
     * @param array the array
     * @return the "boxed" array
     */
    public static Double[][] toBoxedArray(double[][] array) {
        Double[][] result = new Double[array.length][];
        for (int i = 0; i < array.length; i++) {
            result[i] = new Double[array[i].length];
            for (int j = 0; j < array[i].length; j++)
                result[i][j] = array[i][j];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] createTypedArray(T[][] original, int length) {
        return (T[]) Array.newInstance(original[0][0].getClass(), length);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[][] create2dTypedArray(T[][] original, int length) {
        return (T[][]) Array.newInstance(original[0].getClass(), length);
    }


    /**
     * Overwrites an existing column in an object table
     *
     * @param values        the table in which values shall be replaced
     * @param replaceValues the values to be placed
     * @param targetColumn  the column to be overwritten
     */
    public static void replaceColumnValues(Object[][] values, int[] replaceValues, int targetColumn) {
        for (int i = 0; i < values.length; i++) {
            values[i][targetColumn] = replaceValues[i];
        }
    }
}
