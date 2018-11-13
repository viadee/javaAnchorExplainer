package de.goerke.tobias.anchorj.tabular;

import java.util.function.Function;

/**
 */
public class ColumnDescription {

    private final TabularFeature.ColumnType columnType;
    private final String name;
    private final boolean doUse;
    private final boolean isTargetFeature;
    private final Function<Object[], Object[]> transformation;
    private final Function<Number[], Integer[]> discretizer;

    public ColumnDescription(TabularFeature.ColumnType columnType, String name, boolean doUse,
                             boolean isTargetFeature, Function<Object[], Object[]> transformation,
                             Function<Number[], Integer[]> discretizer) {
        this.columnType = columnType;
        this.name = name;
        this.doUse = doUse;
        this.isTargetFeature = isTargetFeature;
        this.transformation = transformation;
        this.discretizer = discretizer;
    }

    public TabularFeature.ColumnType getColumnType() {
        return columnType;
    }

    public String getName() {
        return name;
    }

    public boolean isDoUse() {
        return doUse;
    }

    public boolean isTargetFeature() {
        return isTargetFeature;
    }

    public Function<Object[], Object[]> getTransformation() {
        return transformation;
    }

    public Function<Number[], Integer[]> getDiscretizer() {
        return discretizer;
    }
}
