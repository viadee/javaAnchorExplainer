package de.goerke.tobias.anchorj.tabular;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.goerke.tobias.anchorj.base.LabeledInstanceList;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import static de.goerke.tobias.anchorj.util.ArrayUtils.appendColumn;
import static de.goerke.tobias.anchorj.util.ArrayUtils.extractIntegerColumn;
import static de.goerke.tobias.anchorj.util.ArrayUtils.removeColumn;
import static de.goerke.tobias.anchorj.util.ArrayUtils.toBoxedArray;
import static de.goerke.tobias.anchorj.util.ArrayUtils.toPrimitiveArray;
import static de.goerke.tobias.anchorj.util.ArrayUtils.transformToIntArray;

/**
 * Stores multiple instances of a {@link TabularInstance} and their corresponding labels
 */
public class TabularInstanceList extends LabeledInstanceList<TabularInstance> {
    private final TabularFeature[] features;
    private final Map<String, Integer> featureNames;

    /**
     * Constructs the instance.
     *
     * @param dataInstances the contained {@link TabularInstance}s
     * @param labels        the labels, one for each {@link TabularInstance}
     * @param features      the features. One for each column.
     */
    public TabularInstanceList(TabularInstance[] dataInstances, int[] labels, Map<String, Integer> featureNames,
                               TabularFeature[] features) {
        super(dataInstances, labels);
        if (getFeatureCount() != features.length)
            throw new IllegalArgumentException("Feature count of data instances must equal features");
        this.features = features;
        this.featureNames = featureNames;
    }

    /**
     * Constructs the instance.
     *
     * @param table    an object table to be converted to the array of {@link TabularInstance}s
     * @param labels   the labels, one for each {@link TabularInstance}
     * @param features the features. One for each column.
     */
    public TabularInstanceList(Object[][] table, int[] labels, Map<String, Integer> featureNames, TabularFeature[] features) {
        this(Stream.of(transformToIntArray(table)).map(entry -> new TabularInstance(featureNames, entry))
                .toArray(TabularInstance[]::new), labels, featureNames, features);
    }

    /**
     * Copying constructor.
     *
     * @param copyList the list whose values to copy.
     */
    public TabularInstanceList(TabularInstanceList copyList) {
        this(copyList.dataInstances, copyList.labels, copyList.featureNames, copyList.features);
    }

    private static Object[][] balanceDataset(Object[][] values, Integer[] labels) {
        Map<Integer, List<Object[]>> labelToRow = new HashMap<>();
        Map<Integer, Integer> labelDistribution = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            int label = labels[i];
            int count;
            List<Object[]> rows;
            if (labelDistribution.containsKey(label)) {
                count = labelDistribution.get(label);
                rows = labelToRow.get(label);
            } else {
                count = 0;
                rows = new ArrayList<>();
            }
            labelDistribution.put(label, count + 1);
            rows.add(values[i]);
            labelToRow.put(label, rows);
        }

        List<Object[]> rearranged = new ArrayList<>();
        int rarestLabelCount = labelDistribution.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue))
                .orElseThrow(IllegalArgumentException::new).getValue();
        for (int uniqueLabel : labelDistribution.keySet()) {
            List<Object[]> applicableRows = labelToRow.get(uniqueLabel);
            rearranged.addAll(applicableRows.stream().limit(rarestLabelCount).collect(Collectors.toList()));
        }
        Collections.shuffle(rearranged);
        return rearranged.toArray(new Object[rearranged.size()][]);
    }

    /**
     * Truncates the set of instances so that each label has the same amount of instances
     *
     * @return the balanced data list
     */
    TabularInstanceList balance() {
        // Balancing = for each label have the same amount of entries
        // We reattach the labels as balancing shuffles the table, which would lead to our labels being faulty
        Object[][] table = asArray();
        table = appendColumn(table, toBoxedArray(labels));
        table = balanceDataset(table, toBoxedArray(labels));
        // Split labels off again
        int[] newLabels = toPrimitiveArray(extractIntegerColumn(table, table[0].length - 1));
        table = removeColumn(table, table[0].length - 1);
        return new TabularInstanceList(Stream.of(table).map(entry -> new TabularInstance(this.featureNames, entry))
                .toArray(TabularInstance[]::new), newLabels, this.featureNames, this.features);
    }

    /**
     * @return an UnmodifiableList of the contained features
     */
    public List<TabularFeature> getFeatures() {
        return Collections.unmodifiableList(Arrays.asList(features));
    }

    private Object[][] asArray() {
        return Stream.of(dataInstances).map(TabularInstance::getInstance).toArray(Object[][]::new);
    }

    /**
     * Shuffles and splits this list into two.
     * <p>
     * Useful e.g. if splitting test and validation lists
     *
     * @param percentageSize the percentage of entries the first list will contain
     * @return the split lists
     */
    public List<TabularInstanceList> shuffleSplitList(double percentageSize) {
        return shuffleSplitList(percentageSize, new Random());
    }

    /**
     * Shuffles and splits this list into two.
     * <p>
     * Useful e.g. if splitting test and validation lists
     *
     * @param percentageSize the percentage of entries the first list will contain
     * @param rnd            the Random to be used
     * @return the split lists
     */
    public List<TabularInstanceList> shuffleSplitList(double percentageSize, Random rnd) {
        if (!ParameterValidation.isPercentage(percentageSize))
            throw new IllegalArgumentException("Percentage size" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);

        Object[][] array = asArray();
        array = appendColumn(array, toBoxedArray(labels));
        List<Object[]> shuffledList = Arrays.asList(array);
        Collections.shuffle(shuffledList, rnd);
        final int splitIndex = shuffledList.size() - (int) (shuffledList.size() * percentageSize) - 1;
        Object[][] beforeSplitArray = shuffledList.subList(0, splitIndex).toArray(new Object[0][]);
        Object[][] afterSplitArray = shuffledList.subList(splitIndex, shuffledList.size()).toArray(new Object[0][]);
        int[] beforeSplitLabels = toPrimitiveArray(extractIntegerColumn(beforeSplitArray, featureCount));
        int[] afterSplitLabels = toPrimitiveArray(extractIntegerColumn(afterSplitArray, featureCount));
        beforeSplitArray = removeColumn(beforeSplitArray, featureCount);
        afterSplitArray = removeColumn(afterSplitArray, featureCount);

        return (Arrays.asList(
                new TabularInstanceList(beforeSplitArray, beforeSplitLabels, this.featureNames, this.features),
                new TabularInstanceList(afterSplitArray, afterSplitLabels, this.featureNames, this.features)
        ));
    }

}
