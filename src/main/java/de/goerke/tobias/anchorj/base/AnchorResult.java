package de.goerke.tobias.anchorj.base;

/**
 * Represents a result of the Anchors algorithm.
 *
 * @param <T> the type of the data instance
 */
public class AnchorResult<T extends DataInstance<?>> extends AnchorCandidate {
    private final T instance;
    private final int label;
    private final boolean isAnchor;

    /**
     * Constructs the instance
     *
     * @param candidate the {@link AnchorCandidate}
     * @param instance  the instance described
     * @param label     the instance's label
     * @param isAnchor  if true, marks the result is an anchor and adheres to the set constraints
     */
    AnchorResult(AnchorCandidate candidate, T instance, int label, boolean isAnchor) {
        super(candidate.getOrderedFeatures(), candidate.getParentCandidate());
        super.setCoverage(candidate.getCoverage());
        this.registerSamples(candidate.getSampledSize(), candidate.getPositiveSamples());
        this.instance = instance;
        this.label = label;
        this.isAnchor = isAnchor;
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

    /**
     * @return if true, the result is an anchor and subject to the set constraints.
     * if false, this result is the candidate with the best precision
     */
    public boolean isAnchor() {
        return isAnchor;
    }
}
