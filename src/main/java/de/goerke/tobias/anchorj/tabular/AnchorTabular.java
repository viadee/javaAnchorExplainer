package de.goerke.tobias.anchorj.tabular;

import java.util.*;
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
    private final Map<TabularFeature, Map<Object, Object>> mappings;
    private final TabularInstanceVisualizer tabularInstanceVisualizer;

    private AnchorTabular(final TabularInstanceList tabularInstances, final TabularFeature[] features,
                          final Map<TabularFeature, Map<Object, Object>> mappings,
                          final TabularInstanceVisualizer tabularInstanceVisualizer) {
        this.tabularInstances = tabularInstances;
        this.features = features;
        this.mappings = mappings;
        this.tabularInstanceVisualizer = tabularInstanceVisualizer;
    }

    @SuppressWarnings("unchecked")
    private static AnchorTabular preprocess(final Collection<String[]> dataCollection, Map<String, Integer> featureNames,
                                            List<ColumnDescription> columnDescription, final boolean doBalance) {
        // Read data to object
        Object[][] data = mapCollectionToArray(dataCollection);
        data = removeUnusedColumns(columnDescription, data);
        List<ColumnDescription> usedColumns = columnDescription.stream().filter(c -> c.isDoUse()).collect(Collectors.toList());

        // Create features the data set will ultimately consist of
        TabularFeature[] tabularFeatures = transformColumnDescriptionToFeatureDescription(usedColumns);

        // Apply transformation to used columns
        applyTransformations(data, usedColumns);

        // Store the mappings that were conducted in order to be able to reverse them later on
        Map<TabularFeature, Map<Object, Object>> mappings = transformDataAndCreateReverseTransformationMapping(data, usedColumns, tabularFeatures);

        // Convert to int array
        Object[][] dataAsInt = transformToIntArray(data);
        int[] transformedLabelColumn = null;

        // Split off labels if there are
        final List<ColumnDescription> targetColumns = usedColumns.stream().filter(c -> c.isTargetFeature()).collect(Collectors.toList());
        if (!targetColumns.isEmpty()) {
            int labelColumnIndex = usedColumns.indexOf(targetColumns.get(0));
            Object[] mappingResult = transformColumnToUniqueValues(dataAsInt, labelColumnIndex);
            transformedLabelColumn = (int[]) mappingResult[0];
            dataAsInt = removeColumn(dataAsInt, labelColumnIndex);
            tabularFeatures = Stream.of(tabularFeatures)
                    .filter(f -> !f.isTargetFeature()).toArray(TabularFeature[]::new);
        }

        TabularInstanceList instances = new TabularInstanceList(dataAsInt, transformedLabelColumn, featureNames, tabularFeatures);

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

    private static Map<TabularFeature, Map<Object, Object>> transformDataAndCreateReverseTransformationMapping(Object[][] data, List<ColumnDescription> usedColumns, TabularFeature[] finalFeatures) {
        Map<TabularFeature, Map<Object, Object>> mappings = new LinkedHashMap<>();

        // Transform categorical features to be in a range of 0..(distinct values)
        // Also discretize nominal values
        for (int i = 0; i < usedColumns.size(); i++) {
            ColumnDescription internalColumn = usedColumns.get(i);

            // Only categorize categorical features
            if (internalColumn.getColumnType() == TabularFeature.ColumnType.CATEGORICAL) {
                Object[] result = transformColumnToUniqueValues(data, i);
                mappings.put(finalFeatures[i], (Map<Object, Object>) result[1]);
                replaceColumnValues(data, (int[]) result[0], i);
            }

            // Discretize nominal values if discretizer given
            else if (internalColumn.getColumnType() == TabularFeature.ColumnType.NOMINAL && internalColumn.getDiscretizer() != null) {
                Number[] valuesToBeDiscretized = new Number[data.length];
                for (int j = 0; j < valuesToBeDiscretized.length; j++) {
                    if (data[j][i] instanceof String)
                        valuesToBeDiscretized[j] = Double.valueOf((String) data[j][i]);
                    else if (data[j][i] instanceof Integer)
                        valuesToBeDiscretized[j] = (Integer) data[j][i];
                    else
                        throw new IllegalArgumentException("Could not read nominal column");
                }
                Object[] discretizedValues = internalColumn.getDiscretizer().apply(valuesToBeDiscretized);
                // Discretized values come from a "range" of input values.
                Map<Object, Set<Number>> tmpMapping = new LinkedHashMap<>();
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

                Map<Object, Object> mapping = new LinkedHashMap<>();
                for (Map.Entry<Object, Set<Number>> entry : tmpMapping.entrySet()) {
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
    private static Object[][] removeUnusedColumns(List<ColumnDescription> columnDescription, Object[][] data) {
        List<Integer> unusedIndices = new ArrayList<>();
        for (int i = 0; i < columnDescription.size(); i++) {
            if (!columnDescription.get(i).isDoUse())
                unusedIndices.add(i);
        }
        // Remove unused columns
        if (!unusedIndices.isEmpty()) {
            data = removeColumns(data, unusedIndices);
        }
        return data;
    }


    private static TabularFeature[] transformColumnDescriptionToFeatureDescription(List<ColumnDescription> internalColumns) {
        List<ColumnDescription> usedFeatures = new ArrayList<>();
        for (ColumnDescription internalColumn : internalColumns) {
            if (internalColumn.isDoUse())
                usedFeatures.add(internalColumn);
        }

        TabularFeature[] result = new TabularFeature[usedFeatures.size()];
        for (int i = 0; i < result.length; i++) {
            ColumnDescription internalColumn = usedFeatures.get(i);
            TabularFeature.ColumnType type;
            if (internalColumn.getColumnType() == TabularFeature.ColumnType.NATIVE)
                type = TabularFeature.ColumnType.NATIVE;
            else if ((internalColumn.getColumnType() == TabularFeature.ColumnType.CATEGORICAL) ||
                    (internalColumn.getColumnType() == TabularFeature.ColumnType.NOMINAL && internalColumn.getDiscretizer() != null))
                type = TabularFeature.ColumnType.CATEGORICAL;
            else
                type = TabularFeature.ColumnType.NOMINAL;

            result[i] = new TabularFeature(type, internalColumn.getName(), internalColumn.isTargetFeature());
        }
        return result;
    }

    private static void applyTransformations(Object[][] data, List<ColumnDescription> internalColumns) {
        for (int i = 0; i < internalColumns.size(); i++) {
            Function<Object[], Object[]> transformation = internalColumns.get(i).getTransformation();
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
        return new Object[]{result, valueSet};
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

        final int rowLength = data.iterator().next().length;
        Object[][] convertedData = new Object[data.size()][];
        Iterator<String[]> dataIter = data.iterator();
        for (int i = 0; i < data.size(); i++) {
            Object[] dataRow = new Object[rowLength];
            String[] oldRow = dataIter.next();
            System.arraycopy(oldRow, 0, dataRow, 0, rowLength);
            convertedData[i] = dataRow;
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
    public Map<TabularFeature, Map<Object, Object>> getMappings() {
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
        private final List<ColumnDescription> columnDescriptions = new ArrayList<>();
        private boolean doBalance = false;
        private final Map<String, Integer> featureLabels = new HashMap<>();

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
            return this.build(dataCollection, false);
        }

        /**
         * Builds the configured instance
         *
         * @param dataCollection the data to be transformed
         * @param excludeFirst   exclude the first row. Helpful if it is the header row
         * @return the {@link AnchorTabular} instance
         */
        public AnchorTabular build(Collection<String[]> dataCollection, boolean excludeFirst) {
            //if (columnDescriptions.stream().noneMatch(c -> c.isTargetFeature))
            //    throw new IllegalArgumentException("Not target feature specified");
            for (String[] fileContent : dataCollection) {
                if (fileContent.length != columnDescriptions.size()) {
                    throw new IllegalArgumentException("InternalColumn count does not match loaded data's features. " +
                            fileContent.length + " vs " + columnDescriptions.size());
                }
            }

            if (excludeFirst) {
                final Iterator<String[]> iterator = dataCollection.iterator();
                iterator.next();
                iterator.remove();
            }

            return AnchorTabular.preprocess(dataCollection, this.featureLabels, this.columnDescriptions, this.doBalance);
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
            this.addColumn(new ColumnDescription(null, name, false, false, null, null));
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
            this.addColumn(new ColumnDescription(TabularFeature.ColumnType.CATEGORICAL, name, true, false, transformation, null));
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
            this.addColumn(new ColumnDescription(TabularFeature.ColumnType.NOMINAL, name, true, false, transformation, discretizer));
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
            this.addColumn(new ColumnDescription(TabularFeature.ColumnType.NOMINAL, name, true, false, null, discretizer));
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
            if (this.columnDescriptions.stream().anyMatch(ColumnDescription::isTargetFeature)) {
                throw new IllegalArgumentException("There is already a target column registered");
            }
            this.addColumn(new ColumnDescription(TabularFeature.ColumnType.CATEGORICAL, name, true, true, transformation, null));
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
            this.addColumn(new ColumnDescription(TabularFeature.ColumnType.NATIVE, name, true, false, null, null));
            return this;
        }

        private TabularPreprocessorBuilder addColumn(ColumnDescription description) {
            this.columnDescriptions.add(description);
            this.featureLabels.put(description.getName(), this.featureLabels.size());
            return this;
        }

        public List<ColumnDescription> getColumnDescriptions() {
            return columnDescriptions;
        }

        public boolean isDoBalance() {
            return doBalance;
        }
    }

}
