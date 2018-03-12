package onethreeseven.roi.view.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.model.ITrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.roi.algorithm.*;
import onethreeseven.roi.graphics.RoIGraphic;
import onethreeseven.roi.model.*;
import onethreeseven.spm.algorithm.*;
import onethreeseven.spm.data.SPMFParser;
import onethreeseven.spm.model.SequentialPattern;
import onethreeseven.trajsuitePlugin.graphics.LabelPrefab;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.EntitySupplier;
import onethreeseven.trajsuitePlugin.model.TransactionProcessor;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;
import onethreeseven.trajsuitePlugin.transaction.RemoveEntitiesTransaction;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;
import onethreeseven.trajsuitePlugin.util.IdGenerator;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for RoI view fxml
 * @author Luke Bermingham
 */
public class RoIViewController {

    //making grid pane
    @FXML
    public Label nSelectedTrajsLabel;
    @FXML
    public Button makeGridBtn;
    @FXML
    public Spinner<Integer> cellSizeSpinner;

    //roi pane
    @FXML
    public TitledPane roisPane;
    @FXML
    public CheckBox mineSPMCheckbox;
    @FXML
    public ChoiceBox<WrappedEntity<RoIMiningSpace>> miningSpaceChoiceBox;
    @FXML
    public ChoiceBox<AbstractRoIMining> roiAlgorithmChoiceBox;
    @FXML
    public Spinner<Integer> minimumDensitySpinner;

    //spm pane
    @FXML
    public TitledPane spmPane;
    @FXML
    public Spinner<Integer> minsupSpinner;
    @FXML
    public Label spmParamLabel;
    @FXML
    public Spinner<Integer> spmParamSpinner;
    @FXML
    public ChoiceBox<SPMAlgorithm> spmAlgoChoiceBox;

    //feedback label
    @FXML
    public Label feedbackLabel;

    //Bottom bar
    @FXML
    public ProgressBar progressBar;
    @FXML
    public Button mineRoIsBtn;

    private final Map<String, ITrajectory> selectedTrajs;



