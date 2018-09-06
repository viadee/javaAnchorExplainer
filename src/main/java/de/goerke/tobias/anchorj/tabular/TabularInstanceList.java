package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.LabeledInstanceList;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.goerke.tobias.anchorj.util.ArrayUtils.*;

/**
 * Stores multiple instances of a {@link TabularInstance} and their corresponding labels
 */
public class TabularInstanceList extends LabeledInstanceList<TabularInstance> {
    private final TabularFeature[] features;

    /**
     * Constructs the instance.
     *
     * @param dataInstances the contained {@link TabularInstance}s
     * @param labels        the labels, one for each {@link TabularInstance}
     * @param features      the features. One for each column.
     */
    public TabularInstanceList(TabularInstance[] dataInstances, int[] labels,
                               TabularFeature[] features) {
        super(dataInstances, labels);
        if (getFeatureCount() != features.length)
            throw new IllegalArgumentException("Feature count of data instances must equal features");
        this.features = features;
    }

    /**
     * Constructs the instance.
     *
     * @param table    an integer table to be converted to the array of {@link TabularInstance}s
     * @param labels   the labels, one for each {@link TabularInstance}
     * @param features the features. One for each column.
     */
    public TabularInstanceList(Integer[][] table, int[] labels, TabularFeature[] features) {
        this(Stream.of(toIntArray(table)).map(TabularInstance::new).toArray(TabularInstance[]::new), labels, features);
    }

    /**
     * Copying constructor.
     *
     * @param copyList the list whose values to copy.
     */
    public TabularInstanceList(TabularInstanceList copyList) {
        this(copyList.dataInstances, copyList.labels, copyList.features);
    }

    /**
     * Truncates the set of instances so that each label has the same amount of instances
     *
     * @return the balanced data list
     */
    TabularInstanceList balance() {
        // Balancing = for each label have the same amount of entries
        // We reattach the labels as balancing shuffles the table, which would lead to our labels being faulty
        Integer[][] table = asArray();
        table = appendColumn(table, toBoxedArray(labels));
        table = balanceDataset(table, toBoxedArray(labels));
        // Split labels off again
        int[] newLabels = toPrimitiveArray(extractColumn(table, table[0].length - 1));
        table = removeColumn(table, table[0].length - 1);
        return new TabularInstanceList(Stream.of(table).map(TabularInstance::new).toArray(TabularInstance[]::new),
                newLabels, features);
    }


    /**
     * @return an UnmodifiableList of the contained features
     */
    public List<TabularFeature> getFeatures() {
        return Collections.unmodifiableList(Arrays.asList(features));
    }

    private Integer[][] asArray() {
        return Stream.of(dataInstances).map(TabularInstance::getInstance).toArray(Integer[][]::new);
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
        if (!ParameterValidation.isPercentage(percentageSize))
            throw new IllegalArgumentException("Percentage size" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);

        Integer[][] array = asArray();
        array = appendColumn(array, toBoxedArray(labels));
        List<Integer[]> shuffledList = Arrays.asList(array);
        Collections.shuffle(shuffledList);
        final int splitIndex = shuffledList.size() - (int) (shuffledList.size() * percentageSize) - 1;
        Integer[][] beforeSplitArray = shuffledList.subList(0, splitIndex).toArray(new Integer[0][]);
        Integer[][] afterSplitArray = shuffledList.subList(splitIndex, shuffledList.size()).toArray(new Integer[0][]);
        int[] beforeSplitLabels = toPrimitiveArray(extractColumn(beforeSplitArray, featureCount));
        int[] afterSplitLabels = toPrimitiveArray(extractColumn(afterSplitArray, featureCount));
        beforeSplitArray = removeColumn(beforeSplitArray, featureCount);
        afterSplitArray = removeColumn(afterSplitArray, featureCount);

        return (Arrays.asList(
                new TabularInstanceList(beforeSplitArray, beforeSplitLabels, features),
                new TabularInstanceList(afterSplitArray, afterSplitLabels, features)
        ));
    }

    private static Integer[][] balanceDataset(Integer[][] values, Integer[] labels) {
        Map<Integer, List<Integer[]>> labelToRow = new HashMap<>();
        Map<Integer, Integer> labelDistribution = new HashMap<>();
        for (int i = 0; i < labels.length; i++) {
            int label = labels[i];
            int count;
            List<Integer[]> rows;
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

        List<Integer[]> rearranged = new ArrayList<>();
        int rarestLabelCount = labelDistribution.entrySet().stream().min(Comparator.comparingInt(Map.Entry::getValue)).get().getValue();
        for (int uniqueLabel : labelDistribution.keySet()) {
            List<Integer[]> applicableRows = labelToRow.get(uniqueLabel);
            rearranged.addAll(applicableRows.stream().limit(rarestLabelCount).collect(Collectors.toList()));
        }
        Collections.shuffle(rearranged);
        return rearranged.toArray(new Integer[rearranged.size()][]);
    }

}
