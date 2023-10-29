package redilog.utils;

import java.util.function.Consumer;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import redilog.init.Redilog;

public class LoggerUtil {

    public static void logWarnAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
    }

    public static void logErrorAndCreateMessage(Consumer<Text> feedback, String message) {
        Redilog.LOGGER.warn(message);
        feedback.accept(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }

    public static void logErrorAndCreateMessage(Consumer<Text> feedback, String logMessage, String chatMessage) {
        Redilog.LOGGER.error(logMessage);
        feedback.accept(Text.literal(chatMessage).setStyle(Style.EMPTY.withColor(Formatting.RED)));
    }

}
