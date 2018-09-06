package de.goerke.tobias.anchorj.base;

import de.goerke.tobias.anchorj.base.coverage.CoverageIdentification;
import de.goerke.tobias.anchorj.base.coverage.PerturbationBasedCoverageIdentification;
import de.goerke.tobias.anchorj.base.exploration.BestAnchorIdentification;
import de.goerke.tobias.anchorj.base.exploration.KL_LUCB;

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
    private static final double DEFAULT_KL_LUCB_EPSILON = 0.05;
    private static final int DEFAULT_KL_LUCB_BATCH_SIZE = 100;

    private ClassificationFunction<T> classificationFunction;
    private PerturbationFunction<T> perturbationFunction;
    private BestAnchorIdentification bestAnchorIdentification;
    private CoverageIdentification coverageIdentification;
    private T explainedInstance;
    private int explainedInstanceLabel;

    /*
     *  Default values
     */
    private double delta = 0.1;
    private Integer maxAnchorSize = null;
    private int beamSize = 2;
    private double tau = 1;
    private double tauDiscrepancy = 0.05;
    private int initSampleCount = 1;
    private int threadCount = 1;
    private boolean doBalanceSampling = false;
    private boolean lazyCoverageEvaluation = false;

    public AnchorConstructionBuilder() {
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
        this(classificationFunction, perturbationFunction, null, -1);
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
        this.classificationFunction = classificationFunction;
        this.perturbationFunction = perturbationFunction;
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
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
        this.threadCount = threadCount;
        this.doBalanceSampling = doBalanceSampling;
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
     * Sets lazy coverage evaluation.
     * <p>
     * If set true, a candidate's coverage will only be determined when needed to, i.e. when extending or returning it
     *
     * @param lazyCoverageEvaluation the coverage identification
     * @return the current {@link AnchorConstructionBuilder} for chaining
     */
    public AnchorConstructionBuilder<T> setLazyCoverageEvaluation(final boolean lazyCoverageEvaluation) {
        this.lazyCoverageEvaluation = lazyCoverageEvaluation;
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
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
        this.perturbationFunction = this.perturbationFunction.createForInstance(explainedInstance);
        return this;
    }

    private CoverageIdentification createDefaultCoverageIdentification() {
        return PerturbationBasedCoverageIdentification.createFromPerturbationFunction(
                DEFAULT_COVERAGE_SAMPLE_COUNT, perturbationFunction);
    }

    private BestAnchorIdentification createDefaultBestAnchorIdentification() {
        return new KL_LUCB(DEFAULT_KL_LUCB_BATCH_SIZE, DEFAULT_KL_LUCB_EPSILON);
    }


    /**
     * Build the instance setting the setBestAnchorIdentification values or their pre-configures default values.
     *
     * @return the anchor construction
     */
    public AnchorConstruction<T> build() {
        return new AnchorConstruction<>(classificationFunction, perturbationFunction,
                (bestAnchorIdentification == null) ? createDefaultBestAnchorIdentification() : bestAnchorIdentification,
                (coverageIdentification == null) ? createDefaultCoverageIdentification() : coverageIdentification,
                explainedInstance, explainedInstanceLabel,
                (maxAnchorSize == null) ? explainedInstance.getFeatureCount() : maxAnchorSize,
                beamSize, delta, tau, tauDiscrepancy, initSampleCount,
                threadCount, doBalanceSampling, lazyCoverageEvaluation);
    }

    /**
     * @return the {@link ClassificationFunction} to be obtained by the {@link SubmodularPick} algorithm
     */
    ClassificationFunction<T> getClassificationFunction() {
        return classificationFunction;
    }

    /**
     * @return the max thread count to be obtained by the {@link SubmodularPick} algorithm
     */
    int getMaxThreadCount() {
        return Math.min(1, threadCount);
    }
}