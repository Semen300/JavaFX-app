package com.simon.diplom;

import com.dlsc.formsfx.model.structure.*;
import com.dlsc.formsfx.model.validators.DoubleRangeValidator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

@Component
public class MainController {
    @FXML
    private VBox pallet;
    @FXML
    private Pane workArea;
    @FXML
    private TextArea logArea;
    @FXML
    private VBox formArea;
    private Point2D lastMousePosition;
    private double scale = 1.0;
    private final double MAX_SCALE = 1.0;
    private final double MIN_SCALE = 0.3;
    private final List<Pane> selectedNodes = new ArrayList<>();
    private final List<Pane> selectedEvidence = new ArrayList<>();
    private final List<Pane> selectedNegativeEvidence = new ArrayList<>();
    private Line currentLine;
    private Line selectedLine;
    private File openedFile;
    private boolean isSaved;
    private Stage stage;
    public void setStage(Stage stage){
        this.stage = stage;
        this.stage.setOnCloseRequest(event->{
            if(!showConfirmation()) event.consume();
        });
    }
    private boolean isMoving = false;
    private boolean isSelecting = false;
    private boolean isConnecting = false;
    private AppModel model;
    @Autowired
    public void injectModel(AppModel model){
        this.model = model;
    }
    private Rectangle selectionRectangle;
    private double selectionStartX, selectionStartY;

