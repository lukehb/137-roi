package onethreeseven.roi;

import javafx.stage.Stage;
import onethreeseven.trajsuitePlugin.model.BaseTrajSuiteProgram;
import onethreeseven.trajsuitePlugin.view.BasicFxApplication;

/**
 * Todo: write documentation
 *
 * @author Luke Bermingham
 */
public class Main extends BasicFxApplication {

    @Override
    protected BaseTrajSuiteProgram preStart(Stage stage) {
        return BaseTrajSuiteProgram.getInstance();
    }

    @Override
    public String getTitle() {
        return "RoIs Module";
    }

    @Override
    public int getStartWidth() {
        return 640;
    }

    @Override
    public int getStartHeight() {
        return 480;
    }

    @Override
    protected void afterStart(Stage stage) {
        super.afterStart(stage);

        BaseTrajSuiteProgram.getInstance().getCLI().doCommand("gt -ne 1000 -nt 50 -n 50".split(" "));

    }
}
