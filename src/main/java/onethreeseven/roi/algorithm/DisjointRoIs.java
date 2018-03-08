package onethreeseven.roi.algorithm;

import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;

/**
 * Just keep expanding RoIs if the cells are above threshold.
 * Not slope criteria or rectangular criteria.
 * @author Luke Bermingham
 */
public class DisjointRoIs extends UniformRoIs {


    @Override
    protected MiningCell handleNeighbourCell(RoIMiningSpace miningSpace,
                                             RoI currentRoI,
                                             MiningCell neighbourCell,
                                             MiningCell currentCell,
                                             int minDensity) {
        if (neighbourCell.isProcessed()) {
            //stop expanding
            return currentCell;
        } else {
            //add neighbour cell to the current roi
            currentRoI.add(neighbourCell);
            neighbourCell.markProcessed();
            return neighbourCell;
        }
    }

}
