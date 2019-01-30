package de.viadee.xai.anchor.execution;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.viadee.xai.anchor.ClassificationFunction;
import de.viadee.xai.anchor.DataInstance;
import de.viadee.xai.anchor.PerturbationFunction;
import de.viadee.xai.anchor.execution.sampling.SamplingFunction;

/**
 * Implementation of the {@link AbstractSamplingService} that gathers samples in
 * a multiple threads.
 *
 * @param <T> Type of the sampled instance
 */
public class ParallelSamplingService<T extends DataInstance<?>> extends AbstractSamplingService<T> {
	private static final long serialVersionUID = 2726826635848365350L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ParallelSamplingService.class);
	final int threadCount;
	// ExecutorService cannot
	private transient ExecutorService executor;

	/**
	 * Creates the sampling service.
	 * <p>
	 * Requires both a perturbation and classification function to evaluate
	 * candidates
	 *
	 * @param classificationFunction Function used to classify any instance of type
	 * @param perturbationFunction   Function used to create perturbations of the
	 *                               explained instance
	 * @param threadCount            the number of threads to use
	 */
	public ParallelSamplingService(ClassificationFunction<T> classificationFunction,
			PerturbationFunction<T> perturbationFunction, int threadCount) {
		super(classificationFunction, perturbationFunction);
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
		this.threadCount = threadCount;
	}

	@Override
	public SamplingSession createSession(int explainedInstanceLabel) {
		return new ParallelSession(explainedInstanceLabel);
	}

	@Override
	public SamplingService notifySamplingFunctionChange(SamplingFunction samplingFunction) {
		return new ParallelSamplingService<>(samplingFunction, threadCount);
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
			// Avoid serialization issues, as ExecutorService is not serializable
			if (ParallelSamplingService.this.executor == null)
				ParallelSamplingService.this.executor = Executors.newFixedThreadPool(Math.max(threadCount, 1));
		}

		protected Collection<Callable<Object>> createCallables() {
			return samplingCountMap.entrySet().stream()
					.map(entry -> (Callable<Object>) () -> doSample(entry.getKey(), entry.getValue()))
					.collect(Collectors.toList());
		}

		@Override
		public void execute() {
			try {
				executor.invokeAll(createCallables());
			} catch (final InterruptedException e) {
				LOGGER.warn("Thread interrupted", e);
				Thread.currentThread().interrupt();
			} finally {
				executor.shutdown();
			}
		}
	}
}
