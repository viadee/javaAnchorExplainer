package de.viadee.xai.anchor.algorithm;

import java.util.Objects;

/**
 * Represents a result of the Anchors algorithm.
 *
 * @param <T> the type of the data instance
 */
public class AnchorResult<T extends DataInstance<?>> extends AnchorCandidate {
    private static final long serialVersionUID = -1378097055362567551L;

    private final T instance;
    private final int explainedInstanceLabel;
    private final boolean isAnchor;
    private final double timeSpent;
    private final double timeSpentSampling;

    /**
     * Constructs the instance
     *
     * @param candidate              the {@link AnchorCandidate}
     * @param instance               the instance described
     * @param explainedInstanceLabel the instance's explainedInstanceLabel
     * @param isAnchor               if true, marks the result is an anchor and adheres to the set constraints
     * @param timeSpent              the total time spent constructing the result
     * @param timeSpentSampling      the total time spent sampling and evaluating candidates
     */
    public AnchorResult(AnchorCandidate candidate, T instance, int explainedInstanceLabel, boolean isAnchor,
                        double timeSpent, double timeSpentSampling) {
        super(candidate.getOrderedFeatures(), candidate.getParentCandidate());
        super.setCoverage(candidate.getCoverage());
        this.registerSamples(candidate.getSampledSize(), candidate.getPositiveSamples());
        this.instance = instance;
        this.explainedInstanceLabel = explainedInstanceLabel;
        this.isAnchor = isAnchor;
        this.timeSpent = timeSpent;
        this.timeSpentSampling = timeSpentSampling;
    }

    /**
     * @return the described instance
     */
    public T getInstance() {
        return instance;
    }

    /**
     * @return the described instance's explainedInstanceLabel
     */
    public int getExplainedInstanceLabel() {
        return explainedInstanceLabel;
    }

    /**
     * @return if true, the result is an anchor and subject to the set constraints.
     * if false, this result is the candidate with the best precision
     */
    public boolean isAnchor() {
        return isAnchor;
    }

    /**
     * @return the total time spent constructing the result in milliseconds
     */
    public double getTimeSpent() {
        return timeSpent;
    }

    /**
     * @return the total time spent sampling and evaluating candidates in milliseconds
     */
    public double getTimeSpentSampling() {
        return timeSpentSampling;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnchorResult<?> that = (AnchorResult<?>) o;
        return explainedInstanceLabel == that.explainedInstanceLabel &&
                isAnchor == that.isAnchor &&
                Double.compare(that.timeSpent, timeSpent) == 0 &&
                Double.compare(that.timeSpentSampling, timeSpentSampling) == 0 &&
                Objects.equals(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), instance, explainedInstanceLabel, isAnchor, timeSpent, timeSpentSampling);
    }

}
