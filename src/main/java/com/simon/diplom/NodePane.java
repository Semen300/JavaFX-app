package com.simon.diplom;

import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class NodePane extends Pane {
    private Label name = new Label();
    private Label id = new Label();
    private Label priorHeader;
    private Label prior = new Label();
    private Label posteriorHeader;
    private Label posterior = new Label();
    private List<Line> inputLinks = new ArrayList<>();
    private List<Line> outputLinks = new ArrayList<>();
    private final Double MIN_WIDTH = 120.0;
    private Double basicWidth;
    private Double basicLayoutX;
    private Double basicLayoutY;

    public NodePane(Integer id, String name, double x, double y, double scale){
        this.name.setText(name);
        this.id.setText("id: " + id);
        Text nameText = new Text(this.name.getText());
        Text idText = new Text(this.id.getText());
        double width = Math.max(1.1*nameText.getLayoutBounds().getWidth()+1.1*idText.getLayoutBounds().getWidth(), MIN_WIDTH);
        basicWidth = width;
        basicLayoutX = x/scale;
        basicLayoutY = y/scale;
        double height = width/2;
        this.setPrefSize(width, height);

        this.name.layoutXProperty().set(5);
        this.name.layoutYProperty().bind(this.heightProperty().multiply(0.16667).subtract(this.name.heightProperty().divide(2)));
        this.getChildren().add(this.name);

        this.id.layoutXProperty().bind(this.widthProperty().subtract(this.id.widthProperty()).subtract(5));
        this.id.layoutYProperty().bind(this.heightProperty().multiply(0.16667).subtract(this.name.heightProperty().divide(2)));
        this.getChildren().add(this.id);

        Line lineTop = new Line();
        lineTop.startXProperty().set(0);
        lineTop.endXProperty().bind(this.widthProperty());
        lineTop.startYProperty().bind(this.heightProperty().multiply(0.33333));
        lineTop.endYProperty().bind(this.heightProperty().multiply(0.33333));
        this.getChildren().add(lineTop);

        priorHeader = new Label("Prior:");
        priorHeader.layoutXProperty().bind(this.widthProperty().multiply(0.25).subtract(priorHeader.widthProperty().divide(2)));
        priorHeader.layoutYProperty().bind(this.heightProperty().multiply(0.46).subtract(priorHeader.heightProperty().divide(2)));
        this.getChildren().add(priorHeader);

        posteriorHeader = new Label("Posterior:");
        posteriorHeader.layoutXProperty().bind(this.widthProperty().multiply(0.75).subtract(posteriorHeader.widthProperty().divide(2)));
        posteriorHeader.layoutYProperty().bind(this.heightProperty().multiply(0.46).subtract(posteriorHeader.heightProperty().divide(2)));
        this.getChildren().add(posteriorHeader);

        Line lineMiddle = new Line();
        lineMiddle.startXProperty().set(0);
        lineMiddle.endXProperty().bind(this.widthProperty());
        lineMiddle.startYProperty().bind(this.heightProperty().multiply(0.58));
        lineMiddle.endYProperty().bind(this.heightProperty().multiply(0.58));
        this.getChildren().add(lineMiddle);

        this.prior.setText("-----");
        this.prior.layoutXProperty().bind(this.widthProperty().multiply(0.25).subtract(this.prior.widthProperty().divide(2)));
        this.prior.layoutYProperty().bind(this.heightProperty().multiply(0.79).subtract(this.prior.heightProperty().divide(2)));
        this.getChildren().add(this.prior);

        this.posterior.setText("-----");
        this.posterior.layoutXProperty().bind(this.widthProperty().multiply(0.75).subtract(this.posterior.widthProperty().divide(2)));
        this.posterior.layoutYProperty().bind(this.heightProperty().multiply(0.79).subtract(this.posterior.heightProperty().divide(2)));
        this.getChildren().add(this.posterior);

        Line lineVertical = new Line();
        lineVertical.startXProperty().bind(this.widthProperty().multiply(0.5));
        lineVertical.endXProperty().bind(this.widthProperty().multiply(0.5));
        lineVertical.startYProperty().bind(this.heightProperty().multiply(0.33333));
        lineVertical.endYProperty().bind(this.heightProperty());
        this.getChildren().add(lineVertical);

        resize(scale);
        this.setLayoutX(x);
        this.setLayoutY(y);
        this.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black;");
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

    public void setName(String name, double scale) {
        Text nameText = new Text(name);
        this.name.setText(name);
        Text idText = new Text(this.id.getText());
        basicWidth = Math.max(1.1*nameText.getLayoutBounds().getWidth()+1.1*idText.getLayoutBounds().getWidth(), MIN_WIDTH);
        resize(scale);
    }

    public void setPrior(Double prior){
        if(prior!=null) this.prior.setText(String.format("%.3f", prior));
        else this.prior.setText("----");
    }

    public void setPosterior(Double posterior){
        if(posterior!=null) this.posterior.setText(String.format("%.3f", posterior));
        else this.posterior.setText("----");
    }

    public void resize(double scale){
        Font newFont = new Font(12*scale);
        this.name.setFont(newFont);
        this.id.setFont(newFont);
        this.priorHeader.setFont(newFont);
        this.prior.setFont(newFont);
        this.posteriorHeader.setFont(newFont);
        this.posterior.setFont(newFont);

        this.applyCss();
        this.layout();

        double oldWidth = this.getPrefWidth();
        double oldHeight = this.getPrefHeight();
        double width = scale*basicWidth;
        double height = scale*basicWidth/2;
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
