package onethreeseven.roi.model;

import onethreeseven.common.util.Maths;
import onethreeseven.common.util.NDUtil;
import onethreeseven.roi.algorithm.AbstractRoIMining;
import onethreeseven.trajsuitePlugin.model.BoundingCoordinates;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A grid which can have the density of its grid cells tracked.
 * This is a requirement for RoI mining.
 * @see AbstractRoIMining
 * @author Luke Bermingham
 */
public class RoIGrid extends Grid implements RoIMiningSpace {

    private final Map<Integer, RoIGridCell> gridCells;

    /**
     * Creates a new roi grid using a specified n-d grid
     * This constructor is typically called by {@link MiningSpaceFactory}.
     *
     * @param grid the n-d grid
     * @see MiningSpaceFactory
     */
    RoIGrid(Grid grid) {
        super(grid.nCellsPerDimension, grid.getBounds());
        this.gridCells = new HashMap<>();
    }

    private int getIdx(int[] indices) {
        return NDUtil.flattenIndices(indices, this.nCellsPerDimension);
    }

    /**
     * Increment the density of the cell at the position specified by the indices.
     * If no cell exists, makes one and sets the density to one.
     *
     * @param entityId the entity that was in this cell
     * @param index    the index of the cell in question
     */
    public void incrementCellDensity(String entityId, int index) {
        RoIGridCell cell = gridCells.get(index);
        if (cell == null) {
            //make grid cell
            RoIGridCell roIGridCell = new RoIGridCell(index, NDUtil.inflateIndex(index, this.nCellsPerDimension));
            roIGridCell.incrementTally(entityId);
            putCell(roIGridCell);
        } else {
            //already exists just increment
            cell.incrementTally(entityId);
        }
    }

    /**
     * Add a grid cell, overrides any existing entry for this cell
     *
     * @param roIGridCell the grid cell to add
     */
    private void putCell(RoIGridCell roIGridCell) {
        this.gridCells.put(roIGridCell.getIndex(), roIGridCell);
    }

    public RoIGridCell moveToCell(int[] startIndices, int[] offset) {
        int[] moved = new int[startIndices.length];
        for (int i = 0; i < startIndices.length; i++) {
            moved[i] = startIndices[i] + offset[i];
            if (moved[i] > nCellsPerDimension[i] - 1 || moved[i] < 0) {
                //out of bounds
                return null;
            }
        }
        return getCell(moved);
    }

    @Override
    public RoIGridCell getCell(int[] indices) {
        return gridCells.get(getIdx(indices));
    }

    @Override
    public void unprocessAll() {
        for (RoIGridCell cell : gridCells.values()) {
            cell.markUnprocessed();
        }
    }

    @Override
    public Collection<? extends MiningCell> getDenseCells() {
        //get the dense cells (but only unprocessed ones)
        List<? extends MiningCell> unprocessedCells = gridCells.values().stream()
                .filter(miningCell -> !miningCell.isProcessed()).sorted((o1, o2) -> o2.getDensity() - o1.getDensity()).collect(Collectors.toList());
        return unprocessedCells;
    }

    /**
     * Given a collection of 1d indices inflate them all into n-d indices
     *
     * @param indices the 1d indices
     * @return the n-d indices
     */
    public int[][] getNdIndices(Collection<Integer> indices) {
        int[][] ndIndices = new int[indices.size()][nDimensions()];
        int i = 0;
        for (Integer index1d : indices) {
            ndIndices[i] = NDUtil.inflateIndex(index1d, nCellsPerDimension);
            i++;
        }
        return ndIndices;
    }

    @Override
    public Collection<MiningCell> getNeighbourCells(MiningCell queryCell) {
        RoIGridCell start = (RoIGridCell) queryCell;
        int nDimensions = nDimensions();
        Collection<MiningCell> neighbourCells = new ArrayList<>(nDimensions * 2);
        //check up and down in each dimension
        for (int n = 0; n < nDimensions; n++) {
            int[] up = new int[nDimensions];
            up[n] = 1;
            int[] down = new int[nDimensions];
            down[n] = -1;
            RoIGridCell movedUp = moveToCell(start.getIndices(), up);
            RoIGridCell movedDown = moveToCell(start.getIndices(), down);
            if (movedUp != null) {
                neighbourCells.add(movedUp);
            }
            if (movedDown != null) {
                neighbourCells.add(movedDown);
            }
        }
        return neighbourCells;
    }

    @Override
    public RoI removeSparsity(RoI roi) {

        //remove sparsity by getting the index bounds of the RoI
        //then filling in the all cells in between
        int[][] cellIndices = getNdIndices(roi.getCells());
        int[][] indexBounds = BoundsUtil.calculateBounds(cellIndices);
        int nDimensions = indexBounds.length;
        //get extreme indices based on bounds
        int[] min = new int[nDimensions];
        int[] max = new int[nDimensions];
        int[] shape = new int[nDimensions];
        for (int n = 0; n < nDimensions; n++) {
            min[n] = indexBounds[n][0];
            max[n] = indexBounds[n][1];
            shape[n] = max[n] - min[n] + 1;
        }

        int[] indices = fitIndicesIntoDimensions(shape, min, max);

        //make the new roi with all indices filled in
        RoI denseRoI = new RoI(roi.getId());
        for (int index1d : indices) {

            MiningCell cell = getCell(index1d);
            //if no cell available here, because this is method
            //must remove sparsity it may require a cell to be made
            if (cell == null) {
                cell = new RoIGridCell(index1d, super.toNdIdx(index1d));
                //have to add it in the grid (with zero density) for processing reasons
                this.putCell((RoIGridCell) cell);
            }
            denseRoI.add(cell);
        }
        return denseRoI;
    }


    /**
     * Get the centroid coordinate from an roi using its cells (assuming the roi is from this grid).
     * @param roi The roi the get the centroid coordinates of.
     * @return The centroid coordinates of the RoI.
     */
    public double[] getCentroid(RoI roi){

        int nRois = roi.size();
        double[][] allCentroids = new double[nRois][2];

        int i = 0;
        for (Integer cellId : roi.getCells()) {
            BoundingCoordinates cellBounds = this.getCellBounds(cellId);
            if(cellBounds != null){
                allCentroids[i] = BoundsUtil.getCenter(cellBounds.getBounds());
            }
            i++;
        }

        return Maths.medoid(allCentroids);
    }

    @Override
    public MiningCell getCell(int idx) {
        return gridCells.get(idx);
    }

    /**
     * Once the grid has been populated with dense cells we want to know the maximum possible cell density
     *
     * @return maximum possible cell density
     */
    public int getMaxPossibleDensity() {
        HashSet<String> possibleEntityIds = new HashSet<>();
        for (RoIGridCell cell : gridCells.values()) {
            possibleEntityIds.addAll(cell.getKeys());
        }
        return possibleEntityIds.size();
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder("RoI Grid(");
        for (int i = 0; i < nCellsPerDimension.length; i++) {
            str.append(String.valueOf(nCellsPerDimension[i]));
            if(i != nCellsPerDimension.length - 1){
                str.append("x");
            }
        }
        str.append(")");
        return str.toString();
    }

}
