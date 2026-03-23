package com.simon.diplom;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;
import java.util.Objects;

public class MainApplication extends Application {
    private ConfigurableApplicationContext springContext;

    @Override
    public void init(){
        springContext = new AnnotationConfigApplicationContext(AppConfig.class);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("views/main-view.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);
        Scene scene = new Scene(fxmlLoader.load(), 920, 740);
        stage.setTitle("Badiap - новый файл");
        stage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResource("/com/simon/diplom/icons/icon.png")).toExternalForm()));
        stage.setScene(scene);
        stage.show();
        MainController controller = fxmlLoader.getController();
        controller.setStage(stage);
    }

    @Override
    public void stop(){
        springContext.close();
    }

    public static void main(String[] args) {
        launch();
    }
}