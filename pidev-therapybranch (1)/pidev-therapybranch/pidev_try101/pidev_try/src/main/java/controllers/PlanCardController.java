package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class PlanCardController {

    @FXML
    private Label planTitle;

    @FXML
    private ImageView planImage;

    public void setData(String title, String imagePath) {
        planTitle.setText(title);

        Image img = new Image(getClass().getResourceAsStream(imagePath));
        planImage.setImage(img);
    }
}
