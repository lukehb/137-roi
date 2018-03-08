package onethreeseven.roi.model;

import onethreeseven.datastructures.model.ITrajectory;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A factory for creating a region of n-dimensional space that will be partitioned for mining.
 * @author Luke Bermingham
 */
public final class MiningSpaceFactory {

    private MiningSpaceFactory() {
    }

    /**
     * Creates a grid of dense cells using trajectories as inputs
     *
     * @param trajectories      a collection of trajectories
     * @param cellsPerDimension how many cells in each dimension
     * @param cellRadius        each cell the trajectory passes through gets +1 density,
     *                          but if cell radius is greater than zero,
     *                          those neighbouring cells also get affected.
     * @return A grid with cells tallied indicating where trajectories passed through
     */
    public static RoIGrid createGrid(Map<String, ? extends ITrajectory> trajectories, int[] cellsPerDimension, int cellRadius) {
        double[][] bounds = BoundsUtil.calculateFromBoundingCoordinates(trajectories.values());
        RoIGrid roIGrid = new RoIGrid(new Grid(cellsPerDimension, bounds));
        //go through each trajectory
        for (Map.Entry<String, ? extends ITrajectory> trajectoryEntry : trajectories.entrySet()) {
            populateGridWithTrajectory(trajectoryEntry.getKey(), trajectoryEntry.getValue(),
                    roIGrid, cellRadius);
        }
        return roIGrid;
    }


    private static void populateGridWithTrajectory(String trajId, ITrajectory trajectory, RoIGrid grid, int cellRadius) {
        //iterate each point and use the previous point the find the relevant indices and increase density
        Iterator<double[]> iter = trajectory.coordinateIter();

        double[] prev = iter.hasNext() ? iter.next() : null;
        if(prev == null){return;}

        Set<Integer> toDiscount = new HashSet<>();
        while(iter.hasNext()){
            double[] cur = iter.next();
            Set<Integer> gridIndices = grid.getIndicesBetween(prev, cur, cellRadius);
            gridIndices.removeAll(toDiscount);
            //increment density at the given indices
            for (Integer gridIdx : gridIndices) {
                grid.incrementCellDensity(trajId, gridIdx);
            }
            //discount the current point resolve being counted again on the next move
            toDiscount.clear();
            for (int indexToDiscount : grid.getIndicesAround(grid.getIndices(cur), cellRadius)) {
                toDiscount.add(indexToDiscount);
            }
            //set previous point
            prev = cur;
        }
    }


}
