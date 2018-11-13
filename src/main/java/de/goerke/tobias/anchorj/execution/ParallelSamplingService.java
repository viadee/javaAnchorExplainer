package de.goerke.tobias.anchorj.execution;

import de.goerke.tobias.anchorj.ClassificationFunction;
import de.goerke.tobias.anchorj.DataInstance;
import de.goerke.tobias.anchorj.PerturbationFunction;
import de.goerke.tobias.anchorj.execution.sampling.SamplingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link AbstractSamplingService} that gathers samples in a multiple threads.
 *
 * @param <T> Type of the sampled instance
 */
public class ParallelSamplingService<T extends DataInstance<?>> extends AbstractSamplingService<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelSamplingService.class);
    final int threadCount;
    private final ExecutorService executor;

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate candidates
     *
     * @param classificationFunction Function used to classify any instance of type
     * @param perturbationFunction   Function used to create perturbations of the explained instance
     * @param threadCount            the number of threads to use
     */
    public ParallelSamplingService(ClassificationFunction<T> classificationFunction,
                                   PerturbationFunction<T> perturbationFunction, int threadCount) {
        super(classificationFunction, perturbationFunction);
        this.executor = Executors.newFixedThreadPool(Math.max(threadCount, 1));
        this.threadCount = threadCount;
    }

    /**
     * Creates the sampling service.
     *
     * @param samplingFunction the sampling function to be used
     * @param threadCount      the number of threads to use
     */
    public ParallelSamplingService(SamplingFunction samplingFunction, int threadCount) {
        super(samplingFunction);
        this.executor = Executors.newFixedThreadPool(Math.max(threadCount, 1));
        this.threadCount = threadCount;
    }

    @Override
    public SamplingSession createSession(int explainedInstanceLabel) {
        return new ParallelSession(explainedInstanceLabel);
    }

    protected class ParallelSession extends AbstractSamplingSession {

        /**
         * Creates an instance.
         *
         * @param explainedInstanceLabel the label being explained
         */
        ParallelSession(int explainedInstanceLabel) {
            super(explainedInstanceLabel);
        }

        protected Collection<Callable<Object>> createCallables() {
            return samplingCountMap.entrySet().stream().map(entry ->
                    (Callable<Object>) () -> doSample(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        }

        @Override
        public void execute() {
            try {
                executor.invokeAll(createCallables());
            } catch (InterruptedException e) {
                LOGGER.warn("Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
