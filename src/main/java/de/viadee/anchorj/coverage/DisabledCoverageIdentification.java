package de.viadee.anchorj.coverage;

import java.util.Set;

/**
 * This class may be used when coverage calculations are not applicable.
 * <p>
 * However, this coverage identification method will penalize longer candidates.
 */
public class DisabledCoverageIdentification implements CoverageIdentification {
    private static final long serialVersionUID = -5492745653645291390L;

    @Override
    public double calculateCoverage(final Set<Integer> featureSet) {
        return 1 / Math.pow(2, featureSet.size());
    }
}
