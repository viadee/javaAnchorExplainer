package de.goerke.tobias.anchorj.tabular;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.goerke.tobias.anchorj.util.ArrayUtils.removeColumn;
import static de.goerke.tobias.anchorj.util.ArrayUtils.replaceColumnValues;
import static de.goerke.tobias.anchorj.util.ArrayUtils.transformToIntArray;

/**
 * Provides default means to use the Anchors algorithm on tabular data
 * <p>
 * To make use of this, use the {@link AnchorTabular.TabularPreprocessorBuilder} to create an instance of this class.
 */
public class AnchorTabular {

    private final TabularInstanceList tabularInstances;
    private final TabularFeature[] features;
    private final Map<TabularFeature, Map<Object, Integer>> mappings;
    private final TabularInstanceVisualizer tabularInstanceVisualizer;

    private AnchorTabular(final TabularInstanceList tabularInstances, final TabularFeature[] features,
                          final Map<TabularFeature, Map<Object, Integer>> mappings,
                          final TabularInstanceVisualizer tabularInstanceVisualizer) {
        this.tabularInstances = tabularInstances;
        this.features = features;
        this.mappings = mappings;
        this.tabularInstanceVisualizer = tabularInstanceVisualizer;
    }

    @SuppressWarnings("unchecked")
    private static AnchorTabular preprocess(final Collection<String[]> dataCollection,
                                            List<InternalColumn> columnDescription, final boolean doBalance) {
        // Read data to object
        Object[][] data = mapCollectionToArray(dataCollection);
        data = removeUnusedColumns(columnDescription, data);
        List<InternalColumn> usedColumns = columnDescription.stream().filter(c -> c.doUse).collect(Collectors.toList());

        // Create features the data set will ultimately consist of
        TabularFeature[] tabularFeatures = transformColumnDescriptionToFeatureDescription(usedColumns);

        // Apply transformation to used columns
        applyTransformations(data, usedColumns);

        // Store the mappings that were conducted in order to be able to reverse them later on
        Map<TabularFeature, Map<Object, Integer>> mappings = transformDataAndCreateReverseTransformationMapping(data, usedColumns, tabularFeatures);

        // Convert to int array
        Object[][] dataAsInt = transformToIntArray(data);
        int[] transformedLabelColumn = null;

        // Split off labels if there are
        final List<InternalColumn> targetColumns = usedColumns.stream().filter(c -> c.isTargetFeature).collect(Collectors.toList());
        if (!targetColumns.isEmpty()) {
            int labelColumnIndex = usedColumns.indexOf(targetColumns.get(0));
            Object[] mappingResult = transformColumnToUniqueValues(dataAsInt, labelColumnIndex);
            transformedLabelColumn = (int[]) mappingResult[0];
            dataAsInt = removeColumn(dataAsInt, labelColumnIndex);
            tabularFeatures = Stream.of(tabularFeatures)
                    .filter(f -> !f.isTargetFeature()).toArray(TabularFeature[]::new);
        }

        TabularInstanceList instances = new TabularInstanceList(dataAsInt, transformedLabelColumn, tabularFeatures);

        // Balancing = for each label have the same amount of entries
        if (doBalance && transformedLabelColumn == null) {
            throw new IllegalArgumentException("Cannot balance when no target column is specified");
        }
        if (doBalance) {
            instances = instances.balance();
        }

        // Create the result explainer
        TabularInstanceVisualizer tabularInstanceVisualizer = new TabularInstanceVisualizer(mappings);

        return new AnchorTabular(instances, tabularFeatures, mappings, tabularInstanceVisualizer);
    }

