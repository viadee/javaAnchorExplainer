package de.goerke.tobias.anchorj.tabular;

import de.goerke.tobias.anchorj.base.AnchorResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * May be used to visualize an instance of the algorithms result for the user.
 */
public class TabularInstanceVisualizer {
    private final Map<TabularFeature, Map<Integer, Object>> invertedMappings;

    /**
     * Constructs the instance.
     *
     * @param mappings the mappings used for transforming values
     */
    public TabularInstanceVisualizer(Map<TabularFeature, Map<Object, Integer>> mappings) {
        this.invertedMappings = new LinkedHashMap<>();
        for (Map.Entry<TabularFeature, Map<Object, Integer>> entry : mappings.entrySet()) {
            this.invertedMappings.put(entry.getKey(), invertMap(entry.getValue()));
        }
    }

    /**
     * Formats the result readable to the user.
     *
     * @param anchorResult the algorithm's result
     * @return a String visualizing the result
     */
    public String visualizeResult(AnchorResult<TabularInstance> anchorResult) {
        final String[] text = instanceToText(anchorResult.getInstance());
        final String[] explanation = new String[anchorResult.getOrderedFeatures().size()];
        int index = 0;
        for (Integer featureNr : anchorResult.getOrderedFeatures()) {
            explanation[index++] = text[featureNr];
        }
        return "IF (" + String.join(" AND " + System.lineSeparator(), explanation) + ") THEN PREDICT " + getLabelMapping().get(anchorResult.getLabel());
    }

    /**
     * Formats an instance readable to the user.
     *
     * @param explainedInstance      the explained instance
     * @param explainedInstanceLabel the explained instance's label
     * @return a String visualizing the result
     */
    public String visualizeInstance(TabularInstance explainedInstance, int explainedInstanceLabel) {
        return String.join(" AND ", instanceToText(explainedInstance)) +
                " with label " + getLabelMapping().get(explainedInstanceLabel);
    }

    private String[] instanceToText(TabularInstance explainedInstance) {
        final List<String> result = new ArrayList<>();
        int i = 0;
        for (Map.Entry<TabularFeature, Map<Integer, Object>> entry : invertedMappings.entrySet()) {
            if (i >= explainedInstance.getFeatureCount())
                break;
            Object value = entry.getValue().get(explainedInstance.getInstance()[i]);
            String tmp = (value instanceof String) ? (String) value : String.valueOf(value);
            result.add(entry.getKey().getName() + " = " + tmp);
            i++;
        }
        return result.toArray(new String[0]);
    }

    private Map<Integer, Object> getLabelMapping() {
        return invertedMappings.entrySet().stream().filter(e -> e.getKey().isTargetFeature()).map(Map.Entry::getValue)
                .findFirst().orElseThrow(IllegalArgumentException::new);
    }

    private static <K, V> Map<V, K> invertMap(Map<K, V> toInvert) {
        final Map<V, K> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : toInvert.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }
}
