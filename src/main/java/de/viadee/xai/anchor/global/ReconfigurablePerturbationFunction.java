package de.viadee.xai.anchor.global;

import de.viadee.xai.anchor.DataInstance;
import de.viadee.xai.anchor.PerturbationFunction;

/**
 * Interface marking perturbation functions that may be used in conjunction with the {@link SubmodularPick} algorithm.
 *
 * @param <T> the type of instance that gets perturbed.
 */
public interface ReconfigurablePerturbationFunction<T extends DataInstance<?>> extends PerturbationFunction<T> {
    /**
     * Initializes the class to create perturbations for another instance.
     * As perturbations are usually immutable, a new instance may be returned.
     * <p>
     * Additionally, the SP-algorithm distributes explaining instances, hence perturbation
     * functions are called in parallel.
     * Therefore, this method should return a new immutable class, perturbing the specified instance only.
     *
     * @param instance the instance to initialize the function for
     * @return an instance of the {@link PerturbationFunction}
     */
    PerturbationFunction<T> createForInstance(T instance);
}
