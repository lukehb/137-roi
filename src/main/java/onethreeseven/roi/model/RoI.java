package onethreeseven.roi.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Represent a region of interest.
 * The representation depends on the algorithm.
 * Typically the RoI stores a collection of cell indices.
 * @author Luke Bermingham
 */
public class RoI implements Iterable<Integer> {

    private final Set<Integer> cells;
    protected double density = 0;
    private int id;

    public RoI(int id) {
        this.id = id;
        this.cells = new HashSet<>();

    }

    public RoI(DensityCell cell, int id) {
        this(id);
        this.add(cell);
    }

    public double getDensity() {
        return density;
    }

    public int size() {
        return cells.size();
    }

    public void add(RoI roi) {
        cells.addAll(roi.cells);
        density = roi.getDensity();
    }

    public void add(DensityCell cell) {
        boolean added = cells.add(cell.getIndex());
        if(added){
            this.density += cell.getDensity();
        }

    }

    public void clear() {
        cells.clear();
        density = 0;
    }

    public boolean contains(Integer indices) {
        return cells.contains(indices);
    }

    public int getId() {
        return id;
    }

    public Set<Integer> getCells() {
        return cells;
    }

    @Override
    public Iterator<Integer> iterator() {
        return cells.iterator();
    }

    @Override
    public String toString() {
        return "RoI {" +
                " id= " + id +
                " density=" + density +
                '}';
    }

}
