package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.DataInstance;

/**
 * Represents an instance (i.e. row) of a data table
 */
public class TabularInstance implements DataInstance<Integer[]> {
    private final Integer[] instance;

    /**
     * Constructs the instance
     *
     * @param instance the instance array containing one element for each column
     */
    public TabularInstance(Integer[] instance) {
        this.instance = instance;
    }

    /**
     * Copying constructor
     *
     * @param instance the instance to be copied
     */
    TabularInstance(TabularInstance instance) {
        Integer[] copy = new Integer[instance.getInstance().length];
        System.arraycopy(instance.getInstance(), 0, copy, 0, instance.getInstance().length);
        this.instance = copy;
    }

    // public static TabularInstance[] fromArray(Integer[][] array) {
    //     TabularInstance[] result = new TabularInstance[array.length];
    //     for (int i = 0; i < result.length; i++)
    //         result[i] = new TabularInstance(array[i]);
    //     return result;
    // }

    @Override
    public Integer[] getInstance() {
        return instance;
    }

    @Override
    public int getFeatureCount() {
        return instance.length;
    }
}
