package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.DataInstance;

/**
 * Represents an instance (i.e. row) of a data table
 */
public class TabularInstance implements DataInstance<Object[]> {
    private final Object[] instance;

    /**
     * Constructs the instance
     *
     * @param instance the instance array containing one element for each column
     */
    public TabularInstance(Object[] instance) {
        this.instance = instance;
    }

    /**
     * Copying constructor
     *
     * @param instance the instance to be copied
     */
    TabularInstance(TabularInstance instance) {
        Object[] copy = new Object[instance.getInstance().length];
        System.arraycopy(instance.getInstance(), 0, copy, 0, instance.getInstance().length);
        this.instance = copy;
    }

    @Override
    public Object[] getInstance() {
        return instance;
    }

//    @Override
//    public Object getFeature(int featureId) {
//        return instance[featureId];
//    }

    @Override
    public int getFeatureCount() {
        return instance.length;
    }
}
