package de.goerke.tobias.anchorj.tabular;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Discretizer partitioning data into n specified classes using their mean values as a class label
 */
public class PercentileMedianDiscretizer implements Function<Number[], Integer[]> {

    private final int classCount;

    /**
     * Creates the discretizer.
     *
     * @param classCount the amount of classes to use
     */
    public PercentileMedianDiscretizer(int classCount) {
        this.classCount = classCount;
    }

    private static double medianIndexValue(List<Number> list) {
        if (list.size() % 2 == 0)
            return (list.get(list.size() / 2).doubleValue() + list.get(list.size() / 2 - 1).doubleValue()) / 2;
        else
            return list.get(list.size() / 2).doubleValue();
    }

    @Override
    public Integer[] apply(final Number[] initialInput) {
        final List<Number> numbers = new ArrayList<>(Arrays.asList(initialInput));
        numbers.sort(Comparator.comparingDouble(Number::doubleValue));
        final Map<Number, Integer> medianValueMapping = new HashMap<>();
        final int classes = Math.min(classCount, numbers.size());
        final int countPerClass = numbers.size() / classes;
        int backlog = numbers.size() % classes;
        int endIndex = 0;
        for (int currentClass = 0; currentClass < classes; currentClass++) {
            final int startIndex = endIndex;
            endIndex = startIndex + countPerClass;
            if (backlog > 0) {
                endIndex++;
                backlog--;
            }
            List<Number> sublist = numbers.subList(startIndex, endIndex);
            final double medianValue = medianIndexValue(sublist);
            for (Number num : sublist) {
                if (!medianValueMapping.containsKey(num))
                    medianValueMapping.put(num, (int) medianValue);
            }

        }
        return Stream.of(initialInput).mapToInt(medianValueMapping::get).boxed().toArray(Integer[]::new);
    }
}
