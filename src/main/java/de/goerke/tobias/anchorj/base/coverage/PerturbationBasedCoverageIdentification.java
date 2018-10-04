package de.goerke.tobias.anchorj.base.coverage;

import de.goerke.tobias.anchorj.base.PerturbationFunction;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.Collections;
import java.util.Set;

/**
 * This class represents the method of determining an Anchor candidate's coverage.
 * <p>
 * Ribeiro (2018) initially proposed to define an Anchor's coverage as cov(A) = E_{D(z)}[A(z)].
 * According to this definition, the coverage directly depends on the perturbation space, as realized by this class.
 */
public class PerturbationBasedCoverageIdentification implements CoverageIdentification {

    private final boolean[][] coverageData;

    /**
     * Instantiates a {@link PerturbationBasedCoverageIdentification}.
     *
     * @param coverageData These are pre-loaded perturbations, having a zero value if the perturbation's feature has not
     *                     been changed, 1 otherwise. This data will be used to determine the coverage of a single
     *                     instance. As we assume that totally random perturbations reflect the complete input-space,
     *                     we just have measure the fraction of instances the rule would apply to.
     */
    public PerturbationBasedCoverageIdentification(final boolean[][] coverageData) {
        if (coverageData == null)
            throw new IllegalArgumentException("Coverage data" + ParameterValidation.NULL_MESSAGE);
        this.coverageData = coverageData;
    }

    /**
     * @param coverageSampleCount  The amount of perturbations that will be obtained in the beginning to later
     *                             determine a candidate's coverage
     * @param perturbationFunction The {@link PerturbationFunction} used to create perturbations
     * @return an instance of the {@link PerturbationBasedCoverageIdentification}
     */
    public static PerturbationBasedCoverageIdentification createFromPerturbationFunction(final int coverageSampleCount,
                                                                                         final PerturbationFunction
                                                                                                 perturbationFunction) {
        if (!ParameterValidation.isUnsigned(coverageSampleCount))
            throw new IllegalArgumentException("Coverage sample count must not be negative");
        if (perturbationFunction == null)
            throw new IllegalArgumentException("Coverage data" + ParameterValidation.NULL_MESSAGE);

        return new PerturbationBasedCoverageIdentification(
                perturbationFunction.perturb(Collections.emptySet(), coverageSampleCount).getFeatureChanged());
    }

    @Override
    public double calculateCoverage(final Set<Integer> featureSet) {
        int matchingRows = 0;
        for (boolean[] row : coverageData) {
            boolean matches = true;
            for (final Integer feature : featureSet) {
                if (row[feature]) {
                    matches = false;
                    break;
                }
            }
            if (matches)
                matchingRows++;
        }

        return matchingRows / (double) coverageData.length;
    }
}
