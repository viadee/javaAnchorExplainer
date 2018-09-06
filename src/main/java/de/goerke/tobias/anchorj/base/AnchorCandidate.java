package de.goerke.tobias.anchorj.base;

import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.io.Serializable;
import java.util.*;

/**
 * Representation of an Anchor candidate.
 * <p>
 * An Anchor is defined by the features is comprises.
 * <p>
 * This class is not completely immutable but thread-safe!
 */
public class AnchorCandidate implements Serializable {
    /*
     * Immutable fields
     */
    private final List<Integer> orderedFeatures;
    private final SortedSet<Integer> canonicalFeatures;
    private final AnchorCandidate parentCandidate;

    /*
     * Mutable fields. Write-access only allowed by synchronized registerSamples!
     */
    private Double coverage;
    private int sampledSize = 0;
    private int positiveSamples = 0;
    private double precision = 0;

    /**
     * Constructs the candidate and sets its immutable features and coverage.
     *
     * @param features        the features the candidate comprises
     * @param parentCandidate the parent candidate this rule has been derived from
     */
    public AnchorCandidate(final Collection<Integer> features, final AnchorCandidate parentCandidate) {
        if (!ParameterValidation.isNotEmpty(features))
            throw new IllegalArgumentException("Candidate" + ParameterValidation.COLLECTION_EMPTY_MESSAGE);
        if (parentCandidate == null && features.size() != 1)
            throw new IllegalArgumentException("No parent candidate specified");
        if (parentCandidate != null && (features.size() != parentCandidate.canonicalFeatures.size() + 1))
            throw new IllegalArgumentException("Parent candidate must have n-1 features");

        // Candidates get "normalized" as treeSet is automatically sorted
        this.orderedFeatures = Collections.unmodifiableList(new ArrayList<>(features));
        this.canonicalFeatures = Collections.unmodifiableSortedSet(new TreeSet<>(features));

        this.parentCandidate = parentCandidate;
    }

    /**
     * Updates the precision of the candidate when new samples were taken.
     * <p>
     * Synchronized access to mutable variables!
     *
     * @param sampleSize      the amount of performed evaluations
     * @param positiveSamples the amount of correctly identified evaluations
     */
    public synchronized void registerSamples(final int sampleSize, final int positiveSamples) {
        if (!ParameterValidation.isUnsigned(sampleSize))
            throw new IllegalArgumentException("Sampled size" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);
        if (!ParameterValidation.isUnsigned(positiveSamples))
            throw new IllegalArgumentException("Positive" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);
        if (positiveSamples > sampleSize)
            throw new IllegalArgumentException("Positive samples must be smaller or equal to sample size");

        this.sampledSize += sampleSize;
        this.positiveSamples += positiveSamples;
        this.precision = (this.sampledSize == 0) ? 0 : this.positiveSamples / (double) this.sampledSize;
    }

    /**
     * @return an UnmodifiableList of the contained features
     * in the order they were constructed
     */
    public List<Integer> getOrderedFeatures() {
        // Is immutable anyways
        return orderedFeatures;
    }

    /**
     * @return an UnmodifiableSortedSet of the canonical features
     */
    public SortedSet<Integer> getCanonicalFeatures() {
        // Immutable too
        return canonicalFeatures;
    }

    /**
     * @return current precision of the anchor
     */
    public double getPrecision() {
        return precision;
    }

    /**
     * @return coverage of the anchor. May be null if not already set.
     */
    public Double getCoverage() {
        return coverage;
    }

    /**
     * Sets the coverage of the anchor.
     * <p>
     * Its coverage may only be set once as it makes no sense of recalculating it.
     * <p>
     * It makes sense not to calculate the coverage at construction time, as some anchors' coverage will never need to
     * be calculated at all.
     * <p>
     * Furthermore, in some scenarios, calculating coverage is expensive.
     * <p>
     * As this method is only being called from synchronous methods, no synchronization is needed.
     *
     * @param coverage the coverage
     */
    public void setCoverage(double coverage) {
        if (this.coverage != null)
            throw new IllegalArgumentException("Coverage has already been set!");
        if (!ParameterValidation.isPercentage(coverage))
            throw new IllegalArgumentException("Coverage" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        this.coverage = coverage;
    }

    /**
     * May be used to lazily evaluate a candidate's coverage
     *
     * @return true, if coverage has not yet been calculated
     */
    boolean isCoverageUndefined() {
        return coverage == null;
    }

    /**
     * @return samples taken so far
     */
    public int getSampledSize() {
        return sampledSize;
    }

    /**
     * @return amount of correct predictions so far
     */
    public int getPositiveSamples() {
        return positiveSamples;
    }

    /**
     * May be used to get the parent this rule has been derived from.
     * <p>
     * Useful to get the added feature precision this anchor provides.
     *
     * @return the parent this rule has been derived from
     */
    public AnchorCandidate getParentCandidate() {
        return parentCandidate;
    }

    /**
     * When adding features to a rule, the precision usually increases (due to random sampling it might decrease).
     *
     * @return the increased precision value this candidate provides over his parent.
     */
    public double getAddedPrecision() {
        double parentPrecision = (parentCandidate == null) ? 0 : parentCandidate.getPrecision();
        return precision - parentPrecision;
    }

    /**
     * When adding a feature to a rule, the precision usually decreases.
     * <p>
     * This method returns this as a negative value.
     *
     * @return the reduced coverage in comparison to this candidate's parent
     */
    public double getAddedCoverage() {
        double parentCoverage = (parentCandidate == null) ? 1 : parentCandidate.getCoverage();
        return coverage - parentCoverage;
    }

    /**
     * When adding a feature to a rule, the precision usually decreases.
     * <p>
     * However, coverage shouldn't be compared in absolutes.
     * <p>
     * This method returns this as a negative value.
     *
     * @return the reduced coverage in comparison to this candidate's parent
     */
    public double getAddedCoverageInPercent() {
        double parentCoverage = (parentCandidate == null) ? 1 : parentCandidate.getCoverage();
        return coverage / parentCoverage;
    }

    @Override
    public String toString() {
        return "AnchorCandidate {" +
                "features=" + canonicalFeatures +
                ", ordering=" + orderedFeatures +
                ", precision=" + precision +
                ", coverage=" + coverage +
                ", sampledSize=" + sampledSize +
                ", positiveSamples=" + positiveSamples +
                '}';
    }
}