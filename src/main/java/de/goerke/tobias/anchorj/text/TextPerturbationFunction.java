package de.goerke.tobias.anchorj.text;

import de.goerke.tobias.anchorj.base.PerturbationFunction;
import de.goerke.tobias.anchorj.util.ParameterValidation;

import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * This class provides a basic perturbation function for text instances.
 * <p>
 * For an instance, a list of alternative words for each feature will be specified.
 * When perturbing the instance, each token will be changed with a probability specified.
 * <p>
 * This class does not provide a real implementation as no third party libraries are included in the core algorithm.
 * In order to perturb text, please implement e.g. a Word2Vec model providing the alternatives needed.
 */
public class TextPerturbationFunction implements PerturbationFunction<TextInstance> {
    private final TextInstance textInstance;
    private final double tokenChangeProbability;
    private final List<List<String>> alternatives;

    private final Random random = new Random();

    /**
     * Instantiates a new instance.
     *
     * @param instance               the instance
     * @param tokenChangeProbability the token change probability
     * @param alternatives           the alternatives for each word
     */
    @SuppressWarnings("unchecked")
    protected TextPerturbationFunction(final TextInstance instance, final double tokenChangeProbability,
                                       final List<List<String>> alternatives) {
        if (!ParameterValidation.isPercentage(tokenChangeProbability))
            throw new IllegalArgumentException("Token change probability" + ParameterValidation.NOT_PERCENTAGE_MESSAGE);
        if (alternatives.size() != instance.getFeatureCount())
            throw new IllegalArgumentException("There must be exactly one (either full or empty) list " +
                    "of alternatives per token");

        this.textInstance = instance;
        this.tokenChangeProbability = tokenChangeProbability;
        this.alternatives = alternatives;
    }

    /**
     * Gets a (random) alternative for the token at the specified index.
     *
     * @param index the index
     * @return the alternative
     */
    private String getAlternative(final int index) {
        final List<String> currentAlternatives = this.alternatives.get(index);
        return currentAlternatives.get(random.nextInt(currentAlternatives.size()));
    }

    /**
     * Use the tokenChangeProbability to determine whether to swap for an alternative.
     *
     * @param index the index
     * @return the string
     */
    protected String doPerturb(final int index) {
        String perturbedToken = textInstance.getInstance()[index];
        if (random.nextDouble() <= tokenChangeProbability) {
            perturbedToken = getAlternative(index);
        }
        return perturbedToken;
    }

    @Override
    public PerturbationFunction<TextInstance> createForInstance(final TextInstance instance) {
        return new TextPerturbationFunction(instance, tokenChangeProbability, alternatives);
    }

    @Override
    public PerturbationResult<TextInstance> perturb(final Set<Integer> immutableFeaturesIdx, final int nrPerturbations) {
        final String[][] tempRawResult = new String[textInstance.getInstance().length][nrPerturbations];
        final boolean[][] result = new boolean[nrPerturbations][textInstance.getInstance().length];

        for (int i = 0; i < textInstance.getInstance().length; i++) {
            boolean immutableToken = immutableFeaturesIdx != null && immutableFeaturesIdx.contains(i);
            final String token = textInstance.getInstance()[i];
            tempRawResult[i] = new String[nrPerturbations];
            for (int j = 0; j < nrPerturbations; j++) {
                String perturbedToken = token;
                if (!immutableToken) {
                    perturbedToken = doPerturb(i);
                    if (!token.equals(perturbedToken))
                        result[j][i] = true;
                }
                tempRawResult[i][j] = perturbedToken;
            }
        }

        final TextInstance[] rawResult = new TextInstance[nrPerturbations];
        for (int i = 0; i < nrPerturbations; i++) {
            final String[] tokens = new String[textInstance.getFeatureCount()];
            for (int j = 0; j < tokens.length; j++)
                tokens[j] = tempRawResult[j][i];
            rawResult[i] = new TextInstance(tokens);
        }

        return new PerturbationResultImpl<>(rawResult, result);
    }

    /**
     * Gets alternatives.
     *
     * @return the alternatives
     */
    protected List<List<String>> getAlternatives() {
        return alternatives;
    }

    /**
     * Gets random object for reuse.
     *
     * @return the random
     */
    protected Random getRandom() {
        return random;
    }
}
