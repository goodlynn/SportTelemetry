package com.sports.telemetry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TelemetryController {

    private static final String FILE_NAME = "telemetry_history.txt";

    // Вкладка Мониторинга
    @FXML private TextField nameField;
    @FXML private LineChart<Number, Number> hrChart;
    @FXML private Label hrLabel;
    @FXML private Label zoneLabel;
    @FXML private ProgressBar loadBar;
    @FXML private ProgressBar recoveryBar;
    @FXML private Label statusLabel;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;

    // Вкладка Результатов
    @FXML private TextField searchField;
    @FXML private TableView<SessionResult> historyTable;
    @FXML private TableColumn<SessionResult, String> colName;
    @FXML private TableColumn<SessionResult, String> colDuration;
    @FXML private TableColumn<SessionResult, Integer> colAvgHr;
    @FXML private TableColumn<SessionResult, Integer> colMaxHr;
    @FXML private TableColumn<SessionResult, String> colZone;

    // Новый график во второй вкладке для просмотра истории
    @FXML private LineChart<Number, Number> historyChart;

    private final TelemetryModel model = new TelemetryModel();
    private final Random random = new Random();

    private final ObservableList<SessionResult> tableData = FXCollections.observableArrayList();
    private FilteredList<SessionResult> filteredData;

    private TelemetryBackgroundService backgroundService;
    private int timeSeconds = 0;
    private int totalHR = 0;
    private int maxHR = 0;
    private double internalRecoveryReserve = 1.0;

    // Список для сбора точек текущей сессии
    private final List<Integer> currentSessionPoints = new ArrayList<>();
    private XYChart.Series<Number, Number> liveSeries;
    private XYChart.Series<Number, Number> historySeries;

    @FXML
    public void initialize() {
        // Инициализация живого графика
        liveSeries = new XYChart.Series<>();
        liveSeries.setName("Текущий пульс");
        hrChart.getData().add(liveSeries);

        // Инициализация исторического графика
        historySeries = new XYChart.Series<>();
        historySeries.setName("Архивный пульс сессии");
        historyChart.getData().add(historySeries);

        // Привязки данных
        loadBar.progressProperty().bind(model.workloadProperty());
        recoveryBar.progressProperty().bind(model.recoveryProperty());
        zoneLabel.textProperty().bind(model.currentZoneProperty());
        model.heartRateProperty().addListener((obs, old, newVal) -> hrLabel.setText(newVal + " BPM"));

        // Настройка таблицы
        colName.setCellValueFactory(new PropertyValueFactory<>("athleteName"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colAvgHr.setCellValueFactory(new PropertyValueFactory<>("avgHeartRate"));
        colMaxHr.setCellValueFactory(new PropertyValueFactory<>("maxHeartRate"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("finalZone"));

        loadHistoryFromFile();

        filteredData = new FilteredList<>(tableData, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(session -> {
                if (newValue == null || newValue.trim().isEmpty()) return true;
                return session.getAthleteName().toLowerCase().contains(newValue.toLowerCase().trim());
            });
        });
        historyTable.setItems(filteredData);

        // СЛУШАТЕЛЬ КЛИКА ПО ТАБЛИЦЕ: Воспроизведение старого графика при нажатии на строку!
        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showHistoricalChart(newSelection);
            }
        });

        // Фоновый сервис с улучшенной «живой» симуляцией нагрузок
        backgroundService = new TelemetryBackgroundService();
        backgroundService.setPeriod(Duration.seconds(1));
        backgroundService.setOnSucceeded(event -> {
            Integer hr = backgroundService.getValue();
            if (hr != null) {
                timeSeconds++;
                totalHR += hr;
                if (hr > maxHR) maxHR = hr;

                currentSessionPoints.add(hr); // Сохраняем точку в память

                model.setHeartRate(hr);
                model.updateZone();
                calculateMedicalMetrics(hr);

                // Выводим на живой график. Ограничение на удаление точек УБРАНО, чтобы график не исчезал!
                liveSeries.getData().add(new XYChart.Data<>(timeSeconds, hr));
            }
        });
        stopBtn.setDisable(true);
    }

    @FXML
    protected void onStartButtonClick() {
        if (nameField.getText().trim().isEmpty()) {
            statusLabel.setText("Ошибка: Введите ФИО спортсмена!");
            return;
        }
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        nameField.setDisable(true);
        statusLabel.setText("Идет запись: " + nameField.getText());

        timeSeconds = 0; totalHR = 0; maxHR = 0;
        internalRecoveryReserve = 1.0;
        model.setWorkload(0.0);
        model.setRecovery(1.0);

        currentSessionPoints.clear();
        liveSeries.getData().clear();
        backgroundService.restart();
    }

    @FXML
    protected void onStopButtonClick() {
        backgroundService.cancel();
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        nameField.setDisable(false);

        int avgHr = (timeSeconds > 0) ? totalHR / timeSeconds : 0;

        // Превращаем список точек пульса в единую строку через запятую для сохранения
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentSessionPoints.size(); i++) {
            sb.append(currentSessionPoints.get(i));
            if (i < currentSessionPoints.size() - 1) sb.append(",");
        }
        String pointsStr = sb.toString().isEmpty() ? "70" : sb.toString();

        SessionResult newResult = new SessionResult(
                nameField.getText().trim(),
                timeSeconds + " сек",
                avgHr, maxHR,
                model.getCurrentZone(),
                pointsStr
        );

        tableData.add(newResult);
        saveRecordToFile(newResult);
        statusLabel.setText("Запись завершена и сохранена.");
    }

    // Отрисовка сохраненного графика из истории
    private void showHistoricalChart(SessionResult result) {
        historySeries.getData().clear();
        historyChart.setTitle("График ЧСС сессии: " + result.getAthleteName());

        String[] points = result.getHrPoints().split(",");
        for (int i = 0; i < points.length; i++) {
            try {
                int hrValue = Integer.parseInt(points[i]);
                historySeries.getData().add(new XYChart.Data<>(i + 1, hrValue));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void calculateMedicalMetrics(int hr) {
        // Нагрузка: теперь реагирует быстрее и нагляднее
        double currentLoad = (double) (hr - 60) / (185 - 60);
        model.setWorkload(Math.max(0.0, Math.min(1.0, currentLoad)));

        // Резерв восстановления (динамическая шкала усталости)
        if (hr > 125) {
            // Если пульс высокий, шкала начнет заметно уменьшаться (имитация утомления)
            internalRecoveryReserve -= 0.015 * ((double) hr / 125.0);
        } else {
            // Если пульс низкий, силы восстанавливаются
            internalRecoveryReserve += 0.01;
        }
        internalRecoveryReserve = Math.max(0.0, Math.min(1.0, internalRecoveryReserve));
        model.setRecovery(internalRecoveryReserve);
    }

    private void saveRecordToFile(SessionResult result) {
        try (FileWriter fw = new FileWriter(FILE_NAME, StandardCharsets.UTF_8, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(result.getAthleteName() + ";" + result.getDuration() + ";" +
                    result.getAvgHeartRate() + ";" + result.getMaxHeartRate() + ";" +
                    result.getFinalZone() + ";" + result.getHrPoints());
        } catch (IOException e) {
            System.err.println("Ошибка записи файла: " + e.getMessage());
        }
    }

    private void loadHistoryFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(";");
                if (parts.length == 6) {
                    tableData.add(new SessionResult(parts[0], parts[1],
                            Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), parts[4], parts[5]));
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка чтения файла: " + e.getMessage());
        }
    }

    // УЛУЧШЕННЫЙ СИМУЛЯТОР: Специально гонит пульс вверх, чтобы показать работу шкал!
    private class TelemetryBackgroundService extends ScheduledService<Integer> {
        private int hr = 72;
        @Override protected Task<Integer> createTask() {
            return new Task<>() {
                @Override protected Integer call() {
                    // Создаем фазовую тренировку:
                    if (timeSeconds < 12) {
                        hr += random.nextInt(12) - 2; // Разминка: пульс уверенно идет вверх
                    } else if (timeSeconds < 28) {
                        hr += random.nextInt(15) - 3; // Интенсивный спринт: гоним пульс в критическую зону (>150 BPM)
                    } else {
                        hr -= random.nextInt(14) - 3; // Фаза заминки: пульс падает, резерв сил восстанавливается
                    }
                    return Math.max(60, Math.min(185, hr));
                }
            };
        }
    }
}