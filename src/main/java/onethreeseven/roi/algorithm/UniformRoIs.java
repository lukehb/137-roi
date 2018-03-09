package onethreeseven.roi.algorithm;


import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;

import java.util.Collection;

/**
 * Heuristic RoI algorithm from "Trajectory Pattern Mining" by Giannotti et al.
 * It has a criteria that all RoIs must be rectangular-shaped.
 * This means it can include cells with low density, so long as the average density is above the threshold.
 * We have extended this algorithm to support mining RoIs in n-dimensions.
 * In a 2d grid uniform RoIs are rectangular, in a 3d grid they are rectangular prisms, and so on for higher dimensions.
 * @author Luke Bermingham
 */
public class UniformRoIs extends AbstractRoIMining {

    @Override
    protected MiningCell handleNeighbourCell(RoIMiningSpace roIMiningSpace,
                                             RoI currentRoI,
                                             MiningCell neighbourCell,
                                             MiningCell currentCell,
                                             int minDensity) {
        //need to factor the rectangular average density
        int newId = currentRoI.getId() + 1;
        //make a potential roi because we don't want to affect the real one if this one is impossible
        RoI potentialRoI = new RoI(neighbourCell, newId);
        potentialRoI.add(currentRoI);

        //fill in any missing cells
        potentialRoI = roIMiningSpace.removeSparsity(potentialRoI);
        if (potentialRoI == null) {
            return currentCell;
        }

        //now all cells are filled in assess the density of the potential region
        double avgDensity = potentialRoI.getDensity() / potentialRoI.size();
        if (avgDensity >= minDensity) {
            //need to copy contents to current roi here
            currentRoI.clear();
            currentRoI.add(potentialRoI);
            //mark all the cells in the new region processed
            currentRoI.forEach(idx -> roIMiningSpace.getCell(idx).markProcessed());
            //neighbour cell was a successful candidate for expansion, make it the new seed
            return neighbourCell;
        } else {
            //stop expanding
            return currentCell;
        }
    }

    @Override
    protected MiningCell pickNextCell(Collection<MiningCell> neighbourCells,
                                      MiningCell currentCell,
                                      int minDensity) {
        //find the most dense neighbour that is above the min density
        MiningCell mostDense = null;
        for (MiningCell neighbourCell : neighbourCells) {
            int neighbourDensity = neighbourCell.getDensity();
            if (!neighbourCell.isProcessed() && neighbourDensity >= minDensity &&
                    (mostDense == null || neighbourCell.getDensity() > mostDense.getDensity())) {
                //make the current neighbour the most dense
                mostDense = neighbourCell;
            }
        }
        //there was no good neighbour so return current cell
        if (mostDense == null) {
            mostDense = currentCell;
        }
        return mostDense;
    }

    @Override
    public String toString(){
        return "Uniform";
    }

}
