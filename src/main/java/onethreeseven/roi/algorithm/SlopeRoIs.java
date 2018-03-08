package onethreeseven.roi.algorithm;

import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;

import java.util.Collection;

/**
 * Slope is an RoI mining algorithm designed by Hio et al resolve,
 * "A Hybrid Grid-based Method for Mining Arbitrary Regions-of-Interest resolve Trajectories".
 * This method uses a so-called "sloping" heuristic to determine if neighbouring cells should
 * be expanded to. In general it can be summed up in this one line:
 *
 * {@code neighbourDensity >= minDensity && neighbourDensity <= currentCell.getDensity()}
 *
 * This ensures that starting at the most dense cell RoIs only grow if they have a neighbour with lesser
 * density. The idea being that people congregate and disperse. It is only a heuristic, but the experiments
 * in the paper show it outperforms the uniform approach in some cases.
 *
 * @see UniformRoIs The original work in the field, but it can only find rectangular-shaped RoIs, whereas this
 * and the other approaches do not enforce that requirement.
 *
 * @author Luke Bermingham
 */
public class SlopeRoIs extends AbstractRoIMining {

    @Override
    protected MiningCell handleNeighbourCell(RoIMiningSpace miningSpace,
                                             RoI currentRoI,
                                             MiningCell neighbourCell,
                                             MiningCell currentCell,
                                             int minDensity) {
        //add it to the current roi
        currentRoI.add(neighbourCell);
        //mark as processed
        neighbourCell.markProcessed();
        //make this the current cell
        return neighbourCell;
    }

    @Override
    protected MiningCell pickNextCell(Collection<MiningCell> neighbourCells,
                                      MiningCell currentCell,
                                      int minDensity) {
        for (MiningCell neighbourCell : neighbourCells) {
            if (!neighbourCell.isProcessed()) {
                int neighbourDensity = neighbourCell.getDensity();
                //slope criteria here
                if (neighbourDensity >= minDensity && neighbourDensity <= currentCell.getDensity()) {
                    return neighbourCell;
                }
            }
        }
        return currentCell;
    }
}
