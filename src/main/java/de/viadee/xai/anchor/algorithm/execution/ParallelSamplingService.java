package de.viadee.xai.anchor.algorithm.execution;

import de.viadee.xai.anchor.algorithm.ClassificationFunction;
import de.viadee.xai.anchor.algorithm.DataInstance;
import de.viadee.xai.anchor.algorithm.PerturbationFunction;
import de.viadee.xai.anchor.algorithm.execution.sampling.SamplingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link AbstractSamplingService} that gathers samples in
 * a multiple threads.
 * <p>
 * TODO why is there such a  parameter abundance? Why is there no default constructor creating the executor services?
 *
 * @param <T> Type of the sampled instance
 */
public class ParallelSamplingService<T extends DataInstance<?>> extends AbstractSamplingService<T> implements Closeable {
    private static final long serialVersionUID = 2726826635848365350L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelSamplingService.class);

    private transient ExecutorService executorService;

    private final ExecutorServiceSupplier executorServiceSupplier;

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        if (this.executorServiceSupplier != null) {
            this.executorService = this.executorServiceSupplier.get();
        } else {
            this.executorService = null;
        }
    }

    /**
     * Creates the sampling service.
     * <p>
     * Requires both a perturbation and classification function to evaluate
     * candidates
     *
     * @param classificationFunction  Function used to classify any instance of type
     * @param perturbationFunction    Function used to create perturbations of the
     *                                explained instance
     * @param executorService         the desired executor
     * @param executorServiceSupplier needed if this instance gets serialized e. g. to cluster the execution with Spark.
     *                                Simple lambda with desired Executor as return type
     */
    public ParallelSamplingService(ClassificationFunction<T> classificationFunction,
                                   PerturbationFunction<T> perturbationFunction,
                                   ExecutorService executorService,
                                   ExecutorServiceSupplier executorServiceSupplier) {
        super(classificationFunction, perturbationFunction);
        this.executorService = executorService;
        this.executorServiceSupplier = executorServiceSupplier;
    }

    /**
     * Creates the sampling service.
     *
     * @param samplingFunction        the sampling function to be used
     * @param executorService         the desired executor
     * @param executorServiceSupplier needed if this instance gets serialized e. g. to cluster the execution with Spark.
     *                                Simple lambda with desired Executor as return type
     */
    public ParallelSamplingService(SamplingFunction samplingFunction,
                                   ExecutorService executorService,
                                   ExecutorServiceSupplier executorServiceSupplier) {
        super(samplingFunction);
        this.executorService = executorService;
        this.executorServiceSupplier = executorServiceSupplier;
    }

    @Override
    public SamplingSession createSession(int explainedInstanceLabel) {
        return new ParallelSession(explainedInstanceLabel);
    }

    /**
     * Closes the internally used ExecutorService.
     * <p>
     * The service can no longer be used after this method has been called.
     */
    @Override
    public void close() {
        if (this.executorService != null) {
            LOGGER.debug("closing session");
            this.executorService.shutdown();
        }
    }

    @Override
    public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
        return new ParallelSamplingService<>(samplingFunction, this.executorService, this.executorServiceSupplier);
    }

    protected ExecutorService getExecutorService() {
        return executorService;
    }

    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    protected ExecutorServiceSupplier getExecutorServiceSupplier() {
        return executorServiceSupplier;
    }

    protected class ParallelSession extends AbstractSamplingSession {
        private static final long serialVersionUID = 5719558301835996215L;

        /**
         * Creates an instance.
         *
         * @param explainedInstanceLabel the label being explained
         */
        ParallelSession(int explainedInstanceLabel) {
            super(explainedInstanceLabel);
        }

        protected Collection<Callable<Object>> createCallables() {
            return this.samplingCountMap.entrySet().stream()
                    .map(entry -> (Callable<Object>) () -> doSample(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        }

        @Override
        public void execute() {
            try {
                ParallelSamplingService.this.executorService.invokeAll(createCallables());
            } catch (final InterruptedException e) {
                LOGGER.error("Thread interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
