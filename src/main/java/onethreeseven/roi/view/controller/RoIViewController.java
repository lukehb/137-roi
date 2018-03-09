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
import onethreeseven.datastructures.model.ITrajectory;
import onethreeseven.datastructures.model.SpatioCompositeTrajectory;
import onethreeseven.geo.projection.AbstractGeographicProjection;
import onethreeseven.roi.algorithm.*;
import onethreeseven.roi.graphics.RoIGraphic;
import onethreeseven.roi.model.*;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.model.EntitySupplier;
import onethreeseven.trajsuitePlugin.model.WrappedEntity;
import onethreeseven.trajsuitePlugin.transaction.AddEntitiesTransaction;
import onethreeseven.trajsuitePlugin.transaction.RemoveEntitiesTransaction;
import onethreeseven.trajsuitePlugin.util.BoundsUtil;
import onethreeseven.trajsuitePlugin.util.IdGenerator;
import onethreeseven.trajsuitePlugin.view.EntitySelector;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Controller for RoI view fxml
 * @author Luke Bermingham
 */
public class RoIViewController {

    @FXML
    public Label nSelectedTrajsLabel;
    @FXML
    public Button makeGridBtn;
    @FXML
    public Spinner<Integer> cellSizeSpinner;
    @FXML
    public ChoiceBox<WrappedEntity<RoIMiningSpace>> miningSpaceChoiceBox;
    @FXML
    public Label roiWarningLabel;
    @FXML
    public ChoiceBox<AbstractRoIMining> roiAlgorithmChoiceBox;
    @FXML
    public Spinner<Integer> minimumDensitySpinner;
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
            roiWarningLabel.setText("Please make a grid for RoI mining.");
            miningSpaceChoiceBox.setDisable(true);
            minimumDensitySpinner.setDisable(true);
            roiAlgorithmChoiceBox.setDisable(true);
            mineRoIsBtn.setDisable(true);
        }

        if(hasTrajs){
            cellSizeSpinner.setDisable(false);
            makeGridBtn.setDisable(false);
        }
        //no trajs
        else{
            cellSizeSpinner.setDisable(true);
            roiWarningLabel.setText("Please select some trajectories to mine RoIs from.");
            makeGridBtn.setDisable(true);
        }

        if(hasTrajs && hasGrid){
            miningSpaceChoiceBox.setDisable(false);
            roiWarningLabel.setText("");
            roiAlgorithmChoiceBox.setDisable(false);
            mineRoIsBtn.setDisable(false);
            //setup min density spinner
            int numSelectedTrajs = selectedTrajs.size();
            minimumDensitySpinner.setDisable(false);
            minimumDensitySpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, numSelectedTrajs, 1));
            minimumDensitySpinner.getValueFactory().setValue((int) (numSelectedTrajs * 0.5));
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

        refreshSelectedTrajs();
        setupMiningSpaceChoiceBox();


    }

    @FXML
    public void onMakeGridClicked(ActionEvent actionEvent) {

        //make the grid
        if(!selectedTrajs.isEmpty()){

            ThreadFactory factory = r -> new Thread(r, "Making Grid");
            final ExecutorService service = Executors.newSingleThreadExecutor(factory);

            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
            progressBar.setDisable(false);

            //make the grid
            CompletableFuture.runAsync(this::makeGridImpl, service).handle((aVoid, throwable) -> {
                service.shutdown();
                Platform.runLater(()->{
                    progressBar.setProgress(0);
                    progressBar.setDisable(true);
                });
                return null;
            });



        }


    }

    @FXML
    public void onMineRoIsClicked(ActionEvent actionEvent) {

        AbstractRoIMining algo = roiAlgorithmChoiceBox.getValue();

        //set progress reporter
        algo.setProgressReporter(progress -> Platform.runLater(()->{
            progressBar.setProgress(progress);
        }));

        //setup progress bar
        progressBar.setDisable(false);
        progressBar.setProgress(0);

        minimumDensitySpinner.setDisable(true);
        cellSizeSpinner.setDisable(true);
        mineRoIsBtn.setDisable(true);

        ThreadFactory factory = r -> new Thread(r, "Mining RoIs");
        final ExecutorService service = Executors.newSingleThreadExecutor(factory);

        CompletableFuture.runAsync(this::mineRoIsImpl, service).handle((aVoid, throwable) -> {
            //error
            if(throwable != null){
                throwable.printStackTrace();
                //reset ui
                Platform.runLater(()-> setupMiningState(!selectedTrajs.isEmpty(), !miningSpaceChoiceBox.getItems().isEmpty()));
            }
            //worked fine
            else{
                Platform.runLater(()->{
                    ((Stage)mineRoIsBtn.getScene().getWindow()).close();
                });
            }

            service.shutdown();
            return null;
        });

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

    private void mineRoIsImpl(){
        if(selectedTrajs.isEmpty()){
            return;
        }

        AbstractGeographicProjection projection = null;
        for (ITrajectory traj : selectedTrajs.values()) {
            if(traj instanceof SpatioCompositeTrajectory){
                projection = ((SpatioCompositeTrajectory) traj).getProjection();
                break;
            }
        }

        RoIMiningSpace space = miningSpaceChoiceBox.getValue().getModel();
        int cellDensity = minimumDensitySpinner.getValue();
        AbstractRoIMining algo = roiAlgorithmChoiceBox.getValue();

        Collection<RoI> rois = algo.run(space, cellDensity);


        //handle numeric RoIs
        if(projection == null){
            AddEntitiesTransaction transaction = new AddEntitiesTransaction();
            for (RoI roi : rois) {
                transaction.add("RoIs_" + IdGenerator.nextId(), String.valueOf(roi.getId()), roi);
            }
            BaseTrajSuiteProgram.getInstance().getLayers().process(transaction);
        }
        //handle geographic rois
        else{
            if(space instanceof RoIGrid){
                AddEntitiesTransaction transaction = new AddEntitiesTransaction();
                RoIGrid grid = (RoIGrid) space;
                String layername = "RoIs_" + IdGenerator.nextId();

                for (RoI roi : rois) {
                    RectangularRoI rectangularRoI = new RectangularRoI(grid, roi, projection);
                    String roiId = String.valueOf(rectangularRoI.getId());
                    transaction.add(layername, roiId, rectangularRoI, new RoIGraphic(rectangularRoI));
                }

                BaseTrajSuiteProgram.getInstance().getLayers().process(transaction);
            }
        }
    }

}
