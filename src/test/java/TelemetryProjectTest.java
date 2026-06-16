package com.sports.telemetry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import com.sports.telemetry.TelemetryModel;
import com.sports.telemetry.SessionResult;

public class TelemetryProjectTest {

    private TelemetryModel telemetryModel;
    private SessionResult sessionResult;

    @BeforeEach
    public void setUp() {
        // Инициализируем модель
        telemetryModel = new TelemetryModel();

        // Вызываем конструктор SessionResult с 6 параметрами, которые он требует (передаем тестовые заглушки)
        sessionResult = new SessionResult("Тестовый Пилот", "Сессия 1", 0, 0, "Зона 1", "0:0");
    }

    @Test
    public void testModelInitialization() {
        // Тест 1: Проверяем, что объект модели успешно создается и инициализируется
        assertNotNull(telemetryModel, "Объект TelemetryModel должен быть успешно создан");
    }

    @Test
    public void testSessionResultCreation() {
        // Тест 2: Проверяем, что объект архивной записи успешно создался через конструктор
        assertNotNull(sessionResult, "Объект SessionResult должен успешно создаваться с заданными параметрами");
        assertEquals("Тестовый Пилот", sessionResult.getAthleteName(), "Имя спортсмена должно совпадать с переданным в конструктор");
    }

    @Test
    public void testAthleteNameIsNotEmpty() {
        // Тест 3: Дополнительная проверка бизнес-логики на валидность данных в объекте
        assertFalse(sessionResult.getAthleteName().trim().isEmpty(), "Имя спортсмена в сессии не должно быть пустым");
    }
}