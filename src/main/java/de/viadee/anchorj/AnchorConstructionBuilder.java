package de.viadee.anchorj;

import de.viadee.anchorj.coverage.CoverageIdentification;
import de.viadee.anchorj.coverage.PerturbationBasedCoverageIdentification;
import de.viadee.anchorj.execution.BalancedParallelSamplingService;
import de.viadee.anchorj.execution.LinearSamplingService;
import de.viadee.anchorj.execution.ParallelSamplingService;
import de.viadee.anchorj.execution.SamplingService;
import de.viadee.anchorj.execution.sampling.DefaultSamplingFunction;
import de.viadee.anchorj.execution.sampling.SamplingFunction;
import de.viadee.anchorj.exploration.BestAnchorIdentification;
import de.viadee.anchorj.exploration.KL_LUCB;
import de.viadee.anchorj.global.SubmodularPick;

import java.io.Serializable;

/**
 * Builder class used to configure an {@link AnchorConstruction} instance easily.
 *
 * @param <T> the dataInstance type constructing an anchor for
 */
public class AnchorConstructionBuilder<T extends DataInstance<?>> implements Serializable {
    private static final long serialVersionUID = -4081808558489352450L;

    private static final int DEFAULT_COVERAGE_SAMPLE_COUNT = 1000;

    private SamplingFunction<T> samplingFunction;
    private T explainedInstance;
    private int explainedInstanceLabel;
    private BestAnchorIdentification bestAnchorIdentification;
    private CoverageIdentification coverageIdentification;
    private SamplingService samplingService;

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

    private AnchorConstructionBuilder(SamplingFunction<T> samplingFunction, T explainedInstance,
                                      int explainedInstanceLabel, BestAnchorIdentification bestAnchorIdentification,
                                      CoverageIdentification coverageIdentification, SamplingService samplingService,
                                      double delta, double epsilon, Integer maxAnchorSize, int beamSize, double tau,
                                      double tauDiscrepancy, int initSampleCount, boolean lazyCoverageEvaluation,
                                      boolean allowSuboptimalSteps) {
        this.samplingFunction = samplingFunction;
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
        this.bestAnchorIdentification = bestAnchorIdentification;
        this.coverageIdentification = coverageIdentification;
        this.samplingService = samplingService;
        this.delta = delta;
        this.epsilon = epsilon;
        this.maxAnchorSize = maxAnchorSize;
        this.beamSize = beamSize;
        this.tau = tau;
        this.tauDiscrepancy = tauDiscrepancy;
        this.initSampleCount = initSampleCount;
        this.lazyCoverageEvaluation = lazyCoverageEvaluation;
        this.allowSuboptimalSteps = allowSuboptimalSteps;
    }

    /**
     * Instantiates a new Anchor construction builder.
     * <p>
     * Uses the classification function to predict a label to explain, if the instance is != null
     *
     * @param classificationFunction the classification function
     * @param perturbationFunction   the perturbation function
     * @param explainedInstance      the instance to explain
     */
    public AnchorConstructionBuilder(final ClassificationFunction<T> classificationFunction,
                                     final PerturbationFunction<T> perturbationFunction,
                                     final T explainedInstance) {
        this(classificationFunction, perturbationFunction, explainedInstance,
                (explainedInstance != null) ? classificationFunction.predict(explainedInstance) : -1);
    }

    /**
     * Instantiates a new Anchor construction builder.
     *
     * @param classificationFunction the classification function
     * @param perturbationFunction   the perturbation function
     * @param explainedInstance      the explained instance
     * @param explainedInstanceLabel the explained instance label
     */
    public AnchorConstructionBuilder(final ClassificationFunction<T> classificationFunction,
                                     final PerturbationFunction<T> perturbationFunction,
                                     final T explainedInstance, final int explainedInstanceLabel) {
        this(new DefaultSamplingFunction<>(classificationFunction, perturbationFunction),
                explainedInstance, explainedInstanceLabel);
    }