    private void updateHeader(){
        String str = "Badiapp";
        if(openedFile!=null) str += " - " + openedFile.getName();
        else str += " - новый файл";
        if(!isSaved) str+="*";
        stage.setTitle(str);
    }
    @FXML
    protected void newFile(){
        if(showConfirmation()) {
            clearAll();
            openedFile = null;
            isSaved = true;
            updateHeader();
            log("Рабочая область очищена");
        }
    }
    @FXML
    protected void loadFile(){
        if(showConfirmation()) {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("CSV", "*.csv")
            );
            chooser.setTitle("Выберите файл для открытия");
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    clearAll();
                    List<String> content = Files.readAllLines(file.toPath(), Charset.forName("Windows-1251"));
                    List<ErrorReport> reports = findErrors(content);
                    if (reports.isEmpty()) {
                        isSaved = true;
                        openedFile = file;
                        updateHeader();
                        log("Файл " + file.getAbsolutePath() + " импортирован успешно");
                        int count = addNodesFromFile(content);
                        log("Из файла импортировано " + count + " узлов");
                        generatePanesByModel();
                        updatePrior();
                    } else {
                        log("Файл " + file.getAbsolutePath() + " содержит " + reports.size() + " ошибок:");
                        for (ErrorReport report : reports) {
                            log(report.toString());
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @FXML
    protected void saveFile(){
        if (model.countBad()==0) {
            if (openedFile != null) {
                if (!isSaved) {
                    try {
                        String content = generateFileFromModel();
                        Files.writeString(openedFile.toPath(), content, Charset.forName("Windows-1251"));
                        log("Модель сохранена в открытый файл");
                        isSaved = true;
                        updateHeader();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else saveToFile();
        }
        else log("В модели присутствуют " + model.countBad() + " ошибок. Исправьте их перед сохранением");
    }
    @FXML
    protected void saveToFile(){
        if(model.countBad()==0) {
            if (model.countObjects() != 0) {
                String content = generateFileFromModel();
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter(".scv", "*.csv")
                );
                File file = chooser.showSaveDialog(stage);
                if (file != null) {
                    try {
                        Files.writeString(file.toPath(), content, Charset.forName("Windows-1251"));
                        openedFile = file;
                        isSaved = true;
                        updateHeader();
                        log("Модель успешно сохранена в файл " + file.getAbsolutePath());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else log("Невозможно сохранить пустую модель");
        }
        else log("В модели присутствуют " + model.countBad() + " ошибок. Исправьте их перед сохранением");
    }
    @FXML
    protected void showInfo(){
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.setTitle("О программе");
        infoAlert.setHeaderText(null);
        Label contentText = new Label("Badiap (BAyesian DIagnostic APplication) \n- приложение для выявления причин неисправностей \nв киберфизических системах на основе байесовских сетей \n\nТекущая версия - 0.9");
        contentText.setWrapText(true);
        infoAlert.getDialogPane().setContent(contentText);
        infoAlert.showAndWait();
    }
    @FXML
    protected void showHelp(){
        Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
        infoAlert.initModality(Modality.NONE);
        infoAlert.setTitle("Помощь в работе с приложением");
        infoAlert.setHeaderText(null);
        StringBuilder content = new StringBuilder();
        content.append("Создание сети:\n");
        content.append("Для начала работы в приложении требуется создать байесовскую сеть, перетаскивая узлы из области палитры в рабочую область, или открыть файл, содержащий описание сети, с помощью соответствующего пункта меню 'Файл'\n");
        content.append("Затем требуеться соединить узлы между собой, образовав сеть. Для этого на каждом узле присутствуют 2 метки:\n");
        content.append("метка на левой стороне узла отвечает за все входящие от родительских узлов связи, правая - за связи, выходящие из текущего узла к дочерним.\n");
        content.append("Для создания всязи между двумя узлами нужно зажать ЛКМ на правой метке родительского узла и перетащить на левую метку дочернего узла.\n");
        content.append("После этого для каждого узла требуется задать имя, описание и таблицу условных вероятностей.\n");
        content.append("Окно редактирования узла можно открыть, нажав ЛКМ на соответствующем узле. Нажатие на узел с зажатой клавишей ctrl позволит выделить сразу несколько узлов; также присутствует возможность выделения рамкой.\n");
        content.append("Нажатие delete удалит все выбранные узлы и выбранную связь.\n");
        content.append("Также присутствует возможность масштабировать и перемещать рабочую область. Масштабирование осуществляется прокруткой колеса мыши, перемещение по рабочей области - перетаскиванием с зажатой ПКМ\n\n");

        content.append("Получение данных с помощью модели:\n");
        content.append("Данная модель позволяет получить 2 типа вероятности наступления каждого события: априорную и апостериорную.\n");
        content.append("Априорная вероятность - вероятность события до получения информации о свидетельствах. Априорная вероятность отвечает на вопрос вида 'Какова вероятность события A, если о наступлении других событий ничего не известно'\n");
        content.append("Апостериорная вероятность - вероятность события после получения информации о свидетельствах. Апостериорная вероятность отвечает на вопрос вида 'Какова вероятность события A, если получены свидетельства о событиях B и C'\n");
        content.append("Априорные вероятности отображаются после заполнения всех узлов, апостериорные - с вводом хотя бы одного свидетельства.\n\n");

        content.append("Ввод свидетельств:\n");
        content.append("Для выбора узла положительным свидетельством нужно один раз нажать ПКМ на узле, для выбора отрицательным - повторно нажать ПКМ на узле, выбранном положительным свидетельством.\n");
        content.append("Под положительным свидетельством понимается утверждение вида 'событие A гарантированно произошло', под отрицательным - 'событие A гарантированно не произошло'\n\n");

        content.append("Работа с файлами:\n");
        content.append("Пункт меню 'Файл->Новый' служит для создания новой модели байесовской сети\n");
        content.append("Пункт меню 'Файл->Открыть' позволяет открыть CSV-файл, содержащий описание модели, для редактирования в программе\n");
        content.append("Открытый файл отображается в заголовке окна после названия программы. Символ '*' после него означает присутствие несохранёных изменений.\n");
        content.append("С помощью пунктов меню 'Сохранить' и 'Сохранить как' позволяют сохранить текущую модель в открытый и новый файл соответственно\n");
        content.append("Для сохранения изменений в открытый файл можно также воспользоваться сочетанием клавиш ctrl+s\n\n");

        content.append("Значения цветов узлов в модели:\n");
        content.append("Желтый - узел выбран\n");
        content.append("Зелёный - узел выбран как положительное свидетельство\n");
        content.append("Красный - узел выбран как отрицательное свидетельство\n");
        content.append("Голубой - узел является симптомом выбранных узлов. Симптом - узел, не являющийся причиной ни для одного узла\n");
        content.append("Розовый - узел является первопричиной выбранных узлов. Первопричина - узел, не имеющий дополнительных причин.\n");
        content.append("Красная рамка - в узле присутсвуют ошибки, препятствующие работе системы\n");
        Label contentText = new Label(content.toString());

        contentText.setWrapText(true);
        infoAlert.getDialogPane().setContent(contentText);
        infoAlert.showAndWait();
    }
    @FXML
    public void initialize(){
        isSaved = true;
        openedFile = null;

        Label basicNode = new Label("Узел сети");
        basicNode.setStyle("-fx-background-color: #87CE40; -fx-padding: 10; -fx-start-margin: 5; -fx-border-color: black;");
        //Начало drag&drop
        basicNode.setOnDragDetected(event->{
            Dragboard db = basicNode.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("node");
            db.setContent(content);

            event.consume();
        });
        pallet.getChildren().add(basicNode);

        Label andNode = new Label("&");
        andNode.setStyle("-fx-background-color: #87CE40; -fx-padding: 10; -fx-start-margin: 5; -fx-border-color: black;");
        //Начало drag&drop
        andNode.setOnDragDetected(event->{
            Dragboard db = andNode.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("and");
            db.setContent(content);

            event.consume();
        });
        pallet.getChildren().add(andNode);

        Label orNode = new Label("V");
        orNode.setStyle("-fx-background-color: #87CE40; -fx-padding: 10; -fx-start-margin: 5; -fx-border-color: black;");
        //Начало drag&drop
        orNode.setOnDragDetected(event->{
            Dragboard db = orNode.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("or");
            db.setContent(content);

            event.consume();
        });
        pallet.getChildren().add(orNode);

        //Обработка drag&drop
        workArea.setOnDragOver(event->{
            if(isConnecting){
                event.acceptTransferModes(TransferMode.LINK);
                currentLine.setEndX(event.getX());
                currentLine.setEndY(event.getY());
            }
            else if(event.getGestureSource()!=workArea && event.getDragboard().hasString()){
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pallet.setOnDragOver(event-> {
            if (isConnecting) {
                event.acceptTransferModes(TransferMode.LINK);
            }
        });

        pallet.setOnDragDropped(event->{
            if(isConnecting){
                isConnecting=false;
                workArea.getChildren().remove(currentLine);
                currentLine = null;
                event.setDropCompleted(false);
            }
        });

        formArea.setOnDragOver(event-> {
            if (isConnecting) {
                event.acceptTransferModes(TransferMode.LINK);
            }
        });

        formArea.setOnDragDropped(event->{
            if(isConnecting){
                isConnecting=false;
                workArea.getChildren().remove(currentLine);
                currentLine = null;
                event.setDropCompleted(false);
            }
        });

        //Окончание drag&drop
        workArea.setOnDragDropped(event->{
            if(isConnecting){
                isConnecting=false;
                workArea.getChildren().remove(currentLine);
                currentLine = null;
                event.setDropCompleted(false);
            }
            else {
                Dragboard db = event.getDragboard();
                String type = db.getString();
                boolean success = false;
                switch (type) {
                    case "node" -> {
                        ModelNode node = new ModelNode(model.getLastID()+1, db.getString()+(model.getLastID()+1), NodeType.BASIC);
                        Pane draggedNode = createDraggableNode(node, event.getX()-60*scale, event.getY());
                        node.setNodePane(draggedNode);
                        model.addNode(node);
                        workArea.getChildren().add(draggedNode);
                        isSaved = false;
                        updateHeader();
                        success = true;
                    }
                    case "and" -> {
                        ModelNode node = new ModelNode(model.getLastID()+1, db.getString()+(model.getLastID()+1), NodeType.AND);
                        Pane draggedNode = createDraggableNode(node, event.getX()-10*scale, event.getY());
                        node.setNodePane(draggedNode);
                        model.addNode(node);
                        workArea.getChildren().add(draggedNode);
                        isSaved = false;
                        updateHeader();
                        success = true;
                    }
                    case "or" -> {
                        ModelNode node = new ModelNode(model.getLastID()+1, db.getString()+(model.getLastID()+1), NodeType.OR);
                        Pane draggedNode = createDraggableNode(node, event.getX()-10*scale, event.getY());
                        node.setNodePane(draggedNode);
                        model.addNode(node);
                        workArea.getChildren().add(draggedNode);
                        isSaved = false;
                        updateHeader();
                        success = true;
                    }
                }
                log("Добавлен узел. Общее число узлов - " + model.countObjects());
                updatePrior();
                unselectAllEvidence();
                event.setDropCompleted(success);
            }
            event.consume();
        });

        //начало растягивания рамки
        workArea.setOnMousePressed(event->{
            if(event.getButton() == MouseButton.PRIMARY) {
                if (event.getTarget() == workArea && event.isPrimaryButtonDown()) {
                    clearSelection();
                    isSelecting = true;

                    selectionStartX = event.getX();
                    selectionStartY = event.getY();

                    selectionRectangle = new Rectangle();
                    selectionRectangle.setX(selectionStartX);
                    selectionRectangle.setY(selectionStartY);

                    selectionRectangle.setFill(Color.web("blue", 0.2));
                    selectionRectangle.setStroke(Color.BLUE);

                    workArea.getChildren().add(selectionRectangle);
                    event.consume();
                }
            }
            else if(event.getButton() == MouseButton.SECONDARY){
                workArea.setCursor(Cursor.CLOSED_HAND);
                lastMousePosition = workArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            }
        });

        //Обработка растягивания рамки, создания связи и перемещения по рабочей области
        workArea.setOnMouseDragged(event ->{
            if(event.getButton()==MouseButton.PRIMARY) {
                if (isSelecting) {
                    double width = Math.abs(event.getX() - selectionStartX);
                    double height = Math.abs(event.getY() - selectionStartY);

                    selectionRectangle.setWidth(width);
                    selectionRectangle.setHeight(height);

                    selectionRectangle.setX(Math.min(selectionStartX, event.getX()));
                    selectionRectangle.setY(Math.min(selectionStartY, event.getY()));
                } else if (isConnecting) {
                    currentLine.setEndX(event.getX() - 0.5);
                    currentLine.setEndY(event.getY() - 0.5);
                }
            }
            else if(event.getButton() == MouseButton.SECONDARY){
                List<Pane> allPanes = workArea.getChildren().stream().filter(o-> o instanceof Pane).map(o-> (Pane) o).toList();

                Point2D currentMousePosition = workArea.sceneToLocal(event.getSceneX(), event.getSceneY());

                double deltaX = currentMousePosition.getX() - lastMousePosition.getX();
                double deltaY = currentMousePosition.getY() - lastMousePosition.getY();


                Bounds workAreaBounds = workArea.getLayoutBounds();
                Bounds groupBounds = computeGroupBounds(allPanes);


                double futureMinX = groupBounds.getMinX() + deltaX;
                double futureMaxX = groupBounds.getMaxX() + deltaX;
                double futureMinY = groupBounds.getMinY() + deltaY;
                double futureMaxY = groupBounds.getMaxY() + deltaY;

                if (futureMaxX < 0) deltaX = -groupBounds.getMaxX();
                if (futureMinX > workAreaBounds.getWidth()) deltaX = workAreaBounds.getWidth() - groupBounds.getMinX();

                if (futureMaxY < 0) deltaY = -groupBounds.getMaxY();
                if (futureMinY > workAreaBounds.getHeight()) deltaY = workAreaBounds.getHeight() - groupBounds.getMinY();

                for (Pane pane : allPanes) {
                    moveObjectByDelta(pane, deltaX, deltaY);
                }

                lastMousePosition = workArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            }
        });

        //Окночание растягивания рамки
        workArea.setOnMouseReleased(event->{
            if(event.getButton()==MouseButton.PRIMARY) {
                unselectLine();
                unselectAllEvidence();
                if (selectionRectangle != null) {
                    selectNodesInRectangle(selectionRectangle);
                    if (selectedNodes.size() == 1) {
                        hideForm();
                        Pane node = selectedNodes.getLast();
                        ModelNode node1 = ((ModelNodeGraphics) node.getUserData()).getNode();
                        if (node1.getType() == NodeType.BASIC) showForm(node1);
                        else showInfoForm(node1);
                    } else {
                        hideForm();
                    }
                    workArea.getChildren().remove(selectionRectangle);
                    selectionRectangle = null;
                    isSelecting = false;
                    event.consume();
                }
            }
            else if(event.getButton() == MouseButton.SECONDARY){
                workArea.setCursor(Cursor.DEFAULT);
            }
        });

        //Нажатие delete и ctrl+s
        workArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.setOnKeyPressed(event -> {
                    if (event.getCode() == KeyCode.DELETE) {
                        if(selectedLine!=null) {
                            deleteLine();
                        }
                        if(!selectedNodes.isEmpty()) {
                            deleteSelectedNodes();
                            hideForm();
                        }
                        isSaved = false;
                        updateHeader();
                        updatePrior();
                    }
                    else if(event.getCode() == KeyCode.S && event.isControlDown()){
                        saveFile();
                    }
                });
            }
        });

        //Передача фокуса
        formArea.setOnMouseClicked(event->{
            if(event.getTarget()==formArea){
                workArea.requestFocus();
            }
        });

        //Обработка масштабирования
        workArea.setOnScroll(event->{
            if (event.getDeltaY()>0 && scale<MAX_SCALE || event.getDeltaY()<=0 && scale>MIN_SCALE) {
                    scale+=event.getDeltaY()/3200;
                    List<Pane> panes = workArea.getChildren().stream().filter(o->o instanceof Pane).map(o->(Pane) o).toList();
                    for (Pane pane : panes){
                        if(pane instanceof NodePane){
                            ((NodePane)pane).resize(scale);
                            pane.setLayoutX(scale*((NodePane)pane).getBasicLayoutX());
                            pane.setLayoutY(scale*((NodePane)pane).getBasicLayoutY());
                            ModelNodeGraphics nodeGraphics = (ModelNodeGraphics) pane.getUserData();
                            for(Line inputLine : nodeGraphics.getInputLinks()){
                                inputLine.setEndX(scale*((NodePane)pane).getBasicLayoutX());
                                inputLine.setEndY(scale*((NodePane)pane).getBasicLayoutY() + pane.getHeight()/2);
                            }
                            for(Line outputLine : nodeGraphics.getOutputLinks()){
                                outputLine.setStartX(scale*((NodePane)pane).getBasicLayoutX()+pane.getWidth());
                                outputLine.setStartY(scale*((NodePane)pane).getBasicLayoutY()+pane.getHeight()/2);
                            }
                        }
                        else{
                            ((LogicalPane)pane).resize(scale);
                            pane.setLayoutX(scale*((LogicalPane)pane).getBasicLayoutX());
                            pane.setLayoutY(scale*((LogicalPane)pane).getBasicLayoutY());
                            ModelNodeGraphics nodeGraphics = (ModelNodeGraphics) pane.getUserData();
                            for(Line inputLine : nodeGraphics.getInputLinks()){
                                inputLine.setEndX(scale*((LogicalPane)pane).getBasicLayoutX());
                                inputLine.setEndY(scale*((LogicalPane)pane).getBasicLayoutY() + pane.getHeight()/2);
                            }
                            for(Line outputLine : nodeGraphics.getOutputLinks()){
                                outputLine.setStartX(scale*((LogicalPane)pane).getBasicLayoutX()+pane.getWidth());
                                outputLine.setStartY(scale*((LogicalPane)pane).getBasicLayoutY()+pane.getHeight()/2);
                            }
                        }
                    }
            }
        });
    }
    private Pane createDraggableNode(ModelNode node, double x, double y){
        Pane container;
        switch (node.getType()){
            case OR -> container = new LogicalPane(NodeType.OR, x, y, scale);
            case AND ->  container = new LogicalPane(NodeType.AND, x, y, scale);
            default ->  container = new NodePane(node.getID(), node.getName(), x, y, scale);
        }

        ModelNodeGraphics nodeGraphics = new ModelNodeGraphics(node, 0, 0);

        container.setUserData(nodeGraphics);

        //Добавление меток создания связей
        Circle bottomMarker = new Circle(3.5*scale);
        bottomMarker.setFill(Color.GRAY);
        bottomMarker.setStroke(Color.BLACK);
        bottomMarker.setStrokeWidth(1);
        bottomMarker.centerXProperty().bind(container.widthProperty());
        bottomMarker.centerYProperty().bind(container.heightProperty().divide(2));

        Circle upMarker = new Circle(3.5*scale);
        upMarker.setFill(Color.GRAY);
        upMarker.setStroke(Color.BLACK);
        upMarker.setStrokeWidth(1);
        upMarker.setMouseTransparent(false);
        upMarker.setVisible(true);
        upMarker.centerXProperty().set(0);
        upMarker.centerYProperty().bind(container.heightProperty().divide(2));

        container.getChildren().addAll(upMarker, bottomMarker);
        bottomMarker.setOnDragDetected(event -> {
            isConnecting = true;
            Dragboard db = bottomMarker.startDragAndDrop(TransferMode.LINK);
            ClipboardContent content = new ClipboardContent();

            content.putString("connect");
            db.setContent(content);
            LineGraphics lineGraphics = new LineGraphics((Pane) bottomMarker.getParent(), bottomMarker);

            currentLine = new Line();
            currentLine.setUserData(lineGraphics);
            currentLine.setStartX(container.getLayoutX()+bottomMarker.getCenterX());
            currentLine.setStartY(container.getLayoutY()+bottomMarker.getCenterY());
            workArea.getChildren().add(currentLine);

            event.consume();
        });

        upMarker.setOnDragOver(event -> {
            if(isConnecting) {
                event.acceptTransferModes(TransferMode.LINK);
                event.consume();
            }
        });

        upMarker.setOnDragDropped(event -> {
            if(isConnecting) {
                isConnecting = false;
                event.acceptTransferModes(TransferMode.LINK);
                LineGraphics lineData1 = (LineGraphics) currentLine.getUserData();
                ModelNodeGraphics startNodeGraphics = (ModelNodeGraphics) lineData1.getStartNode().getUserData();
                ModelNode startNode = startNodeGraphics.getNode();
                ModelNodeGraphics endNodeGraphics = (ModelNodeGraphics) upMarker.getParent().getUserData();
                ModelNode endNode = endNodeGraphics.getNode();
                if(startNode.getNextNodes().contains(endNode)){
                    log("Связь между узлами с id " + startNode.getID() + " и " + endNode.getID() + " уже существует");
                    workArea.getChildren().remove(currentLine);
                    currentLine = null;
                    event.setDropCompleted(false);
                }
                if(startNode.getAncestors().contains(endNode)){
                    log("Создание связи между узлами с id " + startNode.getID() + " и " + endNode.getID() + " приведёт к созданию цикла");
                    workArea.getChildren().remove(currentLine);
                    currentLine = null;
                    event.setDropCompleted(false);
                }
                else {
                    currentLine.setEndX(upMarker.getParent().getLayoutX()+upMarker.getCenterX());
                    currentLine.setEndY(upMarker.getParent().getLayoutY()+upMarker.getCenterY());
                    currentLine.toBack();
                    LineGraphics lineData = (LineGraphics) currentLine.getUserData();
                    lineData.setEndNode((Pane)upMarker.getParent());
                    lineData.setEndMarker(upMarker);

                    endNodeGraphics.addInputLink(currentLine);
                    startNodeGraphics.addOutputLink(currentLine);

                    currentLine.setOnMouseClicked(event1->{
                        unselectLine();
                        selectLine((Line) event1.getTarget());
                    });

                    endNode.addPrevious(startNode);
                    startNode.addNext(endNode);
                    startNode.changeColorForLNOnAddNext();
                    updatePrior();
                    unselectAllEvidence();

                    lineData.getStartNode().setUserData(startNodeGraphics);

                    log("Создана связь между объектами с id "+ startNode.getID() +" и " + endNode.getID());
                    log("\tНовое количество предков объекта с id " + endNode.getID() + ": " + endNode.countPrevious());
                    log("\tНовое количество потомков объекта с id " + startNode.getID() + ": " + startNode.countNext());
                    isSaved = false;
                    updateHeader();
                    event.setDropCompleted(true);
                }
                currentLine = null;
                event.consume();
            }
        });

        container.setLayoutX(x);
        container.setLayoutY(y);
        if(node.isGood()) container.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        else container.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");

        //Нажатие на объект
        container.setOnMousePressed(event -> {
            if (!isConnecting && !(event.getTarget() instanceof Circle)) {
                isMoving = false;
                lastMousePosition = workArea.sceneToLocal(event.getSceneX(), event.getSceneY());
            }
            if(event.getButton() == MouseButton.SECONDARY) event.consume();
        });

        //Обработка перетаскивания объекта
        container.setOnMouseDragged(event -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                if (!isConnecting && !(event.getTarget() instanceof Circle) && event.getButton() == MouseButton.PRIMARY) {
                    isMoving = true;

                    Point2D currentMousePosition = workArea.sceneToLocal(event.getSceneX(), event.getSceneY());

                    double deltaX = currentMousePosition.getX() - lastMousePosition.getX();
                    double deltaY = currentMousePosition.getY() - lastMousePosition.getY();

                    List<Pane> nodesToMove = selectedNodes.isEmpty() ? List.of(container) : selectedNodes;

                    Bounds workAreaBounds = workArea.getLayoutBounds();
                    Bounds groupBounds = computeGroupBounds(nodesToMove);


                    double futureMinX = groupBounds.getMinX() + deltaX;
                    double futureMaxX = groupBounds.getMaxX() + deltaX;
                    double futureMinY = groupBounds.getMinY() + deltaY;
                    double futureMaxY = groupBounds.getMaxY() + deltaY;

                    if (futureMaxX < 0) deltaX = -groupBounds.getMaxX();
                    if (futureMinX > workAreaBounds.getWidth())
                        deltaX = workAreaBounds.getWidth() - groupBounds.getMinX();

                    if (futureMaxY < 0) deltaY = -groupBounds.getMaxY();
                    if (futureMinY > workAreaBounds.getHeight())
                        deltaY = workAreaBounds.getHeight() - groupBounds.getMinY();

                    for (Pane pane : nodesToMove) {
                        moveObjectByDelta(pane, deltaX, deltaY);
                    }

                    lastMousePosition = new Point2D(
                            lastMousePosition.getX() + deltaX,
                            lastMousePosition.getY() + deltaY
                    );
                }
                event.consume();
            }
        });



        //Окончание нажатия на объект
        container.addEventHandler(MouseEvent.MOUSE_RELEASED, event->{
            unselectLine();
            if(!isMoving && !isConnecting) {
                if (event.getButton() == MouseButton.PRIMARY) {
                    unselectAllEvidence();
                    if (event.isControlDown()) {
                        if (selectedNodes.contains(container)) {
                            unselectNode(container);
                            if (selectedNodes.size() == 1) {
                                ModelNode node1 = ((ModelNodeGraphics) selectedNodes.getLast().getUserData()).getNode();
                                if (node1.getType() == NodeType.BASIC) showForm(node1);
                                else showInfoForm(node1);
                            }
                            if (selectedNodes.size() == 0) hideForm();
                        } else {
                            selectNode(container);
                            if (selectedNodes.size() == 1) {
                                ModelNode node1 = ((ModelNodeGraphics) container.getUserData()).getNode();
                                if (node1.getType() == NodeType.BASIC) showForm(node1);
                                else showInfoForm(node1);
                            } else {
                                hideForm();
                            }
                        }
                    } else {
                        clearSelection();
                        hideForm();
                        selectNode(container);
                        ModelNode node1 = ((ModelNodeGraphics) container.getUserData()).getNode();
                        if (node1.getType() == NodeType.BASIC) showForm(node1);
                        else showInfoForm(node1);
                    }
                }
                else if(event.getButton()==MouseButton.SECONDARY){
                    workArea.setCursor(Cursor.DEFAULT);
                    clearSelection();
                    if(selectedEvidence.contains(container)){
                        unselectNodeAsEvidence(container);
                        selectNodeAsNegEvidence(container);
                    }
                    else if(selectedNegativeEvidence.contains(container)){
                        unselectNodeAsNegativeEvidence(container);
                    }
                    else{
                        selectNodeAsEvidence(container);
                    }

                    for(Pane evi : selectedEvidence){
                        highlightAll(evi);
                    }
                }
            }
            event.consume();
        });
        return container;
    }
    private Bounds computeGroupBounds(List<Pane> nodes) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Pane pane : nodes) {
            double x = pane.getLayoutX();
            double y = pane.getLayoutY();
            double w = pane.getWidth();
            double h = pane.getHeight();

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x + w > maxX) maxX = x + w;
            if (y + h > maxY) maxY = y + h;
        }

        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }
    private void moveObjectByDelta(Pane pane, double deltaX, double deltaY) {
        double newX = pane.getLayoutX() + deltaX;
        double newY = pane.getLayoutY() + deltaY;

        pane.setLayoutX(newX);
        pane.setLayoutY(newY);
        if(pane instanceof NodePane){
            ((NodePane)pane).setBasicLayoutX(newX/scale);
            ((NodePane)pane).setBasicLayoutY(newY/scale);
        }
        else if(pane instanceof LogicalPane){
            ((LogicalPane)pane).setBasicLayoutX(newX/scale);
            ((LogicalPane)pane).setBasicLayoutY(newY/scale);
        }

        ModelNodeGraphics modelNodeGraphics = (ModelNodeGraphics) pane.getUserData();
        List<Line> inputLines = modelNodeGraphics.getInputLinks();
        List<Line> outputLines = modelNodeGraphics.getOutputLinks();

        if (inputLines != null) {
            for (Line inputLine : inputLines) {
                LineGraphics lineGraphics = (LineGraphics) inputLine.getUserData();
                inputLine.setEndX(newX + lineGraphics.getEndMarker().getCenterX());
                inputLine.setEndY(newY + lineGraphics.getEndMarker().getCenterY());
            }
        }

        if (outputLines != null) {
            for (Line outputLine : outputLines) {
                LineGraphics lineGraphics = (LineGraphics) outputLine.getUserData();
                outputLine.setStartX(newX + lineGraphics.getStartMarker().getCenterX());
                outputLine.setStartY(newY + lineGraphics.getStartMarker().getCenterY());
            }
        }
    }
    private void selectNode(Pane pane) {
        highlightAll(pane);
        ModelNode node = ((ModelNodeGraphics) pane.getUserData()).getNode();
        if(node.isGood()) pane.setStyle("-fx-background-color: #FFD700; -fx-border-color: black; -fx-border-width: 1");
        else pane.setStyle("-fx-background-color: #FFD700; -fx-border-color: red; -fx-border-width: 2");
        selectedNodes.add(pane);
    }
    private void unselectNode(Pane pane) {
        unhighlightAll(pane);
        ModelNode node = ((ModelNodeGraphics) pane.getUserData()).getNode();
        if(node.isGood()) pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        else pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
        selectedNodes.remove(pane);
    }
    private void clearSelection() {
        for(Pane pane: selectedNodes) unhighlightAll(pane);
        for (Pane pane : selectedNodes) {
            ModelNode node = ((ModelNodeGraphics) pane.getUserData()).getNode();
            if(node.isGood()) pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
            else pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
        }
        selectedNodes.clear();
    }
    private void selectLine(Line line){
        line.setStroke(Color.ORANGE);
        line.setStrokeWidth(2);
        selectedLine = line;
    }
    private void unselectLine(){
        if(selectedLine!=null) {
            selectedLine.setStroke(Color.BLACK);
            selectedLine.setStrokeWidth(1);
            selectedLine = null;
        }
    }
    private void selectNodeAsEvidence(Pane pane){
        Integer numberOfBad = model.countBad();
        if(numberOfBad==0){
            highlightAll(pane);
            selectedEvidence.add(pane);
            pane.setStyle("-fx-background-color: #33FF00; -fx-border-color: black; -fx-border-width: 1");
            updatePosterior();
        }
        else log("В данный момент в модели присутствует " + numberOfBad + " ошибок. Исправьте их перед выбором свидетельства.");
    }
    private void selectNodeAsNegEvidence(Pane pane){
        highlightAll(pane);
        selectedNegativeEvidence.add(pane);
        pane.setStyle("-fx-background-color: #CC0000; -fx-border-color: black; -fx-border-width: 1");
        updatePosterior();
    }
    private void unselectNodeAsEvidence(Pane pane){
        unhighlightAll(pane);
        pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        selectedEvidence.remove(pane);
        updatePosterior();
    }
    private void unselectNodeAsNegativeEvidence(Pane pane){
        unhighlightAll(pane);
        pane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        selectedNegativeEvidence.remove(pane);
        updatePosterior();
    }
    private void unselectAllEvidence(){
        for(Pane evidence : selectedEvidence){
            unhighlightAll(evidence);
            evidence.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        }
        for(Pane negEvidence : selectedNegativeEvidence){
            unhighlightAll(negEvidence);
            negEvidence.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
        }
        selectedEvidence.clear();
        selectedNegativeEvidence.clear();
        updatePosterior();
    }
    private void highlightAll(Pane pane){
        List<ModelNode> reasons = ((ModelNodeGraphics)pane.getUserData()).getNode().getReasons();
        for(ModelNode reason : reasons){
            Pane reasonPane = reason.getNodePane();
            if(!selectedNodes.contains(reasonPane) && (!selectedEvidence.contains(reasonPane)) && (!selectedNegativeEvidence.contains(reasonPane))) {
                if (reason.isGood())
                    reasonPane.setStyle("-fx-background-color: #FF9999; -fx-border-color: black; -fx-border-width: 1");
                else
                    reasonPane.setStyle("-fx-background-color: #FF9999; -fx-border-color: red; -fx-border-width: 2");
            }

        }
        List<ModelNode> consequences = ((ModelNodeGraphics)pane.getUserData()).getNode().getConsequences();
        for(ModelNode consequence : consequences){
            Pane consequencePane = consequence.getNodePane();
            if(!selectedNodes.contains(consequencePane) && (!selectedEvidence.contains(consequencePane)) && (!selectedNegativeEvidence.contains(consequencePane))) {
                if (consequence.isGood())
                    consequencePane.setStyle("-fx-background-color: #9999FF; -fx-border-color: black; -fx-border-width: 1");
                else
                    consequencePane.setStyle("-fx-background-color: #9999FF; -fx-border-color: red; -fx-border-width: 2");
            }
        }
    }
    private void unhighlightAll(Pane pane){
        List<ModelNode> reasons = ((ModelNodeGraphics)pane.getUserData()).getNode().getReasons();
        for(ModelNode reason : reasons){
            Pane reasonPane = reason.getNodePane();
            if(!selectedNodes.contains(reasonPane) && (!selectedEvidence.contains(reasonPane)) && (!selectedNegativeEvidence.contains(reasonPane))) {
                if (reason.isGood())
                    reasonPane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
                else
                    reasonPane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
            }
        }
        List<ModelNode> consequences = ((ModelNodeGraphics)pane.getUserData()).getNode().getConsequences();
        for(ModelNode consequence : consequences){
            Pane consequencePane = consequence.getNodePane();
            if(!selectedNodes.contains(consequencePane) && (!selectedEvidence.contains(consequencePane)) && (!selectedNegativeEvidence.contains(consequencePane))) {
                if (consequence.isGood())
                    consequencePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: black; -fx-border-width: 1");
                else
                    consequencePane.setStyle("-fx-background-color: #D3D3D3; -fx-border-color: red; -fx-border-width: 2");
            }
        }
    }
    private void selectNodesInRectangle(Rectangle rectangle){
        Bounds selectionBounds = rectangle.getBoundsInParent();
        for(Node node:workArea.getChildren()){
            if(node instanceof Pane){
                if(node.getBoundsInParent().intersects(selectionBounds)){
                    selectNode((Pane) node);
                }
            }
        }
    }
    private void deleteLine(){
        if(selectedLine!=null) {
            workArea.getChildren().remove(selectedLine);
            LineGraphics lineData = (LineGraphics) selectedLine.getUserData();

            ModelNodeGraphics startNodeData = (ModelNodeGraphics) lineData.getStartNode().getUserData();
            ModelNodeGraphics endNodeData = (ModelNodeGraphics) lineData.getEndNode().getUserData();
            startNodeData.getOutputLinks().remove(selectedLine);
            endNodeData.getInputLinks().remove(selectedLine);
            startNodeData.getNode().deleteNext(endNodeData.getNode());
            endNodeData.getNode().deletePrevious(startNodeData.getNode());
            selectedLine = null;
            log("Связь между узлами и id " + startNodeData.getNode().getID() + " и " + endNodeData.getNode().getID() + " удалена");
            log("\tНовое количество предков объекта с id " + endNodeData.getNode().getID() + ": " + endNodeData.getNode().countPrevious());
            log("\tНовое количество потомков объекта с id " + startNodeData.getNode().getID() + ": " + startNodeData.getNode().countNext());
        }
    }
    private void deleteSelectedNodes() {
        int numberOfNodes = 0;
        for (Pane node : selectedNodes) {
            ModelNodeGraphics currentNodeData = ((ModelNodeGraphics) node.getUserData());

            List<Line> inputLines = currentNodeData.getInputLinks();
            for(Line inputLine: inputLines){
                workArea.getChildren().remove(inputLine);
            }
            List<Line> outputLines = currentNodeData.getOutputLinks();
            for(Line outputLine: outputLines){
                workArea.getChildren().remove(outputLine);
            }

            unhighlightAll(node);
            workArea.getChildren().remove(node);

            List<ModelNode> prevNodes = currentNodeData.getNode().getPreviousNodes();
            for(ModelNode prevNode : prevNodes){
                prevNode.deleteNext(currentNodeData.getNode());
            }
            List<ModelNode> nextNodes = currentNodeData.getNode().getNextNodes();
            for(ModelNode nextNode:nextNodes){
                nextNode.deletePrevious(currentNodeData.getNode());

            }
            model.deleteNode(currentNodeData.getNode());
            numberOfNodes++;
        }
        log("Удалено "+numberOfNodes+" узлов. Общее число узлов - "+model.countObjects());
        selectedNodes.clear();
    }
    public void log(String message){
        logArea.appendText(message+"\n");
    }
    private void showForm(ModelNode node){
        IntegerField idField = Field.ofIntegerType(node.getID()).label("ID").editable(false);
        StringField nameField = Field.ofStringType(node.getName()).label("Название");
        StringField discField = Field.ofStringType(node.getDisc()).label("Описание").multiline(true);

        List<Field<?>> tableFields = new ArrayList<>();
        String title = MyMath.getStringFromBinary(node.getPreviousNodes().stream().map(ModelNode::getID).toList(), true);
        tableFields.add(Field.ofStringType("Вероятность:").label(title).editable(false));
        for(int i=0;i<node.getTableSize();i++){
            List<Integer> binary = MyMath.toBinary(i, node.countPrevious());
            String binaryString = MyMath.getStringFromBinary(binary, true);
            tableFields.add(Field.ofDoubleType(node.getPosProb(binary)).label(binaryString).validate(DoubleRangeValidator.between(0.0, 1.0, "Вероятность должна принадлежать к диапазону [0;1]")));
        }

        Form form = Form.of(
                Group.of(idField, nameField, discField),
                Group.of(tableFields.toArray(new Field[0]))
        );
        FormRenderer renderer = new FormRenderer(form);

        Platform.runLater(() ->
            renderer.lookupAll(".spinner").forEach(field -> {
                if (field instanceof Spinner<?> spinner) {
                    if (spinner.getValueFactory() instanceof SpinnerValueFactory.DoubleSpinnerValueFactory factory) {
                        factory.setAmountToStepBy(0.01);
                        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
                        factory.setConverter(new StringConverter<>() {
                            @Override
                            public String toString(Double value) {
                                return format.format(value);
                            }

                            @Override
                            public Double fromString(String string) {
                                try {
                                    return format.parse(string).doubleValue();
                                } catch (ParseException e) {
                                    return 0.0;
                                }
                            }
                        });
                    }
                }
            }));

        Button saveButton = new Button("Сохранить");
        saveButton.disableProperty().bind(form.validProperty().not());
        saveButton.setOnAction(event->{
            List<Double> toSave = new ArrayList<>();
            for(int i=1;i<tableFields.size();i++){
                DoubleField df = (DoubleField) tableFields.get(i);
                if(df.getValue().isNaN()){
                    toSave.add(0.0);
                }
                else{
                    toSave.add(df.getValue());
                }
            }
            ModelNode nodeToSave = model.getNodeByID(idField.getValue());
            nodeToSave.setName(nameField.getValue());
            ((NodePane) nodeToSave.getNodePane()).setName(nameField.getValue(), scale);
            nodeToSave.setDisc(discField.getValue());
            nodeToSave.setProbabilityTable(toSave);
            updatePrior();
            isSaved = false;
            updateHeader();
            log("Узел с id " + idField.getValue() + " изменён");
        });

        formArea.getChildren().addAll(renderer, saveButton);
    }
    private void showInfoForm(ModelNode node){
        IntegerField idField = Field.ofIntegerType(node.getID()).label("ID").editable(false);

        List<Field<?>> tableFields = new ArrayList<>();
        String title = MyMath.getStringFromBinary(node.getPreviousNodes().stream().map(ModelNode::getID).toList(), true);

        Label error = new Label();
        StringBuilder errorStr = new StringBuilder();
        boolean notEnoughParents = node.getPreviousNodes().size()<2;
        boolean notEnoughChildren = node.countNext()==0;
        if(notEnoughParents || notEnoughChildren){
            if(notEnoughParents){
                errorStr.append("Логический узел должен иметь как минимум 2 родителя!");
            }
            if(notEnoughChildren){
                errorStr.append("\nЛогический узел должен иметь как минимум 1 дочерний узел!");
            }
            error.setText(errorStr.toString());
        }
        else {
            tableFields.add(Field.ofStringType("Вероятность:").label(title).editable(false));
            for (int i = 0; i < node.getTableSize(); i++) {
                List<Integer> binary = MyMath.toBinary(i, node.countPrevious());
                String binaryString = MyMath.getStringFromBinary(binary, true);
                tableFields.add(Field.ofDoubleType(node.getPosProb(binary)).label(binaryString).editable(false));
            }
        }

        Form form;
        if(error.getText().isEmpty()) {
            form = Form.of(
                    Group.of(idField),
                    Group.of(tableFields.toArray(new Field[0]))
            );
        }
        else {
            form = Form.of(
                    Group.of(idField)
            );
        }
        FormRenderer renderer = new FormRenderer(form);
        formArea.getChildren().addAll(renderer, error);
    }
    private void hideForm(){
        formArea.getChildren().clear();
        workArea.requestFocus();
    }
    private void updatePrior(){
        if(model.countBad()==0){
            model.fillProbTable();
            for(ModelNode modelNode : model.getNodes()){
                if(modelNode.getType() == NodeType.BASIC) {
                    ((NodePane) modelNode.getNodePane()).setPrior(model.getProbByEvidence(null, null, modelNode));
                }
            }
        }
        else{
            for(ModelNode modelNode : model.getNodes()){
                if(modelNode.getType() == NodeType.BASIC) {
                    ((NodePane) modelNode.getNodePane()).setPrior(null);
                }
            }
        }
    }
    private void updatePosterior(){
        if(model.countBad()==0 && (!selectedEvidence.isEmpty() || !selectedNegativeEvidence.isEmpty())) {
                List<ModelNode> posNodes = selectedEvidence.stream().map(o -> ((ModelNodeGraphics) o.getUserData()).getNode()).toList();
                List<ModelNode> negNodes = selectedNegativeEvidence.stream().map(o -> ((ModelNodeGraphics) o.getUserData()).getNode()).toList();
                for (ModelNode modelNode : model.getNodes()) {
                    if (modelNode.getType() == NodeType.BASIC) {
                        NodePane pane = (NodePane) modelNode.getNodePane();
                        if(posNodes.contains(modelNode)){
                            pane.setPosterior(1.0);
                        }
                        else if(negNodes.contains(modelNode)){
                            pane.setPosterior(0.0);
                        }
                        else {
                            pane.setPosterior(model.getProbByEvidence(posNodes, negNodes, modelNode));
                        }
                    }
                }
        }
        else{
            for(ModelNode modelNode : model.getNodes()){
                if(modelNode.getType() == NodeType.BASIC) {
                    ((NodePane) modelNode.getNodePane()).setPosterior(null);
                }
            }
        }
    }
    private List<ErrorReport> findErrors(List<String> lines){
        List<Integer> listOfLN = new ArrayList<>();
        List<Integer> numberOfReferences = new ArrayList<>();
        List<ErrorReport> reports = new ArrayList<>();
        for(int lineNum = 0;lineNum < lines.size();lineNum++) {
            numberOfReferences.add(0);
            List<String> fields = new ArrayList<>(List.of(lines.get(lineNum).split(";")));
            if(fields.get(0).equals("")) reports.add(new ErrorReport(lineNum, 0, ErrorType.EMPTY_NAME));
            if(fields.get(0).equals("&") || fields.get(0).equals("v")) listOfLN.add(lineNum);
            int startPos = 1;
            if(!fields.get(0).equals("&") && !fields.get(0).equals("v") && fields.size()>1 && !MyMath.isInteger(fields.get(1)) && !MyMath.isDouble(fields.get(1)) && !fields.get(1).equals("")){
                startPos=2;
            }
            boolean inConnections = true, inProbabilities = false, firstProb = true;
            int numberOfSeparators = 0, numberOfConn = 0, numberOfProb = 0;
            for (int fieldNum = startPos; fieldNum < fields.size();fieldNum++) {
                if(fields.get(fieldNum).equals("")){
                    numberOfSeparators++;
                    inConnections = false;
                }
                else if((fields.get(fieldNum).contains(",") || !inConnections ) && firstProb){
                    if(numberOfSeparators==0) reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.NO_SEPARATOR));
                    firstProb=false;
                    inConnections = false;
                    inProbabilities = true;
                    fieldNum--;
                    continue;
                }

                if(inConnections){
                    numberOfConn++;
                    try {
                        int con = Integer.parseInt(fields.get(fieldNum));
                        if(con>=lineNum+1) reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.UNKNOWN_CONNECTION));
                        numberOfReferences.set(con-1, numberOfReferences.get(con-1)+1);
                    } catch (NumberFormatException e){
                        reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.TYPE_MISMATCH_LINK));
                    }
                }
                else if(inProbabilities) try{
                    numberOfProb++;
                    fields.set(fieldNum, fields.get(fieldNum).replace(',', '.'));
                    double prob = Double.parseDouble(fields.get(fieldNum));
                    if (prob>1) reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.TO_BIG_PROB));
                    else if(prob<0) reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.NEGATIVE_PROB));
                } catch (NumberFormatException e){
                    reports.add(new ErrorReport(lineNum, fieldNum, ErrorType.TYPE_MISMATCH_PROB));
                }
            }
            if ((fields.get(0).equals("&") || fields.get(0).equals("v")) && numberOfConn<2) reports.add(new ErrorReport(lineNum, null, ErrorType.NOT_ENOUGH_PARENTS_LN));
            if ((fields.get(0).equals("&") || fields.get(0).equals("v")) && numberOfProb>0) reports.add(new ErrorReport(lineNum, null, ErrorType.LN_HAS_PROB_TABLE));
            else if(!(fields.get(0).equals("&") || fields.get(0).equals("v")) && numberOfProb!=(int)Math.pow(2, numberOfConn)) reports.add(new ErrorReport(lineNum, null, ErrorType.PROB_NUM_MISMATCH));
        }
        for(Integer logicalNode : listOfLN){
            if(numberOfReferences.get(logicalNode)==0) reports.add(new ErrorReport(logicalNode, null, ErrorType.NOT_ENOUGH_CHILDREN_LN));
        }
        return reports;
    }
    private int addNodesFromFile(List<String> content){
        int addedNodes = 0;
        for(int lineNum = 0; lineNum < content.size(); lineNum++){
            List<String> fields = new ArrayList<>(List.of(content.get(lineNum).split(";")));
            Integer ID = lineNum+1;
            String name = fields.get(0);
            String disc = "";
            NodeType type;
            List<Double> probTable = new ArrayList<>();
            int startPos = 1;
            boolean inConnections = true;
            if(!fields.get(0).equals("&") && !fields.get(0).equals("v") && fields.size()>1 && !MyMath.isInteger(fields.get(1)) && !MyMath.isDouble(fields.get(1)) && !fields.get(1).equals("")){
                startPos=2;
                disc = fields.get(1);
            }
            switch (fields.get(0)){
                case "&" -> type = NodeType.AND;
                case "v" -> type = NodeType.OR;
                default -> type = NodeType.BASIC;
            }

            List<ModelNode> parents = new ArrayList<>();
            for(int fieldNum = startPos; fieldNum < fields.size(); fieldNum++){
                if(fields.get(fieldNum).equals("")) inConnections = false;
                else if(inConnections){
                    int parentID = Integer.parseInt(fields.get(fieldNum));
                    parents.add(model.getNodeByID(parentID));
                }
                else{
                    probTable.add(Double.parseDouble(fields.get(fieldNum).replace(',', '.')));
                }
            }
            ModelNode node = new ModelNode(ID, name, disc, parents, probTable, type);
            for(ModelNode parent : parents) parent.addNext(node);
            model.addNode(node);
            addedNodes++;
        }
        return addedNodes;
    }
    private void generatePanesByModel(){
        List<List<ModelNode>> superList = new ArrayList<>();
        int longestPath = model.getLongestPath();
        for(int i=0;i<=longestPath;i++){
            superList.add(new ArrayList<>());
        }
        for(ModelNode node: model.getNodes()){
            int pathLength = AppModel.getLongestPath(node);
            superList.get(pathLength).add(node);
        }
        double currentX = 10.0*scale;
        for(List<ModelNode> line : superList){
            double longest = 0.0, currentY = (superList.indexOf(line)%2==0) ? 10.0*scale : 15.0*scale;
            for(ModelNode node: line){
                Pane pane = createDraggableNode(node, currentX, currentY);
                node.setNodePane(pane);
                currentY+=pane.getPrefHeight()+30*scale;
                if(longest<pane.getPrefWidth()) longest=pane.getPrefWidth();
                workArea.getChildren().add(pane);
            }
            currentX+=longest+50*scale;
        }
        for(ModelNode node: model.getNodes()){
            Pane endPane = node.getNodePane();
            Circle endMarker = endPane.getChildren().stream().filter(o -> o instanceof Circle).map(o -> (Circle)o).toList().get(0);
            for(ModelNode node1: node.getPreviousNodes()){
                Pane startPane = node1.getNodePane();
                Circle startMarker = startPane.getChildren().stream().filter(o -> o instanceof Circle).map(o -> (Circle)o).toList().get(1);
                Line line = new Line(startPane.getLayoutX() + startPane.getPrefWidth(), startPane.getLayoutY()+startPane.getPrefHeight()/2, endPane.getLayoutX(), endPane.getLayoutY()+endPane.getPrefHeight()/2);
                line.setUserData(new LineGraphics(startPane, endPane, startMarker, endMarker));
                ((ModelNodeGraphics)endPane.getUserData()).addInputLink(line);
                ((ModelNodeGraphics)startPane.getUserData()).addOutputLink(line);
                line.setOnMouseClicked(event1->{
                    unselectLine();
                    selectLine((Line) event1.getTarget());
                });
                workArea.getChildren().add(line);
                line.toBack();
            }
        }
    }
    private String generateFileFromModel(){
        int lastID = 1;
        int maxLevel = model.getLongestPath();
        for(int level = 0; level <= maxLevel; level++){
            List<ModelNode> levelNodes = model.getNodesByLevel(level);
            for(ModelNode node : levelNodes){
                node.setID(lastID);
                lastID++;
            }
        }
        List<ModelNode> sortedNodes = model.getNodes();
        sortedNodes.sort(Comparator.comparingInt(ModelNode::getID));
        StringBuilder str = new StringBuilder();
        for(ModelNode node : sortedNodes){
            str.append(getStringFromNode(node)).append("\n");
        }
        return str.toString();
    }
    private String getStringFromNode(ModelNode node){
        StringBuilder str = new StringBuilder();
        switch (node.getType()){
            case OR -> str.append("v").append(";");
            case AND -> str.append("&").append(";");
            case BASIC -> str.append(node.getName()).append(";");
        }

        if(!node.getDisc().equals("")) str.append(node.getDisc()).append(";");
        for(ModelNode prevNode : node.getPreviousNodes()){
            str.append(prevNode.getID()).append(";");
        }
        if(node.getType()==NodeType.BASIC) {
            str.append(";");
            for (Double prob : node.getProbabilityTable()) {
                String replaced = String.valueOf(prob).replace('.', ',');
                str.append(replaced).append(";");
            }
        }
        return str.substring(0, str.length()-1);
    }
    private void clearAll(){
        model.clear();
        workArea.getChildren().clear();
        selectedNodes.clear();
        selectedEvidence.clear();
        selectedNegativeEvidence.clear();
        currentLine = null;
        selectedLine = null;
        hideForm();
    }
    private boolean showConfirmation() {
        if (!isSaved && model.countObjects()!=0) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText(null);
            alert.setTitle("Подтверждение");
            alert.setContentText("Обнаружены несохранённые изменения.\nВы уверены, что хотите продолжить?");
            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        } else return true;
    }
}