    private static Map<TabularFeature, Map<Object, Integer>> transformDataAndCreateReverseTransformationMapping(Object[][] data, List<InternalColumn> usedColumns, TabularFeature[] finalFeatures) {
        Map<TabularFeature, Map<Object, Integer>> mappings = new LinkedHashMap<>();

        // Transform categorical features to be in a range of 0..(distinct values)
        // Also discretize nominal values
        for (int i = 0; i < usedColumns.size(); i++) {
            InternalColumn internalColumn = usedColumns.get(i);

            // Only categorize categorical features
            if (internalColumn.columnType == TabularFeature.ColumnType.CATEGORICAL) {
                Object[] result = transformColumnToUniqueValues(data, i);
                mappings.put(finalFeatures[i], (Map<Object, Integer>) result[1]);
                replaceColumnValues(data, (int[]) result[0], i);
            }

            // Discretize nominal values if discretizer given
            else if (internalColumn.columnType == TabularFeature.ColumnType.NOMINAL && internalColumn.discretizer != null) {
                Number[] valuesToBeDiscretized = new Number[data.length];
                for (int j = 0; j < valuesToBeDiscretized.length; j++) {
                    if (data[j][i] instanceof String)
                        valuesToBeDiscretized[j] = Double.valueOf((String) data[j][i]);
                    else if (data[j][i] instanceof Integer)
                        valuesToBeDiscretized[j] = (Integer) data[j][i];
                    else
                        throw new IllegalArgumentException("Could not read nominal column");
                }
                Integer[] discretizedValues = internalColumn.discretizer.apply(valuesToBeDiscretized);
                // Discretized values come from a "range" of input values.
                Map<Integer, Set<Number>> tmpMapping = new LinkedHashMap<>();
                for (int j = 0; j < discretizedValues.length; j++) {
                    data[j][i] = discretizedValues[j];

                    Set<Number> values;
                    if (tmpMapping.containsKey(discretizedValues[j]))
                        values = tmpMapping.get(discretizedValues[j]);
                    else {
                        values = new HashSet<>();
                        tmpMapping.put(discretizedValues[j], values);
                    }
                    values.add(valuesToBeDiscretized[j]);
                }

                Map<Object, Integer> mapping = new LinkedHashMap<>();
                for (Map.Entry<Integer, Set<Number>> entry : tmpMapping.entrySet()) {
                    Set<Integer> values = entry.getValue().stream().map(Number::intValue).collect(Collectors.toSet());
                    String range = String.format("Range(%d, %d)", Collections.min(values), Collections.max(values));
                    mapping.put(range, entry.getKey());
                }
                mappings.put(finalFeatures[i], mapping);
            } else {
                mappings.put(finalFeatures[i], Collections.emptyMap());
            }
        }
        return mappings;
    }

    /**
     * Iterates through the column description and removes all ignored columns.
     *
     * @param columnDescription list of columns
     * @param data              the 2D data array
     * @return the data without ignored columns
     */
    private static Object[][] removeUnusedColumns(List<InternalColumn> columnDescription, Object[][] data) {
        List<Integer> unusedIndices = new ArrayList<>();
        for (int i = 0; i < columnDescription.size(); i++) {
            if (!columnDescription.get(i).doUse)
                unusedIndices.add(i);
        }
        // Remove unused columns
        if (!unusedIndices.isEmpty()) {
            data = removeColumns(data, unusedIndices);
        }
        return data;
    }


    private static TabularFeature[] transformColumnDescriptionToFeatureDescription(List<InternalColumn> internalColumns) {
        List<InternalColumn> usedFeatures = new ArrayList<>();
        for (InternalColumn internalColumn : internalColumns) {
            if (internalColumn.doUse)
                usedFeatures.add(internalColumn);
        }

        TabularFeature[] result = new TabularFeature[usedFeatures.size()];
        for (int i = 0; i < result.length; i++) {
            InternalColumn internalColumn = usedFeatures.get(i);
            TabularFeature.ColumnType type;
            if (internalColumn.columnType == TabularFeature.ColumnType.NATIVE)
                type = TabularFeature.ColumnType.NATIVE;
            else if ((internalColumn.columnType == TabularFeature.ColumnType.CATEGORICAL) ||
                    (internalColumn.columnType == TabularFeature.ColumnType.NOMINAL && internalColumn.discretizer != null))
                type = TabularFeature.ColumnType.CATEGORICAL;
            else
                type = TabularFeature.ColumnType.NOMINAL;

            result[i] = new TabularFeature(type, internalColumn.name, internalColumn.isTargetFeature);
        }
        return result;
    }

