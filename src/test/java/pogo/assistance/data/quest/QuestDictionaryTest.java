package pogo.assistance.data.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import pogo.assistance.data.model.Task;

class QuestDictionaryTest {

    @Test
    void fromActionDescriptionToAbbreviation_HappyCase_ReturnsExpected() {
        final Optional<String> result = QuestDictionary.fromActionDescriptionToAbbreviation(
                "Make 3 Great Curveball Throws in a row",
                "1500 Stardust");
        // Maybe someday hamcrest will support optionals... [https://github.com/hamcrest/JavaHamcrest/issues/82]
        assertTrue(result.isPresent());
        assertEquals("3G15", result.get());
    }

    @Test
    void fromActionDescriptionToAbbreviation_Unknown_ReturnsEmpty() {
        final Optional<String> result = QuestDictionary.fromActionDescriptionToAbbreviation(
                "Make 3 Great Curveball Throws in a row",
                "Some random string that is not a reward description");
        assertFalse(result.isPresent());
    }

    @Test
    void lookupByAbbreviation_HappyCase_ReturnsExpected() {
        final Optional<Task> result = QuestDictionary.lookupByAbbreviation("3G15");
        assertTrue(result.isPresent());
        result.ifPresent(task -> {
            assertEquals("Make 3 Great Curveball Throws in a row", task.getAction().getDescription());
            assertEquals("1500 Stardust", task.getReward().getDescription());
        });
    }

    @Test
    void lookupByAbbreviation_Unknown_ReturnsEmpty() {
        assertFalse(QuestDictionary.lookupByAbbreviation("¯\\_(ツ)_/¯").isPresent());
    }

}