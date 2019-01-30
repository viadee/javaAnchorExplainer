package de.viadee.xai.anchor.algorithm.coverage;

import java.io.Serializable;
import java.util.Set;

import de.viadee.xai.anchor.algorithm.AnchorCandidate;

/**
 * Interface defining the operations to identify a candidate's coverage.
 * <p>
 * Formally, we define the coverage of an anchor as the probability that it applies to samples from D,
 * i.e. cov(A) = E_{D(z)}[A(z)] (Ribeiro 2018).
 * <p>
 * For most scenarios, the {@link PerturbationBasedCoverageIdentification} is sufficient. This is also the method
 * that has initially been proposed by Ribeiro.
 * <p>
 * However, in some scenarios, e.g. for images, the coverage cannot be identified this easily.
 * Base on the notion that some use-cases require specialized coverage functions, this interface may be used to define
 * custom coverage identification methods.
 */
public interface CoverageIdentification extends Serializable {

    /**
     * Calculates a feature set's coverage.
     * <p>
     * This method will be called at most one time per anchor.
     *
     * @param featureSet the feature set contained by e.g. an {@link AnchorCandidate}
     * @return the calculated coverage
     */
    double calculateCoverage(final Set<Integer> featureSet);
}
