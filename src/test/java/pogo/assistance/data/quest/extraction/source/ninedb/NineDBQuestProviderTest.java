package pogo.assistance.data.quest.extraction.source.ninedb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pogo.assistance.data.model.Quest;
import pogo.assistance.ui.console.ConsoleOutputUtils;

class NineDBQuestProviderTest {

    @Test
    void jsonDataToQuest_HappyCase_ReturnsExpected() {
        final Quest quest = NineDBQuestProvider.jsonDataToQuest(new JsonParser().parse(
                "{\n" +
                        "\"data_id\": \"2006\",\n" +
                        "\"name\": \"\\u30e2\\u30cb\\u30e5\\u30e1\\u30f3\\u30c8 \\u767d\\u77f3\\u99c5\\u306e\\u6b74\\u53f2\",\n" +
                        "\"lat\": \"43.055018\",\n" +
                        "\"lng\": \"141.414032\",\n" +
                        "\"good\": \"0\",\n" +
                        "\"bad\": \"0\",\n" +
                        "\"user_token\": \"@alice510\",\n" +
                        "\"num1\": \"1500\",\n" +
                        "\"num2\": \"4767\",\n" +
                        "\"created\": \"2018-12-15 14:41:23\",\n" +
                        "\"address\": \"\\u5317\\u6d77\\u9053\\u672d\\u5e4c\\u5e02\\u767d\\u77f3\\u533a\\u5e73\\u548c\\u901a\\uff08\\u5317\\uff09\\uff13\\u4e01\\u76ee\\u5317\\uff16\\u2212\\uff11 \\u767d\\u77f3\\u99c5\",\n" +
                        "\"task_name\": \"Make 3 Great Curveball Throws in a row\"\n" +
                        "}").getAsJsonObject());
        assertNotNull(quest);
        assertEquals("Make 3 Great Curveball Throws in a row", quest.getAction().getDescription());
        assertEquals("1500 Stardust", quest.getReward().getDescription());
        assertEquals(43.055018, quest.getLatitude());
        assertEquals(141.414032, quest.getLongitude());
        assertTrue(quest.getAbbreviation().isPresent());
        assertEquals("3G15", quest.getAbbreviation().get());
    }

    @Test
    void jsonDataToQuest_EmptyJsonData_Throws() {
        assertThrows(
                NullPointerException.class,
                () -> NineDBQuestProvider.jsonDataToQuest(new JsonParser().parse("{}").getAsJsonObject()));
    }

    @Disabled("This test makes real service calls. Enable if you want to test the real fetching mechanism.")
    @Test
    void getQuests_RealRequest_NoVerification() {
        final List<Quest> quests = new NineDBQuestProvider().getQuests();
        ConsoleOutputUtils.printAvailableQuestDetails(quests);
    }

}