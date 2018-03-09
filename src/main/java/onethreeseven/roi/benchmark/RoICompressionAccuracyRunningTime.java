package onethreeseven.roi.benchmark;

import onethreeseven.common.util.FileUtil;
import onethreeseven.common.util.Maths;
import onethreeseven.datastructures.data.STTrajectoryParser;
import onethreeseven.datastructures.data.resolver.*;
import onethreeseven.datastructures.model.STTrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.datastructures.util.DataGeneratorUtil;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.geo.projection.ProjectionEquirectangular;
import onethreeseven.roi.algorithm.AbstractRoIMining;
import onethreeseven.roi.algorithm.TrajectoryRoIUtil;
import onethreeseven.roi.algorithm.UniformRoIs;
import onethreeseven.roi.model.MiningSpaceFactory;
import onethreeseven.roi.model.RoI;
import onethreeseven.roi.model.RoIGrid;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Measures compression, accuracy, and running time of an RoI method.
 * @author Luke Bermingham
 */
public class RoICompressionAccuracyRunningTime {

    private final static AbstractGeographicProjection projection = new ProjectionEquirectangular();

    private static final boolean generateTrajs = true;
    private static final int nEntriesPerTraj = 300000;
    private static final int nTrajs = 100;

    private final static File inputFile = new File(FileUtil.makeAppDir("traj"), "geolife_179.txt");

    //contain a single coordinate for each stop
    private final static File truthFile = new File(FileUtil.makeAppDir("traj"), "synthetic_compressed_2000000.txt");

    private final static STTrajectoryParser parser = new STTrajectoryParser(
                projection,
                new IdFieldResolver(0),
                new LatFieldResolver(1),
                new LonFieldResolver(2),
                new TemporalFieldResolver(6,7),
                true);

//    private final static STTrajectoryParser parser = new STTrajectoryParser(
//            projection,
//            new IdFieldResolver(0),
//            new NumericFieldsResolver(1,2),
//            new TemporalFieldResolver(3),
//            true);

    private static final double gridCellSizeMetres = 200;
    private static final int minDensity = 5;

    private static final boolean doAccuracyMeasurement = false;

    private static final boolean doTrajsIncrementally = false;

    private static final AbstractRoIMining[] algos = new AbstractRoIMining[]{
            new UniformRoIs(),
            //new HybridRoIs(),
            //new SlopeRoIs(),
            //new ExpansiveRoIs()
    };

    public static void main(String[] args) throws IOException {

        StringBuilder output = new StringBuilder();



        for (AbstractRoIMining algo : algos) {

            output.append(algo.getClass().getSimpleName()).append("_NEntries,");

            if(doAccuracyMeasurement){
                output.append(algo.getClass().getSimpleName()).append("_accuracy,");
            }

            output.append(algo.getClass().getSimpleName()).append("_compression,");
            output.append(algo.getClass().getSimpleName()).append("_running,");
        }
        output.append("\n");



        Map<String, SpatioCompositeTrajectory> rawTrajs = new HashMap<>();

        if(generateTrajs){
            System.out.println("Generating trajectories.");
            for (int i = 0; i < nTrajs; i++) {
                rawTrajs.put(String.valueOf(i), DataGeneratorUtil.generateTrajectoryWithStops(
                        nEntriesPerTraj, 100, 1000, 100000,3, 3, 45,45));
            }
        }
        else{
            System.out.println("Reading trajectories");
            for (Map.Entry<String, STTrajectory> entry : parser.parse(inputFile).entrySet()) {
                rawTrajs.put(entry.getKey(), entry.getValue());
            }
        }



        if(doTrajsIncrementally){

            Map<String, SpatioCompositeTrajectory> toProcess = new HashMap<>();
            Iterator<Map.Entry<String, SpatioCompositeTrajectory>> iter = rawTrajs.entrySet().iterator();
            int i = 0;
            while(iter.hasNext()){
                Map.Entry<String, SpatioCompositeTrajectory> entry = iter.next();
                toProcess.put(entry.getKey(), entry.getValue());
                i++;
                if(i % 10 == 0){
                    doExperiment(toProcess, output);
                }
            }

        }
        else{
            doExperiment(rawTrajs, output);
        }

        System.out.println(output.toString());

    }

