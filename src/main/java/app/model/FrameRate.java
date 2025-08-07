package app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
public enum FrameRate {
    FPS_23_976("23.976", new BigDecimal("23.976"), "filmy kinowe"),
    FPS_24("24", new BigDecimal("24.000"), "filmy kinowe"),
    FPS_25("25", new BigDecimal("25.000"), "telewizja europejska"),
    FPS_29_97("29.97", new BigDecimal("29.970"), "telewizja amerykańska"),
    FPS_30("30", new BigDecimal("30.000"), "telewizja amerykańska"),
    FPS_50("50", new BigDecimal("50.000"), "telewizja HD europejska"),
    FPS_59_94("59.94", new BigDecimal("59.940"), "telewizja HD amerykańska"),
    FPS_60("60", new BigDecimal("60.000"), "gry, sport");

    private final String name;

    @Getter
    private final BigDecimal preciseValue;

    private final String description;

    public String getNameWithDescription() {
        return "%s (%s)".formatted(name, description);
    }

    public String getNameWithFpsSuffix() {
        return "%s fps".formatted(name);
    }

    @Override
    public String toString() {
        return getNameWithDescription();
    }

    public static BigDecimal getPreciseConversionRatio(FrameRate from, FrameRate to) {
        return from.getPreciseValue().divide(to.getPreciseValue(), 10, RoundingMode.HALF_UP);
    }
}
