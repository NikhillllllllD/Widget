package com.aavash.gui.widget;

import com.aavash.config.ConfigModel;
import com.aavash.config.ConfigurationService;
import com.aavash.dataFetch.DataProviderService;
import com.aavash.dataFetch.Model.CountryData;
import com.aavash.dataFetch.Model.CovidDataModel;
import com.aavash.dataFetch.Model.GlobalData;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WidgetController implements Initializable {

    private ScheduledExecutorService executorService;
    private ConfigModel configModel;

    @FXML
    public AnchorPane rootPane;
    @FXML
    public Text textGlobalReport, textCountryCode, textCountryReport;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configModel = new ConfigurationService().getConfiguration();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initializeScheduler();
        initializeContextMenu();
        textCountryCode.setText(configModel.getCountryCode());
    }

    private void initializeScheduler() {

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(this::loadData,0,configModel.getRefreshIntervalInSeconds(), TimeUnit.SECONDS);
    }

    private void loadData(){
        DataProviderService dataProviderService = new DataProviderService();
        CovidDataModel covidDataModel = dataProviderService.getData(configModel.getCountryName());

        Platform.runLater(()-> {
            inflateData(covidDataModel);
        });
    }

    private void inflateData(CovidDataModel covidDataModel){
        GlobalData globalData = covidDataModel.getGlobalData();
        textGlobalReport.setText(getFormattedData(globalData.getCases(),globalData.getRecovered(),globalData.getDeaths()));

        CountryData countryData = covidDataModel.getCountryData();
        textCountryReport.setText(getFormattedData(countryData.getCases(),countryData.getRecovered(),countryData.getDeaths()));

        readjustStage(textCountryCode.getScene().getWindow());
    }

    private String getFormattedData(long totalCases,long recoveredCases,long totalDeaths){
        return String.format("Cases: %-8d | Recovered: %-6d | Deaths: %-6d",totalCases,recoveredCases,totalDeaths);
    }

    private void readjustStage(Window stage){
        stage.sizeToScene();

        Rectangle2D visibleBounds = Screen.getPrimary().getVisualBounds();
        stage.setX(visibleBounds.getMaxX()-25-textCountryCode.getScene().getWidth());
        stage.setY(visibleBounds.getMinY()+25);
    }

    private  void initializeContextMenu(){
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            System.exit(0);
        });

        MenuItem refreshItem = new MenuItem("Refresh Now");
        refreshItem.setOnAction(event -> {
            executorService.schedule(this::loadData,0,TimeUnit.SECONDS);
        });

        final ContextMenu contextMenu = new ContextMenu(exitItem,refreshItem);

        rootPane.addEventHandler(MouseEvent.MOUSE_PRESSED,event -> {
            if (event.isSecondaryButtonDown()){
                contextMenu.show(rootPane,event.getScreenX(),event.getScreenY());
            }else{
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });
    }
}
