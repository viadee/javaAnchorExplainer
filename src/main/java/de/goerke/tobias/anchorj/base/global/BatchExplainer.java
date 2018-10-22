package de.goerke.tobias.anchorj.base.global;

import de.goerke.tobias.anchorj.base.AnchorConstruction;
import de.goerke.tobias.anchorj.base.AnchorConstructionBuilder;
import de.goerke.tobias.anchorj.base.AnchorResult;
import de.goerke.tobias.anchorj.base.DataInstance;

import java.util.List;

/**
 * This interface is used to obtain multiple Anchors explanations at once.
 * <p>
 * Usually, this enables global explanations.
 * <p>
 * The client is free to define his/her batch explainer as to e.g. use distribution framework, such as Spark.
 *
 * @param <T> Type of the explained instance
 */
public interface BatchExplainer<T extends DataInstance<?>> {

    /**
     * Obtains explanations for the given instances.
     * <p>
     * Implements the optimization policy.
     *
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     * @param instances                 the instances to be explained
     */
    AnchorResult<T>[] obtainAnchors(AnchorConstructionBuilder<T> anchorConstructionBuilder, List<T> instances);
}
