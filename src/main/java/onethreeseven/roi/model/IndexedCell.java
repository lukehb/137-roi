package onethreeseven.roi.model;

/**
 * A general representation of a n-dimensional cell.
 * The cell is tightly coupled to a particular kind of space partition, for example
 * a grid, a triangulation.. etc and the index is used to refer to that specific cell.
 * @author Luke Bermingham
 */
public abstract class IndexedCell {

    public abstract int getIndex();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexedCell)) return false;
        IndexedCell indexedCell = (IndexedCell) o;
        return getIndex() == indexedCell.getIndex();

    }

    @Override
    public int hashCode() {
        return getIndex();
    }
}
