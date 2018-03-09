package onethreeseven.roi.algorithm;

import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * The general mining algorithm that is the base class for all region of interest (RoI) mining algorithms.
 * RoI mining algorithms in general follow this schema in TrajSuite:
 * @see RoIMiningSpace 1) Use a study region and partition it into cells.
 * 2) Allocate a density score to each cell by counting how many trajectories passed through it.
 * 3) Starting at the most dense cell (the cell that had the most trajectories pass through it) start expanding to nearby cells
 * that meet a user-specified density requirement (or some algorithmic heuristic).
 * 4) Once expansion can not longer continue the group of cells you have is called an RoI.
 * 5) Continue the process at the next densest cell that has not been processed yet.
 * @author Luke Bermingham
 */
public abstract class AbstractRoIMining {

    protected Consumer<Double> progressReporter = null;

    public void setProgressReporter(Consumer<Double> progressReporter) {
        this.progressReporter = progressReporter;
    }

    /**
     * Judges the neighbour cell against the current cell we are on, and determines whether the neighbour
     * cell should be added the the RoI we are expanding.
     * @param miningSpace the grid used for RoI mining
     * @param currentRoI    the RoI we are making
     * @param neighbourCell the neighbour cell to potentially add to the roi
     * @param currentCell   the current cell that is already in the RoI
     * @param minDensity the minimum number of time a cell must be visited to be an RoI
     * @return the new current cell (may still be same current cell)
     */
    protected abstract MiningCell handleNeighbourCell(RoIMiningSpace miningSpace,
                                                      RoI currentRoI,
                                                      MiningCell neighbourCell,
                                                      MiningCell currentCell,
                                                      int minDensity);

    protected abstract MiningCell pickNextCell(Collection<MiningCell> neighbourCells,
                                               MiningCell currentCell,
                                               int minDensity);

    public Collection<RoI> run(RoIMiningSpace roIMiningSpace, int minDensity) {

        //params/init
        Collection<? extends MiningCell> denseCells = roIMiningSpace.getDenseCells();
        Collection<RoI> rois = new ArrayList<>();
        int seedId = 0;

        //RoI mining
        int i = 0;
        for (MiningCell seedCell : denseCells) {
            int seedCellTally = seedCell.getDensity();
            //this cell has the required density and is not processed
            if (seedCellTally >= minDensity && !seedCell.isProcessed()) {
                RoI currentRoI = new RoI(seedCell, seedId);
                //increment id for next roi
                seedId++;
                //we use it in the roi, so mark it as processed
                seedCell.markProcessed();
                //expand the current roi, using the current seeding cell
                expandRoI(roIMiningSpace, currentRoI, seedCell, minDensity);
                //outside the expansion while loop add the current roi
                //if it is just one cell we do not call this a region
                if (currentRoI.size() > 1) {
                    rois.add(currentRoI);
                }
            } else {
                //mark as processed don't consider it again
                seedCell.markProcessed();
            }

            i++;
            if(progressReporter != null){
                double progress = (double)i / denseCells.size();
                progressReporter.accept(progress);
            }
        }

        //mark all cells unprocessed, so we can reuse the grid
        unprocessGrid(roIMiningSpace, rois);

        return rois;
    }

    protected void unprocessGrid(RoIMiningSpace grid, Collection<RoI> rois){
        grid.unprocessAll();
    }

    private void expandRoI(RoIMiningSpace roIMiningSpace, RoI currentRoI, MiningCell seedCell, int minDensity) {
        boolean keepExpanding = true;
        while (keepExpanding) {
            //get the neighbour cells for expansion
            Collection<MiningCell> neighbourCells = roIMiningSpace.getNeighbourCells(seedCell);
            //pick the neighbour based on algorithm we are using
            MiningCell picked = pickNextCell(neighbourCells, seedCell, minDensity);
            if (!picked.equals(seedCell)) {
                //handle the neighbour cell using the algorithm
                MiningCell nextCell = handleNeighbourCell(roIMiningSpace, currentRoI, picked, seedCell, minDensity);
                if (nextCell.equals(seedCell)) {
                    keepExpanding = false;
                } else {
                    //next cell was good, so make it the new seed
                    seedCell = nextCell;
                }
            } else {
                keepExpanding = false;
            }
        }
    }


}
