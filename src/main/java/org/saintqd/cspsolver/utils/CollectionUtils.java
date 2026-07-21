package org.saintqd.cspsolver.utils;

import java.util.*;

public class CollectionUtils {

    public static <K, V> List<Map<K, V>> splitMapBySize(Map<K, V> originalMap, int batchSize) {
        if (batchSize <= 0 || batchSize >= originalMap.size()) {
            return Collections.singletonList(originalMap);
        }

        List<Map<K, V>> batches = new ArrayList<>();
        Map<K, V> tempMap = new HashMap<>();
        int count = 0;

        for (Map.Entry<K, V> entry : originalMap.entrySet()) {
            tempMap.put(entry.getKey(), entry.getValue());
            count++;

            if (count == batchSize) {
                // Add a copy of the temporary map to the list of batches
                batches.add(new HashMap<>(tempMap));
                tempMap.clear(); // Clear the temporary map for the next batch
                count = 0;
            }
        }

        // Add any remaining entries that didn't fill a complete batch
        if (!tempMap.isEmpty()) {
            batches.add(new HashMap<>(tempMap));
        }

        return batches;
    }
}
