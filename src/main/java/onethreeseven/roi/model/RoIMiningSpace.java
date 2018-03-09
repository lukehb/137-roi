package onethreeseven.roi.model;

import java.util.Collection;

/**
 * Some kind of n-d space that trajectories pass through.
 * It partitions the study region (i.e grid, triangular irregular network etc.).
 * Then trajectories pass through it, and the cells they pass through have their density incremented.
 * @author Luke Bermingham
 */
public interface RoIMiningSpace {

    /**
     * Set all cells in grid to be unprocessed (i.e. reset for mining again).
     */
    void unprocessAll();

    /**
     * @return The dense cells the trajectory passed through.
     * In this context dense means, number of trajectories passing through.
     * In order of most dense to least dense.
     */
    Collection<? extends MiningCell> getDenseCells();

    /**
     * Neighbours cells in the mining context must have some shared data with the query cell.
     * In 2d this means a shared edge, in 3d this means a shared face
     *
     * @param queryCell the query cell
     * @return the neighbour cells indices
     */
    Collection<MiningCell> getNeighbourCells(MiningCell queryCell);

    /**
     * An RoI is said to be sparse if it missing indices in between its extreme indices.
     *
     * @param roi The RoI, which may or may not be sparse
     * @return the same RoI with sparsity removed (if possible), otherwise null
     */
    RoI removeSparsity(RoI roi);

    /**
     * @param idx index into this space
     * @return the cell corresponding to this index
     */
    MiningCell getCell(int idx);

}
