package de.viadee.xai.anchor.algorithm;

import de.viadee.xai.anchor.algorithm.coverage.CoverageIdentification;
import de.viadee.xai.anchor.algorithm.execution.SamplingService;
import de.viadee.xai.anchor.algorithm.execution.SamplingSession;
import de.viadee.xai.anchor.algorithm.exploration.BestAnchorIdentification;
import de.viadee.xai.anchor.algorithm.util.KLBernoulliUtils;
import de.viadee.xai.anchor.algorithm.util.ParameterValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Base class for constructing Anchors.
 * <p>
 * May be created by the {@link AnchorConstructionBuilder}.
 *
 * @param <T> the type of the predicted instance
 */
public class AnchorConstruction<T extends DataInstance<?>> implements Serializable {
    private static final long serialVersionUID = -478521750533925027L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AnchorConstruction.class);

    /*
     * For an explanation of these parameters, please see class constructor
     */
    private final BestAnchorIdentification bestAnchorIdentification;
    private final CoverageIdentification coverageIdentification;
    private final T explainedInstance;
    private final int explainedInstanceLabel;
    private final int maxAnchorSize;
    private final int beamSize;
    private final double delta;
    private final double epsilon;
    private final double tau;
    private final double tauDiscrepancy;
    private final int initSampleCount;
    private final boolean lazyCoverageEvaluation;
    private final boolean allowSuboptimalSteps;

    private final SamplingService samplingService;


    /**
     * Constructs the instance setting all required parameters
     *
     * @param bestAnchorIdentification Best-Arm identification algorithm
     * @param coverageIdentification   The function used to determine a candidate's coverage.
     * @param samplingService          the used sampling service
     * @param explainedInstance        The instance that is currently being explained
     * @param explainedInstanceLabel   Label of the instance that is being explained, i.e. what was its prediction?
     * @param maxAnchorSize            max combined features of the resulting anchor
     * @param beamSize                 parameter B: size of the current candidates for beam search
     * @param delta                    The delta value describing the probability of identifying the best arm
     *                                 == confidence
     * @param epsilon                  The maximum tolerated error == tolerance
     * @param tau                      The desired precision an anchor needs to achieve.
     *                                 If no candidate achieves at least this precision, the one with the best precision
     *                                 will be returned
     * @param tauDiscrepancy           Usually, it is practically infeasible to sample until the mean and the
     *                                 upper/lower bounds simultaneously fall below or above the tau threshold.
     *                                 Therefore, this variable may be introduced to control this discrepancy.
     * @param initSampleCount          The number of evaluations sampled for each candidate before it gets evaluated by
     *                                 the best arm identification algorithm. While theoretically, a guarantee that no
     *                                 candidates get discarded due to too few samples is provided by delta, using this
     *                                 argument has practical advantages.
     * @param lazyCoverageEvaluation   if set true, a candidate's coverage will only be determined when needed to, i.e.
     *                                 when extending or returning it
     * @param allowSuboptimalSteps     if set to false, candidates that are returned by the best arm identification get
     *                                 removed when their precision is lower than their parent's
     */
    AnchorConstruction(final BestAnchorIdentification bestAnchorIdentification,
                       final CoverageIdentification coverageIdentification,
                       final SamplingService samplingService,
                       final T explainedInstance, final int explainedInstanceLabel, final int maxAnchorSize,
                       final int beamSize, final double delta, final double epsilon, final double tau,
                       final double tauDiscrepancy,
                       final int initSampleCount,
                       boolean lazyCoverageEvaluation, boolean allowSuboptimalSteps) {
        if (bestAnchorIdentification == null)
            throw new IllegalArgumentException("Best anchor identification" + ParameterValidation.NULL_MESSAGE);
        if (coverageIdentification == null)
            throw new IllegalArgumentException("Coverage identification" + ParameterValidation.NULL_MESSAGE);
        if (samplingService == null)
            throw new IllegalArgumentException("Sampling service" + ParameterValidation.NULL_MESSAGE);
        if (explainedInstance == null)
            throw new IllegalArgumentException("Explained instance" + ParameterValidation.NULL_MESSAGE);
        if (!ParameterValidation.isUnsigned(explainedInstanceLabel))
            throw new IllegalArgumentException("Explained instance label" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);
        if (!ParameterValidation.isUnsigned(maxAnchorSize))
            throw new IllegalArgumentException("Max anchor size" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);
        if (!ParameterValidation.isUnsigned(beamSize))
            throw new IllegalArgumentException("Beam size" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);
        if (!ParameterValidation.isPercentage(delta))
            throw new IllegalArgumentException("Delta value" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        if (!ParameterValidation.isPercentage(epsilon))
            throw new IllegalArgumentException("Epsilon value" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        if (!ParameterValidation.isPercentage(tau))
            throw new IllegalArgumentException("Tau value" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        if (!ParameterValidation.isPercentage(tauDiscrepancy))
            throw new IllegalArgumentException("Tau discrepancy value" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        if (!ParameterValidation.isUnsigned(initSampleCount))
            throw new IllegalArgumentException("Initialization sample count" + ParameterValidation.NEGATIVE_VALUE_MESSAGE);

        this.bestAnchorIdentification = bestAnchorIdentification;
        this.coverageIdentification = coverageIdentification;
        this.explainedInstance = explainedInstance;
        this.explainedInstanceLabel = explainedInstanceLabel;
        this.maxAnchorSize = maxAnchorSize;
        this.beamSize = beamSize;
        this.delta = delta;
        this.epsilon = epsilon;
        this.tau = tau;
        this.tauDiscrepancy = tauDiscrepancy;
        this.initSampleCount = initSampleCount;
        this.lazyCoverageEvaluation = lazyCoverageEvaluation;
        this.allowSuboptimalSteps = allowSuboptimalSteps;
        this.samplingService = samplingService;
    }

    public SamplingService getSamplingService() {
        return samplingService;
    }

    private static String createKeyValueMap(Object... objects) {
        String[] results = new String[objects.length / 2];
        List<Object> list = Arrays.asList(objects);
        final Iterator<Object> iterator = list.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            final Object key = iterator.next();
            final Object value = iterator.next();
            results[i++] = key + "=" + value;
        }
        return String.join(", ", results);
    }

    /**
     * Generates a set of news anchors based on a previous best ones.
     * Anchors must reach a certain precision to be eligible to be taken into account.
     * <p>
     * According to Ribeiro:
     * <pre>
     * function GenerateCands(A, c)
     *      A_r = ∅
     *      for all A ∈ A; a_i ∈ x, a_i !∈ A do
     *          if cov(A ∧ a_i) > c then {Only high-coverage}
     *              A_r ← A_r ∪ (A ∧ a_i) {Add as potential anchor}
     *      return A_r {Candidate anchors for next round}
     * </pre>
     *
     * @param previousBest the current best anchors to extend
     * @param featureCount the number of features there are in the explained instance
     * @param minCoverage  the required coverage of an anchor to be eligible to be added to the result set.
     *                     As coverage can only decrease with an increasing feature count, the algorithm does not need
     *                     to care about potential candidates that have a lower coverage than an already found anchor
     * @return a set of anchors extending the passed anchor, matching the required conditions
     */
    private List<AnchorCandidate> generateCandidateSet(final List<AnchorCandidate> previousBest, final int featureCount,
                                                       final double minCoverage) {
        final List<AnchorCandidate> result = new ArrayList<>();
        final Set<AnchorCandidate> intermediateResult = new HashSet<>();
        final Set<Set<Integer>> createdFeatureSet = new HashSet<>();
        // Loop over every available features
        for (final Integer additionalFeature : IntStream.range(0, featureCount).boxed().collect(Collectors.toList())) {
            // if we don't have any anchor to extend then we are in the first round
            // and our current feature is a candidate
            if (previousBest == null || previousBest.isEmpty()) {
                AnchorCandidate candidate = new AnchorCandidate(
                        new LinkedHashSet<>(Collections.singletonList(additionalFeature)),
                        null);
                intermediateResult.add(candidate);
                continue;
            }
            // Loop over the candidates we are going to extend
            for (final AnchorCandidate candidate : previousBest) {
                // If new element is already contained in the candidate go to the next one
                if (candidate.getCanonicalFeatures().contains(additionalFeature))
                    continue;

                final Set<Integer> extendedCandidate = new LinkedHashSet<>(candidate.getOrderedFeatures());
                extendedCandidate.add(additionalFeature);

                HashSet<Integer> unorderedSet = new HashSet<>(extendedCandidate);
                if (createdFeatureSet.contains(unorderedSet))
                    continue;
                createdFeatureSet.add(unorderedSet);

                intermediateResult.add(new AnchorCandidate(extendedCandidate, candidate));
            }
        }
        // Only accept those candidates that have a certain minimum coverage
        for (final AnchorCandidate candidate : intermediateResult) {
            // Only calculate coverage when lazyCoverageEvaluation is set false
            if (!lazyCoverageEvaluation)
                calculateCandidateCoverage(candidate);
            // Construct candidate
            if (minCoverage > 0) {
                // A minimum coverage has been set, so we already need to calculate the coverage right now
                if (lazyCoverageEvaluation)
                    calculateCandidateCoverage(candidate);
                if (candidate.getCoverage() < minCoverage) {
                    // No longer consider this candidate
                    continue;
                }
            }
            result.add(candidate);
        }
        return result;
    }

    /**
     * Finding best candidates may be formulated as an optimization problem
     * (considered a "pure-exploration bandit-problem").
     * <p>
     * Method uses a specified exploration algorithm {@link BestAnchorIdentification}
     *
     * @param topN       the amount of candidates to choose
     * @param candidates the candidate set to chose from
     * @return the result of the algorithm, i.e. the list of best candidates
     */
    private List<AnchorCandidate> bestCandidate(final List<AnchorCandidate> candidates, final int topN) {
        // Ensure all candidates have initSampleCount taken
        SamplingSession session = samplingService.createSession(explainedInstanceLabel);
        for (final AnchorCandidate candidate : candidates) {
            if (candidate.getSampledSize() >= initSampleCount)
                continue;
            session.registerCandidateEvaluation(candidate, initSampleCount - candidate.getSampledSize());
        }
        session.run();

        if (candidates.size() <= topN) {
            // All features are to be selected, hence return all candidates
            LOGGER.debug("Number of arms searched for less or equals total number of features. " +
                    "Returning all candidates.");
            return new ArrayList<>(candidates);
        }

        LOGGER.debug("Calling {} to identify top {} candidates with a significance level of {}",
                bestAnchorIdentification.getClass().getSimpleName(), topN, delta);
        // Discard all found candidates that have a precision of 0
        return bestAnchorIdentification.identify(candidates, samplingService, explainedInstanceLabel, delta, epsilon, topN);
    }

    /**
     * The best-arm identification algorithms can identify the best candidates out of a given set of candidates
     * and make various theoretical guarantees, however, they do not necessarily adhere to the confidence bounds set
     * by the user.
     * <p>
     * Therefore, this method checks whether the candidate meets the precision criteria by repeatedly sampling until it
     * is either assured the candidate is in fact an anchor, or not.
     *
     * @param candidate      the candidate to validate
     * @param actualBeamSize the actual beam size. As the beam size is constrained to be smaller than the actually found
     *                       candidates there needs to be this adjustment in edge cases
     * @return true, if the candidate adheres to the constraints, false otherwise
     */
    private boolean isValidCandidate(final AnchorCandidate candidate, final int actualBeamSize) {
        // I can choose at most (beamSize - 1) tuples at each step and there are at most featureCount steps
        final double beta = Math.log(1 / (delta / (1 + (actualBeamSize - 1) * explainedInstance.getFeatureCount())));
        double mean = candidate.getPrecision();

        double lb = KLBernoulliUtils.dlowBernoulli(mean, beta / candidate.getSampledSize());
        double ub = KLBernoulliUtils.dupBernoulli(mean, beta / candidate.getSampledSize());

        // If prec_lb(A) < tau but prec_ub(A) > tau it needs to be sampled ...
        while ((mean >= tau && lb < tau - tauDiscrepancy) ||
                (mean < tau && ub >= tau + tauDiscrepancy)) {
            LOGGER.debug("Cannot confirm or reject {} is an anchor. Taking more samples.",
                    candidate.getCanonicalFeatures());
            samplingService.createSession(explainedInstanceLabel)
                    .registerCandidateEvaluation(candidate, initSampleCount).run();
            mean = candidate.getPrecision();
            lb = KLBernoulliUtils.dlowBernoulli(mean, beta / candidate.getSampledSize());
            ub = KLBernoulliUtils.dupBernoulli(mean, beta / candidate.getSampledSize());
        }

        // ... until we are either confident A is
        //  - an anchor     (prec_lb(A) > tau) or
        //  - not an anchor (prec_ub(A) < tau)
        return mean >= tau && lb > tau - tauDiscrepancy;
    }

    /**
     * Calculates a candidate's coverage if not already done
     *
     * @param candidate the candidate
     */
    private void calculateCandidateCoverage(AnchorCandidate candidate) {
        if (!candidate.isCoverageUndefined())
            return;
        candidate.setCoverage(coverageIdentification.calculateCoverage(candidate.getCanonicalFeatures()));
    }

    /**
     * BeamSearch algorithm to extend the greedy LUCB approach.
     * Maintains multiple anchor rules in order to be able to "undo" suboptimal choices.
     *
     * <p>
     * According to Ribeiro:
     *
     * <pre>
     * function BeamSearch(f, x, D, τ)
     *      hyperparameters B, e, δ
     *      A∗ ← null, A_0 ← ∅   {Set of candidate rules}
     *      loop
     *          A_t ← GenerateCands(A_t−1 , cov(A∗))
     *          A_t ← B-BestCand(A_t, D, B, δ, tau)   {LUCB}
     *          if A_t = ∅ then break loop
     *          for all A ∈ A_t s.t. prec_lb(A, δ) > τ do
     *              if cov(A) > cov(A∗) then A∗ ← A
     *      return A∗
     * </pre>
     *
     * @return the {@link AnchorResult} of the beam-search
     * @throws NoCandidateFoundException if no single candidate with a precision greater than 0 could be found.
     */
    private AnchorResult<T> beamSearch() throws NoCandidateFoundException {
        LOGGER.info("Starting beam search with beam width {} and a max anchor size of {}", beamSize, maxAnchorSize);
        final double startTime = System.currentTimeMillis();

        int currentSize = 1;
        final Map<Integer, List<AnchorCandidate>> bestOfSize = new HashMap<>();
        AnchorCandidate bestCandidate = null;

        boolean stopLoop = false;
        while (currentSize <= maxAnchorSize && !stopLoop) {
            LOGGER.info("Adding feature {} of {}", currentSize, maxAnchorSize);
            // Generate candidates based on previous round's best candidates
            final List<AnchorCandidate> anchorCandidates = generateCandidateSet(bestOfSize.get(currentSize - 1),
                    explainedInstance.getFeatureCount(), (bestCandidate != null) ? bestCandidate.getCoverage() : 0);
            // If - for whatever reason - no more candidates can be identified, quit search
            if (anchorCandidates.size() == 0)
                break;

            // Identify this round's best candidates
            final int bestCandidateCount = Math.min(anchorCandidates.size(), beamSize);
            final List<AnchorCandidate> bestCandidates = bestCandidate(anchorCandidates, bestCandidateCount);
            // However, filter candidates that have a precision of 0.
            // Or such that decrease their parents precision
            final Iterator<AnchorCandidate> iterator = bestCandidates.iterator();
            while (iterator.hasNext()) {
                final AnchorCandidate candidate = iterator.next();
                // If anchorCandidate size <= beam size, then all candidates get returned without being sampled.
                // Thus, these may not be removed
                if (anchorCandidates.size() > bestCandidateCount && candidate.getPrecision() <= 0) {
                    LOGGER.warn("Removing candidate {} as its precision is 0", candidate.getOrderedFeatures());
                    iterator.remove();
                } else if (!allowSuboptimalSteps && candidate.getAddedPrecision() <= 0) {
                    LOGGER.warn("Removing candidate {} as it decreases its parent's precision",
                            candidate.getOrderedFeatures());
                    iterator.remove();
                }
            }
            if (bestCandidates.isEmpty()) {
                LOGGER.warn("No valid candidates found during best arm identification. Stopping search.");
                break;
            }
            bestOfSize.put(currentSize, bestCandidates);

            // For each candidate check whether it
            for (final AnchorCandidate candidate : bestCandidates) {
                final boolean isValidCandidate = isValidCandidate(candidate, bestCandidateCount);
                LOGGER.info("Top candidate {} is{} a valid anchor with precision {}",
                        candidate.getCanonicalFeatures(), (isValidCandidate) ? "" : " not", candidate.getPrecision());
                // The best candidates returned do not necessarily have the right confidence constraints
                // Check if this candidate is valid, i.e. adheres to the set constraints.
                // Only then it can be a valid result candidate
                // However, still save the "invalid" anchors in case no candidate adheres to the constraints

                if (isValidCandidate) {
                    // If by here the coverage still has not been calculated, do it
                    calculateCandidateCoverage(candidate);

                    // See if current anchor has better coverage then previously bet one
                    if (bestCandidate == null || candidate.getCoverage() > bestCandidate.getCoverage()) {
                        LOGGER.info("Found a new best anchor ({}) with a coverage of {}", candidate.getCanonicalFeatures(),
                                candidate.getCoverage());
                        bestCandidate = candidate;
                        if (candidate.getCoverage() == 1) {
                            LOGGER.info("Found an anchor with a coverage of 1. Stopping search prematurely.");
                            stopLoop = true;
                        }
                    }
                }
            }
            currentSize++;
        }

        // No anchor could be found. Now return best anchor out of all rounds
        final boolean isAnchor = (bestCandidate != null);
        if (bestCandidate == null) {
            LOGGER.warn("Could not identify an anchor satisfying the parameters." +
                    "Searching for best candidate.");
            final List<AnchorCandidate> allCandidates = bestOfSize.values().stream().flatMap(List::stream)
                    .collect(Collectors.toList());
            final List<AnchorCandidate> bestCandidates = bestCandidate(allCandidates, 1);
            if (bestCandidates == null || bestCandidates.isEmpty()) {
                LOGGER.warn("Could not find an Anchor or any candidate with a precision > 0. " +
                        "Throwing NoCandidateFoundException.");
                throw new NoCandidateFoundException();
            }
            bestCandidate = bestCandidates.get(0);
            // As the candidate is no anchor, its coverage has not yet been calculated
            calculateCandidateCoverage(bestCandidate);
            LOGGER.warn("No anchor found, returning best candidate");
        }

        final double timeSpent = System.currentTimeMillis() - startTime;
        LOGGER.info("Found result {} in {}ms", bestCandidate, timeSpent);
        return new AnchorResult<>(bestCandidate, explainedInstance, explainedInstanceLabel, isAnchor,
                timeSpent, samplingService.getTimeSpentSampling());
    }

    /**
     * Constructs the anchor given the specified algorithms and parameters.
     * <p>
     * For more information regarding the Anchors algorithm, see
     * <a href=https://github.com/viadee/javaAnchorExplainer>https://github.com/viadee/javaAnchorExplainer</a>
     * <p>
     * In case no anchors get found or, in general, the precision is bad, there could be multiple reasons for this:
     * <ul>
     * <li>The perturbation function is of low quality</li>
     * <li>The precision for the current prediction is low as it might be a thin prediction</li>
     * <li>The beam size is too small and suboptimal choices have been made</li>
     * </ul>
     * <p>
     * After calculation the optional ExecutorService in SamplingService is shutdown.
     *
     * @return the {@link AnchorResult} of the best-anchor identification and beam-search
     * @throws NoCandidateFoundException if no single candidate with a precision &gt; 0 could be found.
     */
    public AnchorResult<T> constructAnchor() throws NoCandidateFoundException {
        return this.constructAnchor(true);
    }

    /**
     * Constructs the anchor given the specified algorithms and parameters.
     * <p>
     * For more information regarding the Anchors algorithm, see
     * <a href=https://github.com/viadee/javaAnchorExplainer>https://github.com/viadee/javaAnchorExplainer</a>
     * <p>
     * In case no anchors get found or, in general, the precision is bad, there could be multiple reasons for this:
     * <ul>
     * <li>The perturbation function is of low quality</li>
     * <li>The precision for the current prediction is low as it might be a thin prediction</li>
     * <li>The beam size is too small and suboptimal choices have been made</li>
     * </ul>
     *
     * @param endSamplingService if true and needed the ExecutorService in SamplingService is shutdown
     * @return the {@link AnchorResult} of the best-anchor identification and beam-search
     * @throws NoCandidateFoundException if no single candidate with a precision &gt; 0 could be found.
     */
    public AnchorResult<T> constructAnchor(boolean endSamplingService) throws NoCandidateFoundException {
        LOGGER.info("Starting Anchor Construction for instance {} and label {} with params: {}",
                explainedInstance, explainedInstanceLabel,
                createKeyValueMap(
                        "maxAnchorSize", maxAnchorSize,
                        "beamSize", beamSize,
                        "delta", delta,
                        "epsilon", epsilon,
                        "tau", tau,
                        "tauDiscrepancy", tauDiscrepancy,
                        "initSampleCount", initSampleCount,
                        "lazyCoverageEvaluation", lazyCoverageEvaluation,
                        "allowSuboptimalSteps", allowSuboptimalSteps));

        AnchorResult<T> result = beamSearch();
        if (endSamplingService) {
            this.samplingService.endSampling();
        }

        return result;
    }

}
