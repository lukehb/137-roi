package onethreeseven.roi.algorithm;


import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * The expansive algorithm keeps expanding so long as the neighbours are connected and above the threshold.
 * @author Luke Bermingham
 */
public class ExpansiveRoIs extends DisjointRoIs {
    private final Map<Integer, MiningCell> neighbourCandidates = new HashMap<>();

    @Override
    protected MiningCell pickNextCell(Collection<MiningCell> neighbourCells,
                                      MiningCell currentCell,
                                      int minDensity)  {
        //find the most dense neighbour that is above the min density
        MiningCell mostDense = null;
        for (MiningCell neighbourCell : neighbourCells) {
            int neighbourDensity = neighbourCell.getDensity();
            if (!neighbourCell.isProcessed() && neighbourDensity >= minDensity) {

                if (mostDense == null || neighbourCell.getDensity() > mostDense.getDensity()) {
                    //make the current neighbour the most dense
                    mostDense = neighbourCell;
                } else {
                    //add it as a candidate
                    neighbourCandidates.put(neighbourCell.getIndex(), neighbourCell);
                }

            }
        }
        //there was no good neighbour so use candidates
        if (mostDense == null) {
            if (!neighbourCandidates.isEmpty()) {
                //remove first candidate
                return neighbourCandidates.remove(neighbourCandidates.keySet().iterator().next());
            }
            mostDense = currentCell;
        }
        return mostDense;
    }

    @Override
    protected MiningCell handleNeighbourCell(RoIMiningSpace roIMiningSpace,
                                             RoI currentRoI,
                                             MiningCell neighbourCell,
                                             MiningCell currentCell,
                                             int minDensity) {
        MiningCell miningCell = super.handleNeighbourCell(roIMiningSpace, currentRoI, neighbourCell, currentCell, minDensity);
        //if we have processed it into an RoI, it can't be a neighbour
        neighbourCandidates.remove(miningCell.getIndex());
        return miningCell;
    }

    @Override
    public String toString(){
        return "Expansive";
    }

}
