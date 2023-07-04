package com.atsumeru.web.enums;

public enum PlotType {
    UNKNOWN(Integer.MAX_VALUE),
    MAIN_STORY(0),         // Основная история
    ALTERNATIVE_STORY(1),  // Альтернативная история
    PREQUEL(2),            // Предыстория/Приквел
    INTERQUEL(3),          // Интерквел
    SEQUEL(4),             // Продолжение/Сиквел
    THREEQUEL(5),          // Триквел
    QUADRIQUEL(6),         // Квадриквел
    MIDQUEL(7),            // Мидквел
    PARALLELQUEL(8),       // Параллелквел
    REQUEL(9),             // Риквел
    ADAPTATION(10),        // Адаптация
    SPIN_OFF(11),          // Ответвление от оригинала/Спин-офф
    CROSSOVER(12),         // Кроссовер
    COMMON_CHARACTER(13),  // Общий персонаж
    COLORED(14),           // Цветная версия
    OTHER(15);             // Прочее

    private final int order;

    PlotType(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
