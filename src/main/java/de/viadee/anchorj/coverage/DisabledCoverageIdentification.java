package de.viadee.anchorj.coverage;

import java.util.Set;

/**
 * This class may be used when coverage calculations are not applicable.
 * <p>
 * However, this coverage identification method will penalize longer candidates.
 */
public class DisabledCoverageIdentification implements CoverageIdentification {

    @Override
    public double calculateCoverage(final Set<Integer> featureSet) {
        return 1 / Math.pow(2, featureSet.size());
    }
}
