package de.viadee.anchorj.execution.sampling;

import de.viadee.anchorj.*;
import de.viadee.anchorj.coverage.CoverageIdentification;
import de.viadee.anchorj.coverage.PerturbationBasedCoverageIdentification;
import de.viadee.anchorj.global.ReconfigurablePerturbationFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * Default {@link SamplingFunction} implementation.
 * <p>
 * Perturbs and evaluates candidates
 */
public class DefaultSamplingFunction<T extends DataInstance<?>> implements SamplingFunction<T> {
    private static final long serialVersionUID = 2870480419515709442L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSamplingFunction.class);
    private static final int DEFAULT_COVERAGE_SAMPLE_COUNT = 1000;

    private final ClassificationFunction<T> classificationFunction;
    private final PerturbationFunction<T> perturbationFunction;

    /**
     * Creates the sampling function.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction Function used to classify any instance of type
     * @param perturbationFunction   Function used to create perturbations of the
     *                               {@link AnchorConstruction#explainedInstance}
     */
    public DefaultSamplingFunction(ClassificationFunction<T> classificationFunction,
                                   PerturbationFunction<T> perturbationFunction) {
        this.classificationFunction = classificationFunction;
        this.perturbationFunction = perturbationFunction;
    }

    @Override
    public double evaluate(AnchorCandidate candidate, int samplesToEvaluate, int explainedInstanceLabel) {
        if (samplesToEvaluate < 1)
            return 0;

        final PerturbationFunction.PerturbationResult<T> perturbationResult = perturbationFunction.perturb(
                candidate.getCanonicalFeatures(), samplesToEvaluate);
        final int[] predictions = classificationFunction.predict(perturbationResult.getRawResult());

        final int matchingLabels = Math.toIntExact(IntStream.of(predictions)
                .filter(p -> p == explainedInstanceLabel).count());

        candidate.registerSamples(samplesToEvaluate, matchingLabels);

        double precision = matchingLabels / (double) predictions.length;
        LOGGER.trace("Sampling {} perturbations of {} has resulted in {} correct predictions, thus a precision of {}",
                samplesToEvaluate, candidate.getCanonicalFeatures(), matchingLabels, precision);
        return precision;
    }

    @Override
    public DefaultSamplingFunction<T> notifyOriginChange(T explainedInstance) throws UnsupportedOperationException {
        if (!(this.perturbationFunction instanceof ReconfigurablePerturbationFunction)) {
            throw new UnsupportedOperationException("For using the SP-algorithm, the perturbation function needs to " +
                    "be reconfigurable for foreign instances. Please implement the ReconfigurablePerturbationFunction");
        }

        return new DefaultSamplingFunction<>(this.classificationFunction,
                ((ReconfigurablePerturbationFunction<T>) this.perturbationFunction)
                        .createForInstance(explainedInstance));
    }

    /**
     * Extends the {@link #createPerturbationBasedCoverageIdentification()} to specify a coverage sample count
     *
     * @param coverageSampleCount the amount of samples to take for coverage
     * @return a {@link CoverageIdentification} function
     */
    public PerturbationBasedCoverageIdentification createPerturbationBasedCoverageIdentification(final int coverageSampleCount) {
        return PerturbationBasedCoverageIdentification.createFromPerturbationFunction(coverageSampleCount,
                perturbationFunction);
    }

    @Override
    public PerturbationBasedCoverageIdentification createPerturbationBasedCoverageIdentification() {
        return createPerturbationBasedCoverageIdentification(DEFAULT_COVERAGE_SAMPLE_COUNT);
    }

    @Override
    public ClassificationFunction<T> getClassificationFunction() {
        return classificationFunction;
    }

}

