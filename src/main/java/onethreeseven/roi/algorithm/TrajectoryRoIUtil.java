package onethreeseven.roi.algorithm;

import onethreeseven.collections.IntArray;
import onethreeseven.datastructures.model.ITrajectory;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;

import java.util.Collection;
import java.util.Iterator;

/**
 * Convert a single trajectory into a series of RoI visitations.
 * So the points {p1,p2,p3,p4,p5}
 * and the rois {r1,r2,r3}
 * may transform into a visitations sequence like:
 * {r1,r1,r1,r3,r2}
 * @author Luke Bermingham
 */
public final class TrajectoryRoIUtil {

    private TrajectoryRoIUtil(){}

    /**
     * Converts a trajectory to a series of RoI id visitations.
     * @param trajectory trajectory
     * @param rois rois
     * @param roIGrid the roi grid
     * @return An int[] of RoI ids that were visited by this trajectory.
     */
    public static int[] fromTrajToRoISequence(ITrajectory trajectory, Collection<RoI> rois, RoIGrid roIGrid) {
        //make output map for each roi

        IntArray visitedRoIs = new IntArray(trajectory.size(), false);

        int prevId = Integer.MIN_VALUE;

        //set the status of point to the RoI id (or not)
        Iterator<double[]> iter = trajectory.coordinateIter();
        while(iter.hasNext()){
            double[] trajCoord = iter.next();
            int cellIdx = roIGrid.getIndex(trajCoord);
            for (RoI roi : rois) {
                //set the output to the id of the RoI
                //this roi contained the point or not
                if (roi != null && roi.contains(cellIdx)) {
                    int roiId = roi.getId();
                    if(roiId != prevId){
                        visitedRoIs.add(roiId);
                        prevId = roiId;
                    }
                    break;
                }
            }
        }
        return visitedRoIs.getArray();
    }

}
