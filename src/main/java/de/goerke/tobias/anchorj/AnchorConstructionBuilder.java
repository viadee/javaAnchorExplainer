package de.goerke.tobias.anchorj;

import de.goerke.tobias.anchorj.coverage.CoverageIdentification;
import de.goerke.tobias.anchorj.coverage.PerturbationBasedCoverageIdentification;
import de.goerke.tobias.anchorj.execution.BalancedParallelSamplingService;
import de.goerke.tobias.anchorj.execution.LinearSamplingService;
import de.goerke.tobias.anchorj.execution.ParallelSamplingService;
import de.goerke.tobias.anchorj.execution.SamplingService;
import de.goerke.tobias.anchorj.exploration.BestAnchorIdentification;
import de.goerke.tobias.anchorj.exploration.KL_LUCB;
import de.goerke.tobias.anchorj.global.ReconfigurablePerturbationFunction;
import de.goerke.tobias.anchorj.global.SubmodularPick;

/**
 * Builder class used to configure an {@link AnchorConstruction} instance easily.
 *
 * @param <T> the dataInstance type constructing an anchor for
 */
public class AnchorConstructionBuilder<T extends DataInstance<?>> {
    private static final int DEFAULT_COVERAGE_SAMPLE_COUNT = 1000;

    /*
     * Default values not used for constructing the instance but for instantiating other default classes
     */
    private static final int DEFAULT_KL_LUCB_BATCH_SIZE = 100;

    private ClassificationFunction<T> classificationFunction;
    private PerturbationFunction<T> perturbationFunction;
    private BestAnchorIdentification bestAnchorIdentification;
    private CoverageIdentification coverageIdentification;
    private SamplingService samplingService;
    private T explainedInstance;
    private int explainedInstanceLabel;

    /*
     *  Default values
     */
    private double delta = 0.1;
    private double epsilon = 0.1;
    private Integer maxAnchorSize = null;
    private int beamSize = 2;
    private double tau = 1;
    private double tauDiscrepancy = 0.05;
    private int initSampleCount = 1;
    private boolean lazyCoverageEvaluation = false;
    private boolean allowSuboptimalSteps = true;

    private AnchorConstructionBuilder() {
    }

    /**
     * Instantiates a new Anchor construction builder.
     * <p>
     * Shall only be used when using {@link SubmodularPick}, as instance and labels get setBestAnchorIdentification by it.
     * <p>
     * If used for {@link AnchorConstruction} and no instance and label gets setBestAnchorIdentification, an exception will be thrown.
     *
     * @param classificationFunction the classification function
     * @param perturbationFunction   the perturbation function
     */
    public AnchorConstructionBuilder(final ClassificationFunction<T> classificationFunction,
                                     final PerturbationFunction<T> perturbationFunction) {
        this(classificationFunction, perturbationFunction, null);
    }

    /**
     * Instantiates a new Anchor construction builder.
     *
     * @param classificationFunction the classification function
     * @param perturbationFunction   the perturbation function
     * @param explainedInstance      the explained instance
     */
    public AnchorConstructionBuilder(final ClassificationFunction<T> classificationFunction,
                                     final PerturbationFunction<T> perturbationFunction,
                                     final T explainedInstance) {
        this.classificationFunction = classificationFunction;
        this.perturbationFunction = perturbationFunction;
        this.explainedInstance = explainedInstance;
        if (explainedInstance != null) {
            this.explainedInstanceLabel = classificationFunction.predict(explainedInstance);
        } else {
            this.explainedInstanceLabel = -1;
        }
    }

