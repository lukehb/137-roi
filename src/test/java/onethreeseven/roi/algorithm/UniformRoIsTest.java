package onethreeseven.roi.algorithm;

import onethreeseven.datastructures.model.Trajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.roi.model.MiningSpaceFactory;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;
import org.junit.Assert;
import org.junit.Test;
import java.util.BitSet;
import java.util.Collection;
import java.util.Map;

/**
 * Testing the uniform RoIs algorithm.
 * @see UniformRoIs
 * @author Luke Bermingham
 */
public class UniformRoIsTest {

    @Test
    public void testFindRoIs() throws Exception {

        //make the trajectories in "L" shape
        Map<String, Trajectory> trajectories = DataGeneratorUtil.generateNTrajectoriesFrom(new double[][]{
                new double[]{0, 0, 0},
                new double[]{3, 0, 0},
                new double[]{6, 0, 0},
                new double[]{6, 3, 0},
        }, 10);

        RoIGrid roiGrid = MiningSpaceFactory.createGrid(trajectories, new int[]{7, 4, 1}, 0);
        Collection<RoI> rois = new UniformRoIs().run(roiGrid, 3);

        int startIdx = roiGrid.to1dIdx(new int[]{0, 0, 0});
        int endIdx = roiGrid.to1dIdx(new int[]{6, 3, 0});

        BitSet indicesToExpect = new BitSet();
        int len = (endIdx-startIdx)+1;
        for (int i = 0; i < len; i++) {
            indicesToExpect.set(startIdx + i);
        }

        for (RoI roi : rois) {
            for (Integer idx : roi) {
                Assert.assertTrue(indicesToExpect.get(idx));
            }
        }

    }
}