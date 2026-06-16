module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.sports.telemetry to javafx.fxml;
    exports com.sports.telemetry;
}