package onethreeseven.roi.model;

/**
 * A grid cell used for RoI mining.
 * @author Luke Bermingham.
 */
public class RoIGridCell extends MiningCell implements NdCell {

    private final int[] ndIndices;

    public RoIGridCell(int index, int[] ndIndices) {
        super(index);
        this.ndIndices = ndIndices;
    }

    @Override
    public int[] getIndices() {
        return ndIndices;
    }

}
