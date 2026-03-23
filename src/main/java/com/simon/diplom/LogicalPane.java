package com.simon.diplom;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;

import java.util.List;

public class LogicalPane extends Pane {
    private Label name = new Label();
    private Double basicWidth;
    private Double basicLayoutX;
    private Double basicLayoutY;

    public LogicalPane(NodeType type, double x, double y, double scale){
        if(type == NodeType.AND){
            name.setText("&");
        }
        else{
            name.setText("V");
        }
        this.getChildren().add(name);
        basicWidth = 20.0;
        this.setPrefSize(20, 20);
        name.layoutXProperty().bind(this.widthProperty().multiply(0.5).subtract(name.widthProperty().divide(2)));
        name.layoutYProperty().bind(this.heightProperty().multiply(0.5).subtract(name.heightProperty().divide(2)));
        basicLayoutX = x/scale;
        basicLayoutY = y/scale;
        resize(scale);
    }

    public Double getBasicWidth() {
        return basicWidth;
    }

    public void setBasicWidth(Double basicWidth) {
        this.basicWidth = basicWidth;
    }

    public Double getBasicLayoutX() {
        return basicLayoutX;
    }

    public void setBasicLayoutX(Double basicLayoutX) {
        this.basicLayoutX = basicLayoutX;
    }

    public Double getBasicLayoutY() {
        return basicLayoutY;
    }

    public void setBasicLayoutY(Double basicLayoutY) {
        this.basicLayoutY = basicLayoutY;
    }

    public void resize(double scale){
        Font newFont = new Font(12*scale);
        this.name.setFont(newFont);

        this.applyCss();
        this.layout();

        double oldWidth = this.getPrefWidth();
        double oldHeight = this.getPrefHeight();
        double width = scale*basicWidth;
        double height = scale*basicWidth;
        this.setPrefSize(width, height);
        ModelNodeGraphics nodeData = (ModelNodeGraphics) this.getUserData();
        if(nodeData!=null) {
            for (Line inputLink : nodeData.getInputLinks()) {
                inputLink.setEndY(inputLink.getEndY() + (height - oldHeight) / 2);
            }
            for (Line outputLink : nodeData.getOutputLinks()) {
                outputLink.setStartX(outputLink.getStartX() + (width - oldWidth));
                outputLink.setStartY(outputLink.getStartY() + (height - oldHeight) / 2);
            }
            List<Circle> marks = this.getChildren().stream().filter(o -> o instanceof Circle).map(o -> (Circle) o).toList();
            for (Circle mark : marks) {
                mark.setRadius(3.5 * scale);
            }
        }
    }
}
