package com.anon.nhsm.app;

import com.anon.nhsm.data.SaveManager;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private VBox contentAreaRyujinx;

    @FXML
    private VBox contentAreaYuzu;

    @FXML
    private HBox sideMenuRyujinx;

    @FXML
    private HBox sideMenuYuzu;

    private List<VBox> contentAreas;
    private Map<HBox, VBox> menuToContentArea;

    public AnchorPane getAnchorPane() {
        return ap;
    }

    public void init(final SaveManager saveManager) {
        this.saveManager = saveManager;
        contentAreas = List.of(contentAreaNoSelection, contentAreaRyujinx, contentAreaYuzu);
        menuToContentArea = new IdentityHashMap<>();
        menuToContentArea.put(sideMenuRyujinx, contentAreaRyujinx);
        menuToContentArea.put(sideMenuYuzu, contentAreaYuzu);
    }

    public void selectContentArea(final VBox contentArea) {
        contentAreas.forEach(area -> {
            area.setVisible(false);
            area.setDisable(true);
        });

        contentArea.setDisable(false);
        contentArea.setVisible(true);
    }

    @FXML
    void handleClickMenu(MouseEvent event) {
        menuToContentArea.keySet().stream()
                .filter(event.getSource()::equals)
                .map(menuToContentArea::get)
                .findFirst().ifPresent(this::selectContentArea);
    }
}
