package com.anon.nhsm.app;

import com.anon.nhsm.data.SaveManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class EmulatorSelectorController {
    private SaveManager saveManager;

    @FXML
    private AnchorPane ap;

    @FXML
    private VBox contentAreaNoSelection;

    @FXML
    private Pane contentAreaRyujinx;

    @FXML
    private Pane contentAreaYuzu;

    @FXML
    private HBox sideMenuRyujinx;

    @FXML
    private HBox sideMenuYuzu;

    @FXML
    private Button buttonOpenSavesManager;

    private List<Pane> contentAreas;
    private Map<HBox, EmulatorData> menuToEmulatorData;

    private static class EmulatorData {
        private final Pane contentArea;
        private boolean viable;

        public EmulatorData(final Pane contentArea) {
            this.contentArea = contentArea;
        }

        public Pane getContentArea() {
            return contentArea;
        }

        public boolean isViable() {
            return viable;
        }

        public EmulatorData setViable(final boolean viable) {
            this.viable = viable;
            return this;
        }
    }

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void init(final SaveManager saveManager) {
        this.saveManager = saveManager;
        contentAreas = List.of(contentAreaNoSelection, contentAreaRyujinx, contentAreaYuzu);
        menuToEmulatorData = new IdentityHashMap<>();
        menuToEmulatorData.put(sideMenuRyujinx, new EmulatorData(contentAreaRyujinx));
        menuToEmulatorData.put(sideMenuYuzu, new EmulatorData(contentAreaYuzu).setViable(true));
    }

    public void selectContentArea(final Pane contentArea) {
        contentAreas.forEach(area -> {
            area.setVisible(false);
            area.setDisable(true);
        });

        contentArea.setDisable(false);
        contentArea.setVisible(true);
    }

    @FXML
    void handleClickMenu(MouseEvent event) {
        menuToEmulatorData.keySet().stream()
                .filter(event.getSource()::equals)
                .map(menuToEmulatorData::get)
                .findFirst().ifPresent(emulator -> {
                    buttonOpenSavesManager.setDisable(!emulator.isViable());
                    selectContentArea(emulator.getContentArea());
                });
    }

    @FXML
    void handleOpenSaveManager(MouseEvent event) {
        try {
            Application.showApplication(Application.PRIMARY_STAGE);
        } catch (IOException e) {
            Application.openErrorAlert(e);
        }
    }
}