    private static void doExperiment(Map<String, SpatioCompositeTrajectory> rawTrajs, StringBuilder output) throws IOException {

        long gridSetupTime = 0;

//        int toRemove = 160;
//        Iterator<Map.Entry<String, SpatioCompositeTrajectory>> iter = rawTrajs.entrySet().iterator();
//        int nRemoved = 0;
//        while(iter.hasNext() && nRemoved < toRemove){
//            iter.next();
//            iter.remove();
//            nRemoved++;
//        }

        System.out.println("Making grid.");

        RoIGrid grid;
        {
            // [[min,max],[min,max]]
            double[][] bounds = BoundsUtil.calculateFromBoundingCoordinates(rawTrajs.values());
            double[] bl = new double[]{bounds[0][0], bounds[1][0]};
            double[] tr = new double[]{bounds[0][1], bounds[1][1]};
            double diagonalDist = Maths.dist(bl, tr);
            int nGridCells = (int) Math.round(diagonalDist/gridCellSizeMetres);
            long startTime = System.currentTimeMillis();
            grid = MiningSpaceFactory.createGrid(rawTrajs, new int[]{nGridCells, nGridCells}, 1);
            long endTime = System.currentTimeMillis();
            gridSetupTime = endTime - startTime;
        }

        Map<String, STTrajectory> truthTrajs;

        if(doAccuracyMeasurement){
            truthTrajs = parser.parse(truthFile);
        }

        for (AbstractRoIMining roiMiningAlgo : algos) {
            System.out.println("Running RoI mining: " + roiMiningAlgo.getClass().getSimpleName() + "(" + rawTrajs.size() + ")");

            ArrayList<Double> accuracies = new ArrayList<>();

            long startTime = System.currentTimeMillis();
            Collection<RoI> rois = roiMiningAlgo.run(grid, minDensity);
            long endTime = System.currentTimeMillis();
            long roiTime = (endTime - startTime);
            long totalTime = gridSetupTime + roiTime;

            ArrayList<Double> compressions = new ArrayList<>();

            int nEntries = 0;

            for (Map.Entry<String, SpatioCompositeTrajectory> rawEntry : rawTrajs.entrySet()) {

                int[] rawRoISequence = TrajectoryRoIUtil.fromTrajToRoISequence(rawEntry.getValue(), rois, grid);

                if (doAccuracyMeasurement && truthTrajs != null) {
                    STTrajectory truthTraj = truthTrajs.get(rawEntry.getKey());
                    if (rawEntry.getValue().size() == 0 || truthTraj.size() == 0) {
                        continue;
                    }

                    int[] truthRoISequence = TrajectoryRoIUtil.fromTrajToRoISequence(truthTraj, rois, grid);
                    double accuracy = computeAccuracy(rawRoISequence, truthRoISequence);
                    accuracies.add(accuracy);
                }

                compressions.add((rawRoISequence.length / (double) rawEntry.getValue().size()));

                nEntries += rawEntry.getValue().size();
            }

            output.append(nEntries).append(",");

            if (doAccuracyMeasurement) {
                accuracies.stream().mapToDouble(value -> value).average().ifPresent(avgAccuracy -> {
                    output.append(avgAccuracy);
                    output.append(",");
                });
            }

            compressions.stream().mapToDouble(value -> value).average().ifPresent(avgCompression -> {
                output.append(avgCompression);
                output.append(",");
            });

            output.append(totalTime);
            output.append(",");

        }
        output.append("\n");

    }

    private static double computeAccuracy(int[] rawRoISequence,
                                          int[] truthRoISequence){
        //translate trajectory to RoI visitations
        double tp = 0;

        if(rawRoISequence.length == 0 || truthRoISequence.length == 0){
            return 0;
        }

        //truth sequence will be shorter or equal
        int i = 0;
        for (int truthRoI : truthRoISequence) {
            for (; i < rawRoISequence.length; i++) {
                int rawRoI = rawRoISequence[i];
                if(rawRoI == truthRoI){
                    tp++;
                    i++;
                    break;
                }
            }
        }

        //precision
        return tp/rawRoISequence.length;
    }

}
