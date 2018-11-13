package de.goerke.tobias.anchorj.execution.sampling;

import de.goerke.tobias.anchorj.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * Default {@link SamplingFunction} implementation.
 * <p>
 * Perturbs and evaluates candidates
 */
public class DefaultSamplingFunction<T extends DataInstance<?>> implements SamplingFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSamplingFunction.class);

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
}
