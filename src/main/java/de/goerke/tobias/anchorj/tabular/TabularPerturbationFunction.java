package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.PerturbationFunction;

import java.util.*;

/**
 * Implementation of the {@link PerturbationFunction} for arbitrary tabular datasets.
 * <p>
 * Uses a set of {@link TabularInstance}s to randomly return, e.g. the testing set.
 * <p>
 * Creating a custom tabular perturbation function is highly application specific.
 */
public class TabularPerturbationFunction implements PerturbationFunction<TabularInstance> {

    private final TabularInstance instance;
    private final TabularInstance[] perturbationData;

    /**
     * Constructs the instance.
     *
     * @param instance         the {@link TabularInstance} to perturb.
     * @param perturbationData an array of data to generate perturbations from
     */
    public TabularPerturbationFunction(TabularInstance instance, TabularInstance[] perturbationData) {
        if (perturbationData == null || perturbationData.length < 1)
            throw new IllegalArgumentException("Perturbation data must have at least one element");
        this.instance = instance;
        this.perturbationData = perturbationData;
    }


    @Override
    public PerturbationFunction<TabularInstance> createForInstance(TabularInstance instance) {
        return new TabularPerturbationFunction(instance, perturbationData);
    }

    @Override
    public PerturbationResult<TabularInstance> perturb(Set<Integer> immutableFeaturesIdx, int nrPerturbations) {
        // Extend list until space is large enough
        List<TabularInstance> shuffledPerturbations = new ArrayList<>();
        while (shuffledPerturbations.size() < nrPerturbations)
            shuffledPerturbations.addAll(Arrays.asList(perturbationData));
        Collections.shuffle(shuffledPerturbations);

        List<TabularInstance> rawResult = new ArrayList<>();
        List<boolean[]> featuresChanged = new ArrayList<>();
        for (int i = 0; i < nrPerturbations; i++) {
            TabularInstance perturbedInstance = shuffledPerturbations.get(i);
            for (Integer featureId : immutableFeaturesIdx) {
                // Copy all fixed features
                perturbedInstance = new TabularInstance(perturbedInstance);
                perturbedInstance.getInstance()[featureId] = instance.getInstance()[featureId];
            }
            rawResult.add(perturbedInstance);
            boolean[] tempFeatureChanged = new boolean[perturbedInstance.getFeatureCount()];
            for (int j = 0; j < tempFeatureChanged.length; j++) {
                tempFeatureChanged[j] = !instance.getInstance()[j].equals(perturbedInstance.getInstance()[j]);
            }
            featuresChanged.add(tempFeatureChanged);
        }

        return new PerturbationResult<>(rawResult.toArray(new TabularInstance[0]),
                featuresChanged.toArray(new boolean[0][]));
    }

}
