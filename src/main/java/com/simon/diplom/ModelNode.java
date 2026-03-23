package com.simon.diplom;

import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModelNode {
    private Integer ID;
    private String name;
    private String disc;
    private List<ModelNode> previousNodes = new ArrayList<>();
    private List<ModelNode> nextNodes = new ArrayList<>();
    private List<Double> probabilityTable = new ArrayList<>();
    private Pane nodePane = null;
    private NodeType type;
    private boolean isGood = false;

    public ModelNode(Integer ID, String name, NodeType type){
        this.ID = ID;
        this.name = name;
        this.type = type;
        this.disc = "";
        resetProbTable();
    }

    public ModelNode(Integer ID, String name, String disc, List<ModelNode> previousNodes, List<Double> probabilityTable, NodeType type) {
        this.ID = ID;
        this.name = name;
        this.disc = disc;
        this.previousNodes = previousNodes;
        this.probabilityTable = probabilityTable;
        this.type = type;
        if(type == NodeType.OR) fillORProbTable();
        else if(type == NodeType.AND) fillANDProbTable();
        nodePane = null;
        isGood = true;
    }

    @Override
    public String toString() {
        return "ModelNode{" +
                "ID=" + ID +
                ", name='" + name + '\'' +
                '}';
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisc() {
        return disc;
    }

    public void setDisc(String disc) {
        this.disc = disc;
    }

    public boolean isGood() {
        return isGood;
    }

    public void setGood(boolean good) {
        isGood = good;
    }

    public List<ModelNode> getPreviousNodes() {
        return previousNodes;
    }

    public void setPreviousNodes(List<ModelNode> previousNodes) {
        this.previousNodes = previousNodes;
    }

    public List<ModelNode> getNextNodes() {
        return nextNodes;
    }

    public void setNextNodes(List<ModelNode> nextNodes) {
        this.nextNodes = nextNodes;
    }

    public Pane getNodePane() {
        return nodePane;
    }

    public void setNodePane(Pane nodePane) {
        this.nodePane = nodePane;
    }

    public List<Double> getProbabilityTable() {
        return probabilityTable;
    }

    public void setProbabilityTable(List<Double> probabilityTable) {
        if(probabilityTable.size()!=Math.pow(2, previousNodes.size())) throw new RuntimeException("Неверная размерность таблицы");
        this.probabilityTable = probabilityTable;
        isGood=true;
        nodePane.setStyle("-fx-background-color: #FFD700; -fx-border-color: black; -fx-border-width: 1");
    }

    public Integer getTableSize(){
        return (int) Math.pow(2, countPrevious());
    }

    public void addPrevious(ModelNode node){
        previousNodes.add(node);
        switch (type){
            case BASIC -> resetProbTable();
            case AND -> {
                fillANDProbTable();
                changeColorForLN();
            }
            case OR -> {
                fillORProbTable();
                changeColorForLN();
            }
        }
    }

    public void addNext(ModelNode node){
        nextNodes.add(node);
    }
    public void changeColorForLNOnAddNext(){
        if(type == NodeType.OR || type==NodeType.AND) {
            if (countPrevious() >= 2) {
                isGood = true;
                nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
            } else {
                isGood = false;
                nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
            }
        }
    }

    public void deletePrevious(ModelNode node){
        previousNodes.remove(node);
        switch (type){
            case BASIC -> resetProbTable();
            case AND -> {
                fillANDProbTable();
                changeColorForLN();
            }
            case OR -> {
                fillORProbTable();
                changeColorForLN();
            }
        }
    }

    public void deleteNext(ModelNode node) {
        nextNodes.remove(node);
        if(type == NodeType.OR || type==NodeType.AND) {
            if(nextNodes.size()==0){
                isGood = false;
                nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
            }
        }
    }
    public Integer countPrevious(){
        return previousNodes.size();
    }
    public Integer countNext(){
        return nextNodes.size();
    }

    public NodeType getType() {
        return type;
    }

    public void setType(NodeType type) {
        this.type = type;
    }

    public void resetProbTable(){
        int numberOfPrevious = countPrevious();
        probabilityTable =  new ArrayList<>(Collections.nCopies((int)Math.pow(2, numberOfPrevious),0.0));
        isGood = false;
        if(nodePane != null) nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
    }

    public void fillANDProbTable(){
        int numberOfPrevious = countPrevious();
        probabilityTable = new ArrayList<>();
        probabilityTable.addAll(Collections.nCopies((int)Math.pow(2, numberOfPrevious) - 1, 0.0));
        probabilityTable.add(1.0);
    }

    public void fillORProbTable(){
        int numberOfPrevious = countPrevious();
        probabilityTable = new ArrayList<>();
        probabilityTable.add(0.0);
        probabilityTable.addAll(Collections.nCopies((int)Math.pow(2, numberOfPrevious) - 1, 1.0));
    }
    public void changeColorForLN(){
        if(previousNodes.size()<2 || nextNodes.size()==0){
            isGood = false;
            nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
        }
        else{
            isGood = true;
            nodePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        }
    }

    public double getPosProb(List<Integer> mapping){
        if(mapping.size()==countPrevious()) {
            return probabilityTable.get(MyMath.toDecimal(mapping));
        }
        else throw new RuntimeException("Размер запрашиваемого маппинга не совпадает с размерностью ТУВ");
    }
    public double getNegProb(List<Integer> mapping){
        if(mapping.size()==countPrevious()) {
            return 1-probabilityTable.get(MyMath.toDecimal(mapping));
        }
        else throw new RuntimeException("Размер запрашиваемого маппинга не совпадает с размерностью ТУВ");
    }

    public void addAncestors(ModelNode node, List<ModelNode> listToAdd){
        if(!listToAdd.contains(node)){
            listToAdd.add(node);
        }
        if(node.getPreviousNodes().size()!=0){
            for(ModelNode prevNode : node.getPreviousNodes()){
                addAncestors(prevNode, listToAdd);
            }
        }
    }

    public List<ModelNode> getAncestors(){
        List<ModelNode> ancestors = new ArrayList<>();
        addAncestors(this, ancestors);
        return ancestors;
    }

    private void addReasons(ModelNode node, List<ModelNode> listToAdd){
        if(node.countPrevious() == 0 && node.getType() == NodeType.BASIC){
            if(!listToAdd.contains(node)) listToAdd.add(node);
        }
        else{
            for(ModelNode prevNode: node.getPreviousNodes()){
                addReasons(prevNode, listToAdd);
            }
        }
    }

    public List<ModelNode> getReasons(){
        List<ModelNode> reasons = new ArrayList<>();
        addReasons(this, reasons);
        return reasons;
    }

    private void addConsequences(ModelNode node, List<ModelNode> listToAdd){
        if(node.countNext() == 0 && node.getType() == NodeType.BASIC){
            if(!listToAdd.contains(node)) listToAdd.add(node);
        }
        else{
            for(ModelNode nextNode: node.getNextNodes()){
                addConsequences(nextNode, listToAdd);
            }
        }
    }

    public List<ModelNode> getConsequences(){
        List<ModelNode> consequences = new ArrayList<>();
        addConsequences(this, consequences);
        return consequences;
    }
}
