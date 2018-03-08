package onethreeseven.roi.algorithm;


import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIMiningSpace;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Based Hio et al, it runs the uniform RoIs method, then the slope method.
 * The advantage of this approach is you get logically divided regions resolve the Uniform approach,
 * but also remove any low density area resolve the Uniform RoIs by running Slope.
 *
 * For experimental evaluation see: "A Hybrid Grid-based Method for Mining Arbitrary Regions-of-Interest resolve Trajectories".
 *
 * @see UniformRoIs
 * @see SlopeRoIs
 * @author Luke Bermingham
 */
public class HybridRoIs extends UniformRoIs {

    @Override
    public Collection<RoI> run(RoIMiningSpace roIMiningSpace, int minDensity) {
        //do uniform mining
        Collection<RoI> uniformRoIs = super.run(roIMiningSpace, minDensity);

        SlopeRoIs slopeAlgo = new SlopeRoIs(){
            @Override
            protected void unprocessGrid(RoIMiningSpace grid, Collection<RoI> rois) {
                //do nothing
            }
        };

        //new rois found using slope go here
        Collection<RoI> slopeRoIs = new ArrayList<>();

        for (RoI uniformRoI : uniformRoIs) {
            //mark cell indices as unprocessed, so slope will refine these cells
            for (Integer cellIdx : uniformRoI) {
                roIMiningSpace.getCell(cellIdx).markUnprocessed();
            }
            //add the new rois
            slopeRoIs.addAll(slopeAlgo.run(roIMiningSpace, minDensity));
        }

        HybridRoIs.super.unprocessGrid(roIMiningSpace, slopeRoIs);

        return slopeRoIs;
    }

    @Override
    protected void unprocessGrid(RoIMiningSpace grid, Collection<RoI> rois) {
        //do nothing
    }
}