    private static void applyTransformations(Object[][] data, List<InternalColumn> internalColumns) {
        for (int i = 0; i < internalColumns.size(); i++) {
            Function<Object[], Object[]> transformation = internalColumns.get(i).transformation;
            if (transformation == null)
                continue;
            Object[] column = new Object[data.length];
            for (int j = 0; j < data.length; j++) {
                column[j] = data[j][i];
            }
            Object[] transformationResult = transformation.apply(column);
            for (int j = 0; j < data.length; j++) {
                data[j][i] = transformationResult[j];
            }
        }
    }


    /**
     * Transforms an object column to an int column, where each class of unique objects has the same id
     *
     * @param values       the values
     * @param targetColumn its column index
     * @return an int array
     */
    private static Object[] transformColumnToUniqueValues(Object[][] values, int targetColumn) {
        int[] result = new int[values.length];
        Map<Object, Integer> valueSet = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i++) {
            Object cell = values[i][targetColumn];
            if (!valueSet.containsKey(cell)) {
                valueSet.put(cell, valueSet.size());
            }
            result[i] = valueSet.get(cell);
        }
        return new Object[] { result, valueSet };
    }

    private static Object[][] removeColumns(Object[][] values, List<Integer> indices) {
        Object[][] result = new Object[values.length][];
        for (int i = 0; i < result.length; i++) {
            Object[] subResult = new Object[values[i].length - indices.size()];
            int currentIndex = 0;
            for (int j = 0; j < values[i].length; j++) {
                if (indices.contains(j))
                    continue;
                subResult[currentIndex++] = values[i][j];
            }
            result[i] = subResult;
        }
        return result;
    }

    private static Object[][] mapCollectionToArray(Collection<String[]> data) {
        if (data.size() < 1 || data.stream().mapToInt(d -> d.length).distinct().count() != 1)
            throw new RuntimeException("No data submitted or rows are differently sized");

        Object[][] convertedData = new Object[data.size()][data.iterator().next().length];
        Iterator<String[]> dataIter = data.iterator();
        for (int i = 0; i < data.size(); i++) {
            convertedData[i] = dataIter.next();
        }
        return convertedData;
    }

    /**
     * @return the contained instance list
     */
    public TabularInstanceList getTabularInstances() {
        return tabularInstances;
    }

    /**
     * @return an UnmodifiableList of the contained features
     */
    public List<TabularFeature> getFeatures() {
        return Collections.unmodifiableList(Arrays.asList(features));
    }

    /**
     * @return a {@link Map} mapping, for each feature, which value got replaced by which other value during
     * preprocessing
     */
    public Map<TabularFeature, Map<Object, Integer>> getMappings() {
        return mappings;
    }

    /**
     * @return an instance of the {@link TabularInstanceVisualizer} to visualize explanations and instances
     */
    public TabularInstanceVisualizer getVisualizer() {
        return tabularInstanceVisualizer;
    }

    /**
     * Used to construct an {@link AnchorTabular} instance.
     * <p>
     * The addColumn operations must be called as many times as there are columns in the submitted dataset.
     */
    public static class TabularPreprocessorBuilder {
        private final List<InternalColumn> internalColumns = new ArrayList<>();
        private boolean doBalance = false;

        /**
         * Constructs the builder
         */
        public TabularPreprocessorBuilder() {
        }

        /**
         * Builds the configured instance
         *
         * @return the {@link AnchorTabular} instance
         */
        public AnchorTabular build(Collection<String[]> dataCollection) {
            //if (internalColumns.stream().noneMatch(c -> c.isTargetFeature))
            //    throw new IllegalArgumentException("Not target feature specified");
            for (String[] fileContent : dataCollection)
                if (fileContent.length != internalColumns.size())
                    throw new IllegalArgumentException("InternalColumn count does not match loaded data's features. " +
                            fileContent.length + " vs " + internalColumns.size());
            return AnchorTabular.preprocess(dataCollection, internalColumns, doBalance);
        }

        /**
         * May be used to configure the preprocessor to balance the dataset, i.e. to truncate the set of instances so
         * that each label has the same amount of instances
         *
         * @param doBalance true, if to balance dataset
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder setDoBalance(boolean doBalance) {
            this.doBalance = doBalance;
            return this;
        }

        /**
         * Adds a column that will be ignored.
         * Useful if processing a e.g. CSV file where some columns shall be disregarded
         *
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addIgnoredColumn() {
            return addIgnoredColumn(null);
        }

        /**
         * Adds a column that will be ignored.
         * Useful if processing a e.g. CSV file where some columns shall be disregarded
         *
         * @param name the name of the column
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addIgnoredColumn(String name) {
            this.internalColumns.add(new InternalColumn(null, name, false, false, null, null));
            return this;
        }

        /**
         * Adds a categorical column.
         *
         * @param name           the name of the column
         * @param transformation a {@link Function} transforming a column's values. May be used to normalize data.
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addCategoricalColumn(String name, Function<Object[], Object[]> transformation) {
            this.internalColumns.add(new InternalColumn(TabularFeature.ColumnType.CATEGORICAL, name, true, false, transformation, null));
            return this;
        }

        /**
         * Adds a categorical column.
         *
         * @param name the name of the column
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addCategoricalColumn(String name) {
            return this.addCategoricalColumn(name, null);
        }

        /**
         * Adds a nominal column.
         *
         * @param name           the name of the column
         * @param transformation a {@link Function} transforming a column's values. May be used to normalize data.
         * @param discretizer    a {@link Function} used to discretize data, i.e. transforming nominal data
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addNominalColumn(String name, Function<Object[], Object[]> transformation, Function<Number[], Integer[]> discretizer) {
            this.internalColumns.add(new InternalColumn(TabularFeature.ColumnType.NOMINAL, name, true, false, transformation, discretizer));
            return this;
        }

        /**
         * Adds a nominal column.
         *
         * @param name        the name of the column
         * @param discretizer a {@link Function} used to discretize data, i.e. transforming nominal data
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addNominalColumn(String name, Function<Number[], Integer[]> discretizer) {
            this.internalColumns.add(new InternalColumn(TabularFeature.ColumnType.NOMINAL, name, true, false, null, discretizer));
            return this;
        }

        /**
         * Adds a nominal column.
         *
         * @param name the name of the column
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addNominalColumn(String name) {
            return this.addNominalColumn(name, null, null);
        }

        /**
         * Specifies the target column, i.e. the one containing the predicted label.
         * May one be used once per build operation.
         *
         * @param name           the name of the column
         * @param transformation a {@link Function} transforming a column's values. May be used to normalize data.
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addTargetColumn(String name, Function<Object[], Object[]> transformation) {
            if (this.internalColumns.stream().anyMatch(c -> c.isTargetFeature))
                throw new IllegalArgumentException("There is already a target column registered");
            this.internalColumns.add(new InternalColumn(TabularFeature.ColumnType.CATEGORICAL, name, true, true, transformation, null));
            return this;
        }

        /**
         * Specifies the target column, i.e. the one containing the predicted label.
         * May one be used once per build operation.
         *
         * @param name the name of the column
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addTargetColumn(String name) {
            return this.addTargetColumn(name, null);
        }

        /**
         * Adds a column that does retain its type during preprocessing and does not get converted at any time
         *
         * @param name the column name
         * @return the current {@link TabularPreprocessorBuilder}'s instance
         */
        public TabularPreprocessorBuilder addObjectColumn(String name) {
            this.internalColumns.add(new InternalColumn(TabularFeature.ColumnType.NATIVE, name, true, false, null, null));
            return this;
        }
    }

    private static class InternalColumn {
        private final TabularFeature.ColumnType columnType;
        private final String name;
        private final boolean doUse;
        private final boolean isTargetFeature;
        private final Function<Object[], Object[]> transformation;
        private final Function<Number[], Integer[]> discretizer;

        private InternalColumn(TabularFeature.ColumnType columnType, String name, boolean doUse,
                               boolean isTargetFeature, Function<Object[], Object[]> transformation,
                               Function<Number[], Integer[]> discretizer) {
            this.columnType = columnType;
            this.name = name;
            this.doUse = doUse;
            this.isTargetFeature = isTargetFeature;
            this.transformation = transformation;
            this.discretizer = discretizer;
        }
    }

}
