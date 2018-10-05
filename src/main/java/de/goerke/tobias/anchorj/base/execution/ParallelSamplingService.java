package de.goerke.tobias.anchorj.base.execution;

import de.goerke.tobias.anchorj.base.AnchorCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link AbstractSamplingService} that gathers samples in a multiple threads.
 */
public class ParallelSamplingService extends AbstractSamplingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelSamplingService.class);
    final int threadCount;
    private final ExecutorService executor;

    ParallelSamplingService(final BiFunction<AnchorCandidate, Integer, Double> sampleFunction,
                            final int threadCount) {
        super(sampleFunction);
        this.executor = Executors.newFixedThreadPool(Math.max(threadCount, 1));
        this.threadCount = threadCount;
    }

    @Override
    public AbstractSession createSession() {
        return new ParallelSession();
    }

    protected class ParallelSession extends AbstractSamplingService.AbstractSession {

        protected Collection<Callable<Object>> createCallables() {
            return samplingCountMap.entrySet().stream().map(entry ->
                    (Callable<Object>) () -> sampleFunction.apply(entry.getKey(), entry.getValue()))
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
