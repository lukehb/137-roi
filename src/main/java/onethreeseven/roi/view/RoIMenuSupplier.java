package onethreeseven.roi.view;

import javafx.stage.Stage;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.view.*;

/**
 * Supplies menus from this module to others that use this supplier as a service loader.
 * @author Luke Bermingham
 */
public class RoIMenuSupplier implements MenuSupplier {

    @Override
    public void supplyMenus(AbstractMenuBarPopulator populator, BaseTrajSuiteProgram program, Stage primaryStage) {

        TrajSuiteMenu roiMenu = new TrajSuiteMenu("RoIs", 4);
        TrajSuiteMenuItem mineRoIs = new TrajSuiteMenuItem("Mine RoIs", makeMineRoIsMenuItem(primaryStage));
        roiMenu.addChild(mineRoIs);

        populator.addMenu(roiMenu);
    }



    private Runnable makeView(Stage primaryStage, String resource, String title){
        return () -> ViewUtil.loadUtilityView(RoIMenuSupplier.class, primaryStage, title, resource);
    }

    private Runnable makeMineRoIsMenuItem(Stage primaryStage){
        return makeView(primaryStage, "/onethreeseven/roi/view/RoIsView.fxml", "Mine RoIs");
    }

}