    /**
     * Instantiates a new Anchor construction builder.
     *
     * @param samplingFunction       the samplingFunction to use
     * @param explainedInstance      the explained instance
     * @param explainedInstanceLabel the explained instance label
     */
    public AnchorConstructionBuilder(final SamplingFunction<T> samplingFunction,
                                     final T explainedInstance, final int explainedInstanceLabel) {
        this.samplingFunction = samplingFunction;
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
    }

    /**
     * This method sets up the builder for usage within the {@link SubmodularPick} algorithm.
     * <p>
     * As the SP-algorithm requires setting another instance and label for each run, this method takes all required
     * adjustments to the builder to provide a suitable {@link AnchorConstruction}.
     * <p>
     * Creates a temporary new builder as to not change any fields already set.
     * Thus, the builder param does not get changed
     *
     * @param builder           the construction builder used
     * @param explainedInstance the instance being explained by the current SP's iteration
     * @param <T>               the type of the explained instance
     * @return the builder ready to be built for usage within the SP algorithm
     */
    public static <T extends DataInstance<?>> AnchorConstruction<T> buildForSP(AnchorConstructionBuilder<T> builder,
                                                                               final T explainedInstance) {
        final AnchorConstructionBuilder<T> newBuilder = new AnchorConstructionBuilder<>(builder.samplingFunction,
                builder.explainedInstance, builder.explainedInstanceLabel, builder.bestAnchorIdentification,
                builder.coverageIdentification, builder.samplingService, builder.delta, builder.epsilon,
                builder.maxAnchorSize, builder.beamSize, builder.tau, builder.tauDiscrepancy, builder.initSampleCount,
                builder.lazyCoverageEvaluation, builder.allowSuboptimalSteps);

        newBuilder.explainedInstance = explainedInstance;
        newBuilder.samplingFunction = newBuilder.samplingFunction.notifyOriginChange(explainedInstance);

        newBuilder.prepareForBuild();

        newBuilder.explainedInstanceLabel = newBuilder.samplingFunction.getClassificationFunction().predict(explainedInstance);
        if (newBuilder.coverageIdentification instanceof PerturbationBasedCoverageIdentification)
            newBuilder.coverageIdentification = newBuilder.samplingFunction.createPerturbationBasedCoverageIdentification();
        newBuilder.samplingService = newBuilder.samplingService.notifySamplingFunctionChange(newBuilder.samplingFunction);

        return newBuilder.build();
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
            this.samplingService = new LinearSamplingService<>(samplingFunction);
        else if (!doBalanceSampling)
            this.samplingService = new ParallelSamplingService<>(samplingFunction, threadCount);
        else
            this.samplingService = new BalancedParallelSamplingService<>(samplingFunction, threadCount);

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
     * {@link de.viadee.anchorj.execution.LinearSamplingService} is used
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

    private void prepareForBuild() {
        if (this.bestAnchorIdentification == null)
            this.bestAnchorIdentification = new KL_LUCB();
        if (this.coverageIdentification == null)
            this.coverageIdentification = samplingFunction.createPerturbationBasedCoverageIdentification();
        if (this.samplingService == null)
            this.samplingService = new LinearSamplingService<>(samplingFunction);
    }

    /**
     * Build the instance setting the setBestAnchorIdentification values or their pre-configures default values.
     *
     * @return the anchor construction
     */
    public AnchorConstruction<T> build() {
        this.prepareForBuild();
        return new AnchorConstruction<>(bestAnchorIdentification, coverageIdentification, samplingService,
                explainedInstance, explainedInstanceLabel,
                (maxAnchorSize == null) ? explainedInstance.getFeatureCount() : maxAnchorSize,
                beamSize, delta, epsilon, tau, tauDiscrepancy, initSampleCount,
                lazyCoverageEvaluation, allowSuboptimalSteps);
    }

}