package de.goerke.tobias.anchorj.base;

import java.util.Set;

/**
 * Interface providing basic functionality for disturbing instances.
 *
 * @param <T> the type of instance that gets perturbed.
 */
public interface PerturbationFunction<T extends DataInstance<?>> {

    /**
     * Initializes the class to create perturbations for another instance.
     * As perturbations are usually immutable, a new instance may be returned.
     * <p>
     * Only needed when using the SP-algorithm
     *
     * @param instance the instance to initialize the function for
     * @return an instance of the {@link PerturbationFunction}
     */
    default PerturbationFunction<T> createForInstance(T instance) {
        throw new UnsupportedOperationException("createForInstance must be implemented to use SubmodularPick.");
    }

    /**
     * Perturbs an instance to create a possibly similar instance.
     * <p>
     * How an instance gets perturbed is highly application-specific.
     *
     * @param immutableFeaturesIdx the features not to be perturbed as they are specified by an {@link AnchorCandidate}
     * @param nrPerturbations      the number of perturbations to create
     * @return the {@link PerturbationResultImpl} containing the perturbed instances
     */
    PerturbationResult<T> perturb(Set<Integer> immutableFeaturesIdx, int nrPerturbations);

    /**
     * Represents the result of a perturbation run.
     *
     * @param <T> the type of the perturbed instance
     */
    interface PerturbationResult<T> {
        /**
         * @return the raw result of type T
         */
        T[] getRawResult();

        /**
         * @return an array mapping each perturbation (dimension 1) and its features (dimension 2) to
         * whether they have been changed, i.e. are different than in the original instance
         */
        boolean[][] getFeatureChanged();
    }

    /**
     * Basic implementation of the PerturbationResultImpl
     */
    class PerturbationResultImpl<T> implements PerturbationResult<T> {
        private final T[] rawResult;
        private final boolean[][] featureChanged;

        /**
         * Creates the instance.
         *
         * @param rawResult      the raw result of type T
         * @param featureChanged an array mapping each perturbation (dimension 1) and its features (dimension 2) to
         *                       whether they have been changed, i.e. are different than in the original instance
         */
        public PerturbationResultImpl(T[] rawResult, boolean[][] featureChanged) {
            if (rawResult.length != featureChanged.length)
                throw new IllegalArgumentException("Raw results is of different size than changed features size");
            this.rawResult = rawResult;
            this.featureChanged = featureChanged;
        }


        @Override
        public T[] getRawResult() {
            return rawResult;
        }

        @Override
        public boolean[][] getFeatureChanged() {
            return featureChanged;
        }
    }
}
