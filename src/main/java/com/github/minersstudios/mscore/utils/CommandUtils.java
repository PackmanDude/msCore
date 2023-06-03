package com.github.minersstudios.mscore.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public final class CommandUtils {

	@Contract(value = " -> fail")
	private CommandUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Gets time suggestions from number
	 * <p>
	 * Used for command tab completer
	 *
	 * @param input number of time
	 * @return time suggestions
	 */
	public static @NotNull List<String> getTimeSuggestions(@NotNull String input) {
		List<String> suggestions = new ArrayList<>();
		if (!input.matches("\\d+")) return suggestions;
		suggestions.add(input + "s");
		suggestions.add(input + "m");
		suggestions.add(input + "h");
		suggestions.add(input + "d");
		suggestions.add(input + "M");
		suggestions.add(input + "y");
		return suggestions;
	}

	/**
	 * Gets a date with time added
	 * <p>
	 * Regex : \d+[smhdMy]
	 *
	 * @param string time
	 * @return date with time added
	 */
	public static @Nullable Date getDateFromString(@NotNull String string) throws NumberFormatException, DateTimeException, ArithmeticException {
		if (!string.matches("\\d+[smhdMy]")) return null;
		String chronoUnit = string.replaceAll("\\d+", "");
		Instant instant = Instant.now();
		try {
			long number = Long.parseLong(string.replaceAll("[smhdMy]", ""));
			return Date.from(
					switch (chronoUnit) {
						case "s" -> instant.plus(number, ChronoUnit.SECONDS);
						case "m" -> instant.plus(number, ChronoUnit.MINUTES);
						case "h" -> instant.plus(number, ChronoUnit.HOURS);
						case "M" -> instant.plus(Math.multiplyExact(number, ChronoUnit.MONTHS.getDuration().toDays()), ChronoUnit.DAYS);
						case "y" -> instant.plus(Math.multiplyExact(number, ChronoUnit.YEARS.getDuration().toDays()), ChronoUnit.DAYS);
						default -> instant.plus(number, ChronoUnit.DAYS);
					}
			);
		} catch (DateTimeException | NumberFormatException | ArithmeticException ignored) {
			return null;
		}
	}
}
