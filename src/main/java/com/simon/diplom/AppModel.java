package com.simon.diplom;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
@Component
public class AppModel {
    private List<ModelNode> nodes;
    private int lastID;
    private List<Double> probTable;

    public AppModel(){
        this.nodes = new ArrayList<>();
        this.probTable = new ArrayList<>();
        this.lastID=0;
    }
    public List<ModelNode> getNodes() {
        return nodes;
    }
    public void setNodes(List<ModelNode> nodes) {
        this.nodes = nodes;
    }
    public ModelNode getNodeByID(int id){
        ModelNode nodeToReturn = null;
        for(ModelNode node:nodes){
            if(node.getID()==id) nodeToReturn=node;
        }
        return nodeToReturn;
    }
    public int getLastID() {
        return lastID;
    }
    public void setLastID(int lastID) {
        this.lastID = lastID;
    }
    public void addNode(ModelNode node){
        if(!nodes.contains(node)) {
            lastID++;
            if (node.getID() == null) node.setID(lastID);
            nodes.add(node);
        }
        else{
            System.out.println("Узел "+node+" уже присутствует в модели");
        }
    }
    public void deleteNode(ModelNode node){
        nodes.remove(node);
        if(!nodes.isEmpty()){
            lastID = nodes.getLast().getID();
        }
        else lastID = 0;
    }
    public int countObjects(){
        return nodes.size();
    }
    public List<Double> getProbTable() {
        return probTable;
    }
    public void setProbTable(List<Double> probTable) {
        this.probTable = probTable;
    }
    public Integer countBad(){
        return (Integer) (int) nodes.stream().filter(o->!o.isGood()).count();
    }
    public void fillProbTable(){
        probTable = new ArrayList<>();
        for(int i=0;i<Math.pow(2, nodes.size());i++){
            double prob = 1;
            List<Integer> mapping = MyMath.toBinary(i, nodes.size());
            for(int j=0;j<mapping.size();j++){
                List<ModelNode> prevNodes = nodes.get(j).getPreviousNodes();
                if(mapping.get(j)==0) {
                    if (prevNodes.isEmpty()) {
                        prob *= nodes.get(j).getNegProb(new ArrayList<>());
                    }
                    else{
                        List<Integer> mapping1 = new ArrayList<>();
                        for (ModelNode prevNode : prevNodes) {
                            int index = nodes.indexOf(prevNode);
                            mapping1.add(mapping.get(index));
                        }
                        prob *= nodes.get(j).getNegProb(mapping1);
                    }
                }
                else{
                    if (prevNodes.isEmpty()) {
                        prob *= nodes.get(j).getPosProb(new ArrayList<>());
                    }
                    else{
                        List<Integer> mapping1 = new ArrayList<>();
                        for (ModelNode prevNode : prevNodes) {
                            int index = nodes.indexOf(prevNode);
                            mapping1.add(mapping.get(index));
                        }
                        prob *= nodes.get(j).getPosProb(mapping1);
                    }
                }
            }
            probTable.add(prob);
        }
    }
    public double sumProbTable(List<Integer> mapping){
        double sum = 0;
        for(int i=0;i<probTable.size();i++){
            List<Integer> indexMapping = MyMath.toBinary(i, countObjects());
            boolean isGood = true;
            for(int j=0;j<indexMapping.size();j++){
                if (!Objects.equals(mapping.get(j), indexMapping.get(j)) && mapping.get(j) != 2) {
                    isGood = false;
                    break;
                }
            }
            if(isGood) sum+=probTable.get(i);
        }
        return sum;
    }
    public List<Double> check(List<ModelNode> posEvidence, List<ModelNode> negEvidence, List<ModelNode> toCheck) {
        List<Double> toReturn = new ArrayList<>();
        for (int i = 0; i < Math.pow(2, toCheck.size()); i++) {
            List<Integer> mapping = new ArrayList<>(Collections.nCopies(countObjects(), 2));
            List<Integer> mapping1 = MyMath.toBinary(i, toCheck.size());
            if (posEvidence != null) {
                for (ModelNode evi : posEvidence) {
                    mapping.set(nodes.indexOf(evi), 1);
                }
            }
            if(negEvidence != null){
                for(ModelNode evi : negEvidence){
                    mapping.set(nodes.indexOf(evi), 0);
                }
            }
            for (int j = 0; j < mapping1.size(); j++) {
                int index = nodes.indexOf(toCheck.get(j));
                mapping.set(index, mapping1.get(j));
            }
            toReturn.add(sumProbTable(mapping));
        }
        return toReturn;
    }
    public Double getProbByEvidence(List<ModelNode> positiveEvidence, List<ModelNode> negativeEvidence, ModelNode toCheck){
        List<Double> list = check(positiveEvidence, negativeEvidence, List.of(toCheck));
        MyMath.standardize(list);
        return list.get(1);
    }
    public void clear(){
        lastID = 0;
        nodes.clear();
        probTable.clear();
    }
    public static int getLongestPath(ModelNode node) {
        if (node.getPreviousNodes() == null || node.getPreviousNodes().isEmpty()) {
            return 0;
        }
        int maxLength = 0;
        for (ModelNode parent : node.getPreviousNodes()) {
            int length = getLongestPath(parent);
            if (length > maxLength) {
                maxLength = length;
            }
        }
        return maxLength + 1;
    }
    public int getLongestPath(){
        int longestPath = 0;
        for(ModelNode node: nodes){
            int pathLength = getLongestPath(node);
            if(pathLength > longestPath) longestPath = pathLength;
        }
        return longestPath;
    }
    public List<ModelNode> getNodesByLevel(int level){
        List<ModelNode> levelNodes = new ArrayList<>();
        for(ModelNode node : nodes){
            if(AppModel.getLongestPath(node)==level) levelNodes.add(node);
        }
        return levelNodes;
    }
}
