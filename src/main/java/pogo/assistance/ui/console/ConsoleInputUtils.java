package pogo.assistance.ui.console;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * Handles taking various console inputs from user.
 * TODO: See if we can find a library that takes away the heavy lifting. Best if we don't need this class at all!
 *
 * @apiNote
 *      Note: Many of the transformer functions passed to these methods are not side-effect free. Callers should at
 *      least be careful to make those transformers idempotent since we make no guarantee on how many times they will be
 *      executed throughout user interactions.
 */
@UtilityClass
class ConsoleInputUtils {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static boolean promptBoolean(@NonNull final String prompt) {
        System.out.println(prompt);
        return readInput(input -> {
            if (input.equalsIgnoreCase("y")) {
                return true;
            } else if (input.equalsIgnoreCase("n")) {
                return false;
            } else {
                return null;
            }
        });
    }

    public static <T> T promptAndSelectOne(
            @NonNull final String prompt,
            @NotNull final List<T> options,
            @NotNull final Function<T, String> optionToString) {
        return promptAndSelect(prompt, options, optionToString).get(0);
    }

    public static <T> List<T> promptAndSelect(
            @NonNull final String prompt,
            @NotNull final List<T> options,
            @NotNull final Function<T, String> optionToString) {

        Preconditions.checkArgument(!options.isEmpty());

        System.out.println(prompt);
        for (int i = 1; i <= options.size(); i++) {
            System.out.println(String.format("%5s. %s", i, optionToString.apply(options.get(i - 1))));
        }

        return readInts(1, options.size()).stream()
                .map(i -> options.get(i - 1))
                .collect(Collectors.toList());
    }

    public static <T> T readStringToObject(
            @NonNull final String prompt,
            @NotNull final Function<String, T> inputTransformer) {
        System.out.println(prompt);
        return readInput(inputTransformer);
    }

    private static List<Integer> readInts(final int rangeStart, final int rangeEnd) {
        return readInput(input -> {
            final StringTokenizer stringTokenizer = new StringTokenizer(input, ", ");
            final List<Integer> temp = new ArrayList<>();
            while (stringTokenizer.hasMoreTokens()) {
                final int i = Integer.parseInt(stringTokenizer.nextToken());
                if (i < rangeStart || i > rangeEnd) {
                    System.out.println("Invalid input! Try again.");
                    continue;
                }
                temp.add(i);
            }

            return temp.isEmpty() ? null : temp;
        });
    }

    /**
     * @param inputTransformer
     *      Function to transform the input read by scanner into the desired object. Transformer should apply the
     *      necessary validation and return null in case of any failure. Transformer returning null causes the scanner
     *      to read again.
     * @return
     *      Scanner read input, transformed using {@code inputTransformer}. Keeps attempting to read until a non-null
     *      value is returned from {@code inputTransformer}.
     */
    private static <T> T readInput(final Function<String, T> inputTransformer) {
        while (true) {
            try {
                final T transformedInput = inputTransformer.apply(SCANNER.nextLine());
                if (transformedInput == null) {
                    System.out.println("Invalid input! Try again.");
                } else {
                    return transformedInput;
                }
            } catch (final RuntimeException e) {
                System.out.println("Invalid input! Try again.");
            }
        }
    }
}
