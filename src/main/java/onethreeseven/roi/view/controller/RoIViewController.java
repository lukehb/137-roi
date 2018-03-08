package onethreeseven.roi.view.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import onethreeseven.roi.algorithm.AbstractRoIMining;
import onethreeseven.roi.model.RoIMiningSpace;

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
    public ChoiceBox<RoIMiningSpace> miningSpaceChoiceBox;
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

    @FXML
    public void initialize(){

    }

    @FXML
    public void onMakeGridClicked(ActionEvent actionEvent) {

    }

    @FXML
    public void onMineRoIsClicked(ActionEvent actionEvent) {

    }
}