    /**
     * Sets the perturbation function.
     *
     * @param perturbationFunction the perturbation function
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setPerturbationFunction(final PerturbationFunction<T> perturbationFunction) {
        this.perturbationFunction = perturbationFunction;
        return this;
    }

    /**
     * Sets the explained instance.
     *
     * @param explainedInstance the explained instance
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setExplainedInstance(final T explainedInstance) {
        this.explainedInstance = explainedInstance;
        return this;
    }

    /**
     * Sets the explained instance label.
     *
     * @param explainedInstanceLabel the explained instance label
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setExplainedInstanceLabel(final int explainedInstanceLabel) {
        this.explainedInstanceLabel = explainedInstanceLabel;
        return this;
    }

    /**
     * Sets delta.
     *
     * @param delta the delta
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setDelta(final double delta) {
        this.delta = delta;
        return this;
    }

    /**
     * Sets epsilon.
     *
     * @param epsilon the epsilon value
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setEpsilon(final double epsilon) {
        this.epsilon = epsilon;
        return this;
    }

    /**
     * Sets max anchor size.
     * <p>
     * This is the maximum amount of features contained in an Anchor
     *
     * @param maxAnchorSize the max anchor size
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setMaxAnchorSize(final Integer maxAnchorSize) {
        this.maxAnchorSize = maxAnchorSize;
        return this;
    }

    /**
     * Sets beam size.
     * <p>
     * This is the amount of simultaneously evolving candidates for beam search.
     *
     * @param beamSize the beam size
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setBeamSize(final int beamSize) {
        this.beamSize = beamSize;
        return this;
    }

    /**
     * Sets tau.
     * <p>
     * This is the desired precision for an anchor to have.
     *
     * @param tau the tau
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setTau(final double tau) {
        this.tau = tau;
        return this;
    }

    /**
     * Sets tau discrepancy.
     *
     * @param tauDiscrepancy the tau discrepancy
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setTauDiscrepancy(final double tauDiscrepancy) {
        this.tauDiscrepancy = tauDiscrepancy;
        return this;
    }

    /**
     * Sets init sample count.
     * <p>
     * This is the optional amount of samples taken for each candidate before running the
     * {@link BestAnchorIdentification} algorithm.
     *
     * @param initSampleCount the init sample count
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setInitSampleCount(final int initSampleCount) {
        this.initSampleCount = initSampleCount;
        return this;
    }

    /**
     * Enable threading anchor construction builder.
     *
     * @param threadCount       the thread count
     * @param doBalanceSampling the do balance sampling
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> enableThreading(final int threadCount, final boolean doBalanceSampling) {
        if (threadCount <= 1)
            this.samplingService = new LinearSamplingService<>(classificationFunction, perturbationFunction);
        if (!doBalanceSampling)
            this.samplingService = new ParallelSamplingService<>(classificationFunction, perturbationFunction, threadCount);
        this.samplingService = new BalancedParallelSamplingService<>(classificationFunction, perturbationFunction, threadCount);

        return this;
    }

    /**
     * Sets the best anchor identification method.
     * <p>
     * If setBestAnchorIdentification to null, the default {@link KL_LUCB} will be setBestAnchorIdentification.
     *
     * @param bestAnchorIdentification the best anchor identification
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setBestAnchorIdentification(final BestAnchorIdentification bestAnchorIdentification) {
        this.bestAnchorIdentification = bestAnchorIdentification;
        return this;
    }

    /**
     * Sets the coverage identification method.
     * <p>
     * If setBestAnchorIdentification to null, the default {@link PerturbationBasedCoverageIdentification} will be setBestAnchorIdentification.
     *
     * @param coverageIdentification the coverage identification
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setCoverageIdentification(final CoverageIdentification coverageIdentification) {
        this.coverageIdentification = coverageIdentification;
        return this;
    }

    /**
     * Sets the sampling service
     * <p>
     * If setBestAnchorIdentification to null, the default
     * {@link de.goerke.tobias.anchorj.execution.LinearSamplingService} is used
     *
     * @param samplingService the sampling service
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setSamplingService(final SamplingService samplingService) {
        this.samplingService = samplingService;
        return this;
    }

    /**
     * Sets lazy coverage evaluation.
     * <p>
     * If set true, a candidate's coverage will only be determined when needed to, i.e. when extending or returning it
     *
     * @param lazyCoverageEvaluation the lazy coverage evaluation value
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setLazyCoverageEvaluation(final boolean lazyCoverageEvaluation) {
        this.lazyCoverageEvaluation = lazyCoverageEvaluation;
        return this;
    }

    /**
     * Sets allow suboptimal steps.
     * <p>
     * If set to false, candidates that are returned by the best arm identification get
     * removed when their precision is lower than their parent's
     *
     * @param allowSuboptimalSteps the allow suboptimal steps value
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setAllowSuboptimalSteps(final boolean allowSuboptimalSteps) {
        this.allowSuboptimalSteps = allowSuboptimalSteps;
        return this;
    }

    /**
     * This method sets up the builder for usage within the {@link SubmodularPick} algorithm.
     * <p>
     * As the SP-algorithm requires setting another instance and label for each run, this method takes all required
     * adjustments to the builder to provide a suitable {@link AnchorConstruction}.
     *
     * @param explainedInstance      the instance being explained by the current SP's iteration
     * @param explainedInstanceLabel the instanceLabel belonging to the explainedInstance
     * @return the builder ready to be built for usage within the SP algorithm
     */
    public AnchorConstructionBuilder<T> setupForSP(final T explainedInstance, final int explainedInstanceLabel) {
        if (!(this.perturbationFunction instanceof ReconfigurablePerturbationFunction)) {
            throw new IllegalArgumentException("For using the SP-algorithm, the perturbation function needs to " +
                    "be reconfigurable for foreign instances. Please implement the ReconfigurablePerturbationFunction");
        }

        // FIXME perturbation function needs to be set in sampling function
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
        this.perturbationFunction = ((ReconfigurablePerturbationFunction<T>) this.perturbationFunction)
                .createForInstance(explainedInstance);
        return this;
    }

    private CoverageIdentification createDefaultCoverageIdentification() {
        return PerturbationBasedCoverageIdentification.createFromPerturbationFunction(
                DEFAULT_COVERAGE_SAMPLE_COUNT, perturbationFunction);
    }

    private BestAnchorIdentification createDefaultBestAnchorIdentification() {
        return new KL_LUCB(DEFAULT_KL_LUCB_BATCH_SIZE);
    }

    /**
     * Build the instance setting the setBestAnchorIdentification values or their pre-configures default values.
     *
     * @return the anchor construction
     */
    public AnchorConstruction<T> build() {
        return new AnchorConstruction<>(
                (bestAnchorIdentification == null) ? createDefaultBestAnchorIdentification() : bestAnchorIdentification,
                (coverageIdentification == null) ? createDefaultCoverageIdentification() : coverageIdentification,
                (samplingService == null) ? new LinearSamplingService<>(classificationFunction, perturbationFunction) : samplingService,
                explainedInstance, explainedInstanceLabel,
                (maxAnchorSize == null) ? explainedInstance.getFeatureCount() : maxAnchorSize,
                beamSize, delta, epsilon, tau, tauDiscrepancy, initSampleCount,
                lazyCoverageEvaluation, allowSuboptimalSteps);
    }

    /**
     * @return the {@link ClassificationFunction} to be obtained by the {@link SubmodularPick} algorithm
     */
    public ClassificationFunction<T> getClassificationFunction() {
        return classificationFunction;
    }

    /**
     * Sets the classification function.
     *
     * @param classificationFunction the classification function
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setClassificationFunction(final ClassificationFunction<T> classificationFunction) {
        this.classificationFunction = classificationFunction;
        return this;
    }

}
