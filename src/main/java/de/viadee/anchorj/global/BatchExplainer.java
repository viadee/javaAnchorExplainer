package de.viadee.anchorj.global;

import java.io.Serializable;
import java.util.List;

import de.viadee.anchorj.AnchorConstruction;
import de.viadee.anchorj.AnchorConstructionBuilder;
import de.viadee.anchorj.AnchorResult;
import de.viadee.anchorj.DataInstance;

/**
 * This interface is used to obtain multiple Anchors explanations at once.
 * <p>
 * Usually, this enables global explanations.
 * <p>
 * The client is free to define his/her batch explainer as to e.g. use distribution framework, such as Spark.
 *
 * @param <T> Type of the explained instance
 */
public interface BatchExplainer<T extends DataInstance<?>> extends Serializable {

    /**
     * Obtains explanations for the given instances.
     * <p>
     * Implements the optimization policy.
     *
     * @param anchorConstructionBuilder the builder used to create instances of the {@link AnchorConstruction}
     *                                  when running the algorithm.
     * @param instances                 the instances to be explained
     * @return the resulting explanations
     */
    AnchorResult<T>[] obtainAnchors(AnchorConstructionBuilder<T> anchorConstructionBuilder, List<T> instances);
}