    public RoIViewController(){

        this.selectedTrajs = new HashMap<>();

        //add listener for add entity changes
        BaseTrajSuiteProgram.getInstance().getLayers().addEntitiesTransactionProperty.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(()->{
                if(isShowing()){
                    setupMiningSpaceChoiceBox();
                }
            });
        });

        //add listener for remove entity changes
        BaseTrajSuiteProgram.getInstance().getLayers().removeEntitiesTransactionProperty.addListener(new ChangeListener<RemoveEntitiesTransaction>() {
            @Override
            public void changed(ObservableValue<? extends RemoveEntitiesTransaction> observable, RemoveEntitiesTransaction oldValue, RemoveEntitiesTransaction newValue) {
                Platform.runLater(()->{
                    if(isShowing()){
                        setupMiningSpaceChoiceBox();
                    }
                });
            }
        });

        //listen for selection changes
        BaseTrajSuiteProgram.getInstance().getLayers().numEditedEntitiesProperty.addListener((observable, oldValue, newValue) -> {
            Platform.runLater(this::refreshSelectedTrajs);
        });

    }

    protected void refreshSelectedTrajs(){
        final Map<String, ITrajectory> newSelectedTrajs = new HashMap<>();

        //get all selected trajectories
        ServiceLoader<EntitySupplier> serviceLoader = ServiceLoader.load(EntitySupplier.class);
        for (EntitySupplier entitySupplier : serviceLoader) {
            Map<String, WrappedEntity> allSelected = entitySupplier.supplyAllMatching(wrappedEntity -> wrappedEntity.isSelectedProperty().get() && ITrajectory.class.isAssignableFrom(wrappedEntity.getModel().getClass()));
            for (Map.Entry<String, WrappedEntity> entry : allSelected.entrySet()) {
                newSelectedTrajs.put(entry.getKey(), (ITrajectory) entry.getValue().getModel());
            }
        }

        RoIViewController.this.selectedTrajs.clear();
        RoIViewController.this.selectedTrajs.putAll(newSelectedTrajs);
        onSelectedTrajectoriesChanged(newSelectedTrajs);

    }

    protected boolean isShowing() {
        if (mineRoIsBtn == null) {
            return false;
        }
        Scene scene = mineRoIsBtn.getScene();
        if (scene == null) {
            return false;
        }
        Stage stage = (Stage) scene.getWindow();
        return stage != null && stage.isShowing();
    }

    private void setupMiningSpaceChoiceBox(){

        Map<String, WrappedEntity> entities = BaseTrajSuiteProgram.getInstance().getLayers().getAllMatching(
                wrappedEntity -> RoIMiningSpace.class.isAssignableFrom(wrappedEntity.getModel().getClass()));

        Map<String, WrappedEntity<RoIMiningSpace>> spaces = new HashMap<>();
        for (Map.Entry<String, WrappedEntity> entry : entities.entrySet()) {
            spaces.put(entry.getKey(), entry.getValue());
        }

        miningSpaceChoiceBox.getItems().clear();
        miningSpaceChoiceBox.getItems().addAll(spaces.values());

        miningSpaceChoiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(WrappedEntity<RoIMiningSpace> object) {
                return object.getId();
            }

            @Override
            public WrappedEntity<RoIMiningSpace> fromString(String string) {
                return spaces.get(string);
            }
        });

        boolean hasGrid = !miningSpaceChoiceBox.getItems().isEmpty();

        //select first
        if(hasGrid){
            miningSpaceChoiceBox.getSelectionModel().selectFirst();
        }

        boolean hasTrajs = !selectedTrajs.isEmpty();

        setupMiningState(hasTrajs, hasGrid);

    }

    private void setupMiningState(boolean hasTrajs, boolean hasGrid){

        progressBar.setProgress(0);
        progressBar.setDisable(true);

        //there is no grid
        if(!hasGrid){
            feedbackLabel.setText("Please make a grid for RoI mining.");
            miningSpaceChoiceBox.setDisable(true);
            minimumDensitySpinner.setDisable(true);
            roiAlgorithmChoiceBox.setDisable(true);
            mineRoIsBtn.setDisable(true);
        }

        if(hasTrajs){
            cellSizeSpinner.setDisable(false);
            makeGridBtn.setDisable(false);
        }
        else{
            feedbackLabel.setText("Please select some trajectories to mine RoIs from.");
        }

        if(hasTrajs && hasGrid){
            //activate roi pane
            roisPane.setDisable(false);
            miningSpaceChoiceBox.setDisable(false);
            feedbackLabel.setText("");
            roiAlgorithmChoiceBox.setDisable(false);
            mineRoIsBtn.setDisable(false);
            //setup min density spinner
            int numSelectedTrajs = selectedTrajs.size();
            minimumDensitySpinner.setDisable(false);
            minimumDensitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, numSelectedTrajs, 1));
            minimumDensitySpinner.getValueFactory().setValue((int) (numSelectedTrajs * 0.5));
        }
        else{
            roisPane.setDisable(true);
            mineSPMCheckbox.setSelected(false);
        }


    }

    private void onSelectedTrajectoriesChanged(Map<String, ITrajectory> selectedTrajs){
        int numSelectedTrajs = selectedTrajs.size();
        boolean trajsSelected = numSelectedTrajs > 0;
        nSelectedTrajsLabel.setText(String.valueOf(numSelectedTrajs));
        setupMiningState(trajsSelected, !miningSpaceChoiceBox.getItems().isEmpty());
    }

    @FXML
    public void initialize(){
        cellSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 100, 1));

        minimumDensitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10, 1));

        ObservableList<AbstractRoIMining> roiAlgs = FXCollections.observableArrayList(
                new DisjointRoIs(),
                new ExpansiveRoIs(),
                new HybridRoIs(),
                new SlopeRoIs(),
                new ThresholdRoIs(),
                new UniformRoIs());

        roiAlgorithmChoiceBox.getItems().addAll(roiAlgs);
        roiAlgorithmChoiceBox.getSelectionModel().selectFirst();

        //bind the disabled property to the checkbox selection
        spmPane.disableProperty().bind(mineSPMCheckbox.selectedProperty().not());

        //spm algo choice box
        spmAlgoChoiceBox.getItems().addAll(
                new ACSpan(),
                new CCSpan(),
                new MCSpan(),
                new DCSpan()
        );

        //setup min sup
        minsupSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100000, 100));

        //setup spm param spinner
        spmAlgoChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue instanceof DCSpan){
                spmParamLabel.setText("Maximum redundancy (%):");
                spmParamSpinner.setDisable(false);
                spmParamSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 50));
                minsupSpinner.setDisable(false);
            }
            else{
                spmParamLabel.setText("Other parameter:");
                spmParamSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,100, 1));
                spmParamSpinner.setDisable(true);
                minsupSpinner.setDisable(false);
            }
        });

        spmAlgoChoiceBox.getSelectionModel().selectFirst();

        refreshSelectedTrajs();
        setupMiningSpaceChoiceBox();


    }

    @FXML
    public void onMakeGridClicked(ActionEvent actionEvent) {

        //make the grid
        if(!selectedTrajs.isEmpty()){

            feedbackLabel.setText("Making grid...");

            ThreadFactory factory = r -> new Thread(r, "Making Grid");
            final ExecutorService service = Executors.newSingleThreadExecutor(factory);

            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressBar.setDisable(false);

            //make the grid
            CompletableFuture.runAsync(this::makeGridImpl, service).handle((aVoid, throwable) -> {
                service.shutdown();
                Platform.runLater(()->{
                    feedbackLabel.setText("Grid made, can now used it for RoI mining.");
                    progressBar.setProgress(0);
                    progressBar.setDisable(true);
                });
                return null;
            });



        }


    }

    private static final int maxMb = 10;
    private final ThreadFactory factory = r -> new Thread(r, "Mining RoIs");
    private final ExecutorService service = Executors.newSingleThreadExecutor(factory);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);


    private void onCancel(){

        isRunning.set(false);

        //stop all the algos
        for (SPMAlgorithm algorithm : spmAlgoChoiceBox.getItems()) {
            algorithm.stop();
        }

        roisPane.setDisable(false);
        progressBar.setProgress(0);
        mineRoIsBtn.setText("Mine RoIs");
        //reset ui
        setupMiningState(!selectedTrajs.isEmpty(), !miningSpaceChoiceBox.getItems().isEmpty());

        feedbackLabel.setText("Cancelled mining.");

    }

    @FXML
    public void onMineRoIsClicked(ActionEvent actionEvent) {

        if(isRunning.get()){
            //clicking this button again cancels the mining
            onCancel();

        }
        //start mining
        else{
            isRunning.set(true);
            feedbackLabel.setText("Mining RoIs...");

            //setup progress bar
            progressBar.setDisable(false);
            progressBar.setProgress(0);

            AbstractRoIMining algo = roiAlgorithmChoiceBox.getValue();

            //set progress reporter
            algo.setProgressReporter(progress -> Platform.runLater(()->{
                progressBar.setProgress(progress);
            }));

            //turn it into a cancel button
            mineRoIsBtn.setText("Cancel");
            roisPane.setDisable(true);
            final boolean doMineSeqPatterns = mineSPMCheckbox.isSelected();
            mineSPMCheckbox.setSelected(false);

            final RoIMiningSpace space = miningSpaceChoiceBox.getValue().getModel();
            final Map<String, ITrajectory> selectedTrajs = this.selectedTrajs;
            final SPMAlgorithm spmAlgo = spmAlgoChoiceBox.getValue();
            final AtomicBoolean keepRunningFileMonitor = new AtomicBoolean(false);

            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            feedbackLabel.setText("Mining sequences using: " + spmAlgo.getSimpleName());

            Runnable miningRunnable = () -> {
                try{

                    //mine and store rois
                    Collection<RoI> rois = mineRoIs(space);
                    storeRois(rois, space);
                    //mine and store sequential patterns
                    if(doMineSeqPatterns){

                        //do spm
                        SPMParameters spmParams = setupSPMParams(spmAlgo, selectedTrajs, rois, space);
                        Collection<SequentialPattern> patterns = spmAlgo.run(spmParams);

                        storeSequentialPatterns(patterns, spmAlgo);
                        patterns.clear();

                    }
                    rois.clear();

                    //done mining
                    if(isRunning.get()){
                        Platform.runLater(()->{
                            ((Stage)mineRoIsBtn.getScene().getWindow()).close();
                            service.shutdown();
                        });
                    }


                }catch (Exception e){
                    e.printStackTrace();
                    Platform.runLater(this::onCancel);
                }
                finally {
                    keepRunningFileMonitor.set(false);
                    //delete out file if it exists
                    isRunning.set(false);
                }

            };

            Thread runningTask = new Thread(miningRunnable, "Mining RoIs and Maybe SPM");
            runningTask.start();

        }

    }

    /**
     * If file exceeds the specified size, run the onDelete runnable.
     * @param file the file to monitor
     * @param maxMegabytes the max size in mb
     * @param isRunning if this monitor thread should keep running
     * @param onFileSizeReached what to do when file size too large
     */
    private void monitorFileForTooLarge(File file, int maxMegabytes, AtomicBoolean isRunning, Runnable onFileSizeReached){
        Thread task = new Thread(()->{
            isRunning.set(true);
            while(isRunning.get()){
                if(file.exists()){
                    double mb = file.length() / 1024.0 / 1024.0;
                    if(mb > maxMegabytes){
                        onFileSizeReached.run();
                        isRunning.set(false);
                        return;
                    }
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Monitor SPM file");
        task.start();
    }

    private void makeGridImpl(){
        double cellSize = cellSizeSpinner.getValue();

        //bounds
        double[][] bounds = BoundsUtil.calculateFromBoundingCoordinates(selectedTrajs.values());
        int[] nCellsPerDim = new int[bounds.length];
        for (int i = 0; i < bounds.length; i++) {
            double dimLength = bounds[i][1] - bounds[i][0];
            nCellsPerDim[i] = (int) (dimLength / cellSize);
        }

        RoIGrid grid = MiningSpaceFactory.createGrid(selectedTrajs, nCellsPerDim, 0);
        BaseTrajSuiteProgram.getInstance().getLayers().add("Grids", grid);
    }

    private void storeRois(Collection<RoI> rois, RoIMiningSpace space){

        AbstractGeographicProjection projection = null;
        for (ITrajectory traj : selectedTrajs.values()) {
            if(traj instanceof SpatioCompositeTrajectory){
                projection = ((SpatioCompositeTrajectory) traj).getProjection();
                break;
            }
        }

        //handle numeric RoIs
        if(projection == null){
            AddEntitiesTransaction transaction = new AddEntitiesTransaction();
            for (RoI roi : rois) {
                transaction.add("RoIs_" + IdGenerator.nextId(), String.valueOf(roi.getId()), roi);
            }
            ServiceLoader<TransactionProcessor> services = ServiceLoader.load(TransactionProcessor.class);
            for (TransactionProcessor transactionProcessor : services) {
                transactionProcessor.process(transaction);
            }
        }
        //handle geographic rois
        else{
            if(space instanceof RoIGrid){

                RoIGrid grid = (RoIGrid) space;

                //get min and max density
                int maxDensity = Integer.MIN_VALUE;

                //make the rect rois and observe min/max density in the dataset
                RectangularRoI[] rectRoIs = new RectangularRoI[rois.size()];
                int i = 0;
                for (RoI roi : rois) {
                    RectangularRoI rectangularRoI = new RectangularRoI(grid, roi, projection);
                    rectRoIs[i] = rectangularRoI;

                    //look for observed min/max density
                    int density = (int) rectangularRoI.getDensity();
                    if(density > maxDensity){
                        maxDensity = density;
                    }

                    i++;
                }

                //add the rect rois to transaction
                AddEntitiesTransaction transaction = new AddEntitiesTransaction();
                String layername = "RoIs_" + IdGenerator.nextId();

                for (RectangularRoI rectangularRoI : rectRoIs) {
                    String roiId = String.valueOf(rectangularRoI.getId());
                    RoIGraphic graphic = new RoIGraphic(rectangularRoI, 0, maxDensity);

                    double[] centreLatLon = rectangularRoI.getLatLonBounds().getLatLonCentroid();

                    LabelPrefab labelPrefab = new LabelPrefab(rectangularRoI.toString(), true, centreLatLon);

                    graphic.additionalPrefabs.add(labelPrefab);
                    transaction.add(layername, roiId, rectangularRoI, graphic);
                }

                ServiceLoader<TransactionProcessor> services = ServiceLoader.load(TransactionProcessor.class);
                for (TransactionProcessor transactionProcessor : services) {
                    transactionProcessor.process(transaction);
                }
            }
        }
    }

    private SPMParameters setupSPMParams(SPMAlgorithm spmAlgo, Map<String, ITrajectory> selectedTrajs, Collection<RoI> rois, RoIMiningSpace space){

        int[][] seqDb = new int[selectedTrajs.size()][];

        int i = 0;
        for (Map.Entry<String, ITrajectory> entry : selectedTrajs.entrySet()) {
            int[] sequence = TrajectoryRoIUtil.fromTrajToRoISequence(entry.getValue(), rois, (RoIGrid) space);
            seqDb[i] = sequence;
            i++;
        }

        int minSup = minsupSpinner.getValue();
        int otherParam = spmParamSpinner == null ? 1 : spmParamSpinner.getValue();

        SPMParameters params = new SPMParameters(seqDb, minSup);
        if(spmAlgo instanceof DCSpan){
            params.setMaxRedund(otherParam / 100.0d);
        }

        return params;
    }

    private Collection<RoI> mineRoIs(RoIMiningSpace miningSpace){
        int cellDensity = minimumDensitySpinner.getValue();
        AbstractRoIMining algo = roiAlgorithmChoiceBox.getValue();
        return algo.run(miningSpace, cellDensity);
    }

    private void storeSequentialPatterns(Collection<SequentialPattern> patterns, SPMAlgorithm algorithm){

        AddEntitiesTransaction transaction = new AddEntitiesTransaction();

        String layername = "Sequential Patterns - " + algorithm.getSimpleName();

        //todo: will have to invent some geographical sequence object

        for (SequentialPattern pattern : patterns) {
            transaction.add(layername, IdGenerator.nextId(), pattern);
        }

        ServiceLoader<TransactionProcessor> services = ServiceLoader.load(TransactionProcessor.class);
        for (TransactionProcessor transactionProcessor : services) {
            transactionProcessor.process(transaction);
        }

    }

}
