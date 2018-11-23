package de.viadee.anchorj.execution;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import de.viadee.anchorj.AnchorCandidate;
import de.viadee.anchorj.ClassificationFunction;
import de.viadee.anchorj.DataInstance;
import de.viadee.anchorj.PerturbationFunction;
import de.viadee.anchorj.execution.sampling.SamplingFunction;

/**
 * Implementation of the {@link AbstractSamplingService} and extension of {@link ParallelSamplingService}.
 * <p>
 * Equally distributes the amount of samples to be obtained among all threads.
 * <p>
 * Hence, a single thread may obtain predictions for different candidates in one batch.
 *
 * @param <T> Type of the sampled instance
 */
public class BalancedParallelSamplingService<T extends DataInstance<?>> extends ParallelSamplingService<T> {

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction Function used to classify any instance of type
     * @param perturbationFunction   Function used to create perturbations of the explained instance
     * @param threadCount            the number of threads to use
     */
    public BalancedParallelSamplingService(ClassificationFunction<T> classificationFunction,
                                           PerturbationFunction<T> perturbationFunction, int threadCount) {
        super(classificationFunction, perturbationFunction, threadCount);
    }

    /**
     * Creates the sampling service.
     *
     * @param samplingFunction the sampling function to be used
     * @param threadCount      the number of threads to use
     */
    public BalancedParallelSamplingService(SamplingFunction samplingFunction, int threadCount) {
        super(samplingFunction, threadCount);
    }

    @Override
    public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
        return new BalancedParallelSamplingService<>(samplingFunction, threadCount);
    }

    @Override
    public SamplingSession createSession(int explainedInstanceLabel) {
        return new BalancedParallelSession(explainedInstanceLabel);
    }

    private class BalancedParallelSession extends ParallelSession {

        /**
         * Creates an instance.
         *
         * @param explainedInstanceLabel the label being explained
         */
        private BalancedParallelSession(int explainedInstanceLabel) {
            super(explainedInstanceLabel);
        }

        @Override
        protected Collection<Callable<Object>> createCallables() {
            Collection<Callable<Object>> result = new ArrayList<>();
            // Equally distribute all calls (no matter which candidate) among the different threads
            final int totalCount = samplingCountMap.values().stream().mapToInt(i -> i).sum();
            final int countPerThread = totalCount / threadCount;
            // Division rest needs to be distributed, too
            int leftOver = totalCount % threadCount;

            final Iterator<Map.Entry<AnchorCandidate, Integer>> iterator = samplingCountMap.entrySet().iterator();
            // Check if no samples shall be taken at all
            if (!iterator.hasNext())
                return Collections.emptyList();
            Map.Entry<AnchorCandidate, Integer> current = iterator.next();

            int currentLeft = current.getValue();
            for (int i = 0; i < threadCount; i++) {
                final List<Runnable> subRunnables = new ArrayList<>();
                int threadRemainingCount = countPerThread;
                // Spread division rest over first threads
                if (leftOver > 0) {
                    threadRemainingCount++;
                    leftOver--;
                }
                while (threadRemainingCount > 0) {
                    final int sampleCount = Math.min(threadRemainingCount, currentLeft);
                    final AnchorCandidate candidate = current.getKey();
                    currentLeft -= sampleCount;
                    threadRemainingCount -= sampleCount;
                    subRunnables.add(() -> doSample(candidate, sampleCount));
                    if (currentLeft < 1) {
                        // Check if we reached the end
                        if (!iterator.hasNext())
                            break;
                        current = iterator.next();
                        currentLeft = current.getValue();
                    }
                }
                result.add(Executors.callable(() -> subRunnables.forEach(Runnable::run)));
            }
            return result;
        }
    }
}
