package de.viadee.xai.anchor.algorithm.execution;

import de.viadee.xai.anchor.algorithm.AnchorCandidate;
import de.viadee.xai.anchor.algorithm.ClassificationFunction;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.PerturbationFunction;
import de.viadee.xai.anchor.algorithm.execution.sampling.SamplingFunction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private static final long serialVersionUID = 344301140970085409L;

    private final int threadCount;

    private final ExecutorServiceFunction executorServiceFunction;

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (this.getExecutorService() == null && this.executorServiceFunction != null) {
            this.setExecutorService(this.executorServiceFunction.apply(threadCount));
        }
    }

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction  Function used to classify any instance of type
     * @param perturbationFunction    Function used to create perturbations of the explained instance
     * @param executorService         the executor service
     * @param executorServiceSupplier the executor service supplier
     * @param threadCount             the number of threads to use
     */
    public BalancedParallelSamplingService(ClassificationFunction<T> classificationFunction,
                                           PerturbationFunction<T> perturbationFunction,
                                           ExecutorService executorService,
                                           ExecutorServiceSupplier executorServiceSupplier,
                                           int threadCount) {
        super(classificationFunction, perturbationFunction, executorService, executorServiceSupplier);
        this.threadCount = threadCount;
        this.executorServiceFunction = null;
    }

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction  Function used to classify any instance of type
     * @param perturbationFunction    Function used to create perturbations of the explained instance
     * @param executorService         the executor service
     * @param executorServiceFunction the executor service function
     * @param threadCount             the number of threads to use
     */
    public BalancedParallelSamplingService(ClassificationFunction<T> classificationFunction,
                                           PerturbationFunction<T> perturbationFunction,
                                           ExecutorService executorService,
                                           ExecutorServiceFunction executorServiceFunction,
                                           int threadCount) {
        super(classificationFunction, perturbationFunction, executorService, null);
        this.threadCount = threadCount;
        this.executorServiceFunction = executorServiceFunction;
    }

    /**
     * @param samplingFunction        the sampling function
     * @param executorService         the executor service
     * @param executorServiceSupplier the executor service supplier
     * @param threadCount             the thread count
     */
    public BalancedParallelSamplingService(SamplingFunction samplingFunction,
                                           ExecutorService executorService,
                                           ExecutorServiceSupplier executorServiceSupplier,
                                           int threadCount) {
        super(samplingFunction, executorService, executorServiceSupplier);
        this.threadCount = threadCount;
        this.executorServiceFunction = null;
    }

    /**
     * @param samplingFunction        the sampling function
     * @param executorService         the executor service
     * @param executorServiceFunction the executor service function
     * @param threadCount             the thread count
     */
    public BalancedParallelSamplingService(SamplingFunction samplingFunction,
                                           ExecutorService executorService,
                                           ExecutorServiceFunction executorServiceFunction,
                                           int threadCount) {
        super(samplingFunction, executorService, null);
        this.threadCount = threadCount;
        this.executorServiceFunction = executorServiceFunction;
    }

    @Override
    public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
        return new BalancedParallelSamplingService<>(samplingFunction, this.getExecutorService(),
                this.getExecutorServiceSupplier(), this.getThreadCount());
    }

    @Override
    public SamplingSession createSession(int explainedInstanceLabel) {
        return new BalancedParallelSession(explainedInstanceLabel);
    }

    protected int getThreadCount() {
        return threadCount;
    }

    private class BalancedParallelSession extends ParallelSession {
        private static final long serialVersionUID = 8982485103898064125L;

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
            final int threadCount = BalancedParallelSamplingService.this.getThreadCount();
            Collection<Callable<Object>> result = new ArrayList<>();
            // Equally distribute all calls (no matter which candidate) among the different threads
            final int totalCount = this.samplingCountMap.values().stream().mapToInt(i -> i).sum();
            final int countPerThread = totalCount / threadCount;
            // Division rest needs to be distributed, too
            int leftOver = totalCount % threadCount;

            final Iterator<Map.Entry<AnchorCandidate, Integer>> iterator = this.samplingCountMap.entrySet().iterator();
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
