package de.goerke.tobias.anchorj.base;

/**
 * Represents a result of the Anchors algorithm.
 *
 * @param <T> the type of the data instance
 */
public class AnchorResult<T extends DataInstance<?>> extends AnchorCandidate {
    private final T instance;
    private final int label;

    /**
     * Constructs the instance
     *
     * @param candidate the {@link AnchorCandidate}
     * @param instance  the instance described
     * @param label     the instance's label
     */
    AnchorResult(AnchorCandidate candidate, T instance, int label) {
        super(candidate.getOrderedFeatures(), candidate.getParentCandidate());
        super.setCoverage(candidate.getCoverage());
        this.registerSamples(candidate.getSampledSize(), candidate.getPositiveSamples());
        this.instance = instance;
        this.label = label;
    }

    /**
     * @return the described instance
     */
    public T getInstance() {
        return instance;
    }

    /**
     * @return the described instance's label
     */
    public int getLabel() {
        return label;
    }
}
