module diplom {

    requires javafx.controls;
    requires javafx.fxml;
    requires spring.context;
    requires spring.beans;
    requires com.dlsc.formsfx;

    opens com.simon.diplom to javafx.fxml, spring.core, spring.beans, spring.context, com.dlsc.formsfx;

    exports com.simon.diplom;
}