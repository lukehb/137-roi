package onethreeseven.roi.algorithm;

import onethreeseven.roi.model.MiningCell;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An roi detect algorithm that forms one single RoI using any
 * cells that are above the density threshold. They do not even have to be connected!
 * This is mainly used for testing, its real world use is limited.
 * @author Luke Bermingham
 */
public class ThresholdRoIs extends AbstractRoIMining {


    @Override
    protected MiningCell handleNeighbourCell(RoIMiningSpace roIMiningSpace,
                                             RoI currentRoI,
                                             MiningCell neighbourCell,
                                             MiningCell currentCell,
                                             int minDensity) {
        throw new UnsupportedOperationException("Not used in threshold roi mining");
    }

    @Override
    protected MiningCell pickNextCell(Collection<MiningCell> neighbourCells,
                                      MiningCell currentCell,
                                      int minDensity) {
        throw new UnsupportedOperationException("Not used in threshold roi mining");
    }

    @Override
    public Collection<RoI> run(RoIMiningSpace roIMiningSpace, int minDensity) {

        RoI roi = null;
        for (MiningCell denseCell : roIMiningSpace.getDenseCells()) {
            if (denseCell.getDensity() >= minDensity) {
                if (roi == null) {
                    roi = new RoI(1);
                }
                roi.add(denseCell);
                denseCell.markProcessed();
            }
        }

        Collection<RoI> rois = new ArrayList<>();
        if(roi != null){
            rois.add(roi);
        }
        return rois;
    }

    @Override
    public String toString(){
        return "Threshold";
    }

}
