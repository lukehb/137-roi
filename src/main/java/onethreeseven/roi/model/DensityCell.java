package onethreeseven.roi.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A cell that tracks its own density.
 * Density is a useful property for mining.
 * Density in this context refers to how many entities exist (or passed through)
 * this cell.
 * @author Luke Bermingham
 */
public class DensityCell extends IndexedCell {

    /**
     * Each entity that passes through this cell has its id recorded and tallied.
     * The reason for the tally is that potentially the same entity can pass through
     * a cell multiple times.
     */
    private final Map<String, Integer> densityMap;

    private final int index;
    private int totalDensity = 0;

    public DensityCell(int index) {
        this.index = index;
        densityMap = new HashMap<>();
    }

    @Override
    public int getIndex() {
        return index;
    }

    public void incrementTally(String id) {
        //increment the id by one
        densityMap.put(id, densityMap.getOrDefault(id, 0) + 1);
        totalDensity++;
    }

    public void clear() {
        densityMap.clear();
    }

    /**
     * @return The number of unique visits to this cell, i.e each entity's visit only counts once
     */
    public int getDensity() {
        return densityMap.size();
    }

    /**
     * @return The total number of visits (even by the same entity).
     */
    public int getTotalDensity(){
        return totalDensity;
    }

    /**
     * Gets the tally of the ids passed in, or SPECIAL CASE: if null is passed in gets the tally
     * of the entire cell (assuming there is some density to tally)
     * @param ids the ids of the entities to find density for, if null just return total density
     * @return the density
     */
    public int getDensityById(String... ids) {
        if (ids == null) {
            return getDensity();
        }
        int cumulataiveTally = 0;
        for (String intersectorId : ids) {
            cumulataiveTally += densityMap.getOrDefault(intersectorId, 0);
        }
        return cumulataiveTally;
    }

    public String containsOne(String... ids) {
        for (String intersectorId : ids) {
            if (densityMap.containsKey(intersectorId)) {
                return intersectorId;
            }
        }
        return null;
    }

    public boolean containsAll(String... ids) {
        int counter = 0;
        for (String intersectorId : ids) {
            if (densityMap.containsKey(intersectorId)) {
                counter++;
            }
        }
        return (counter == ids.length);
    }

    public Set<String> getMatchingIds(String... ids) {
        if (ids == null) {
            return null;
        }
        HashSet<String> containedIds = new HashSet<String>();
        for (String intersectorId : ids) {
            if (densityMap.containsKey(intersectorId)) {
                containedIds.add(intersectorId);
            }
        }
        return containedIds;
    }

    public Set<String> getKeys() {
        return densityMap.keySet();
    }

}
