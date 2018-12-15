package pogo.assistance.data.extraction.pokemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import pogo.assistance.data.model.Quest;
import pogo.assistance.util.FileIOUtils;

class PokemapQuestProviderTest {

    @Test
    void prepareQueryStringFromMetadata_HappyCase_ReturnsExpected() throws IOException {
        final String queryString = PokemapQuestProvider.prepareQueryStringFromMetadata(new String(
                Files.readAllBytes(FileIOUtils.resolvePackageLocalFilePath("metadata-query-output.json", PokemapQuestProviderTest.class)),
                StandardCharsets.UTF_8));
        assertEquals("quests%5B%5D=2%2C0%2C1&quests%5B%5D=2%2C0%2C2&quests%5B%5D=2%2C0%2C3" +
                "&quests%5B%5D=2%2C0%2C101&quests%5B%5D=2%2C0%2C102&quests%5B%5D=2%2C0%2C103&quests%5B%5D=2%2C0%2C201" +
                "&quests%5B%5D=2%2C0%2C202&quests%5B%5D=2%2C0%2C701&quests%5B%5D=2%2C0%2C703&quests%5B%5D=2%2C0%2C705" +
                "&quests%5B%5D=2%2C0%2C706&quests%5B%5D=2%2C0%2C708&quests%5B%5D=2%2C0%2C1301&quests%5B%5D=3%2C200%2C0" +
                "&quests%5B%5D=3%2C500%2C0&quests%5B%5D=3%2C1000%2C0&quests%5B%5D=3%2C1500%2C0&quests%5B%5D=7%2C0%2C1" +
                "&quests%5B%5D=7%2C0%2C4&quests%5B%5D=7%2C0%2C7&quests%5B%5D=7%2C0%2C37&quests%5B%5D=7%2C0%2C55" +
                "&quests%5B%5D=7%2C0%2C56&quests%5B%5D=7%2C0%2C60&quests%5B%5D=7%2C0%2C66&quests%5B%5D=7%2C0%2C92" +
                "&quests%5B%5D=7%2C0%2C95&quests%5B%5D=7%2C0%2C100&quests%5B%5D=7%2C0%2C102&quests%5B%5D=7%2C0%2C113" +
                "&quests%5B%5D=7%2C0%2C124&quests%5B%5D=7%2C0%2C125&quests%5B%5D=7%2C0%2C126&quests%5B%5D=7%2C0%2C129" +
                "&quests%5B%5D=7%2C0%2C133&quests%5B%5D=7%2C0%2C137&quests%5B%5D=7%2C0%2C147&quests%5B%5D=7%2C0%2C171" +
                "&quests%5B%5D=7%2C0%2C200&quests%5B%5D=7%2C0%2C246&quests%5B%5D=7%2C0%2C322&quests%5B%5D=7%2C0%2C327" +
                "&quests%5B%5D=7%2C0%2C349", queryString);
    }

    @Test
    void parseQuestsFromQuestData_HappyCase_ReturnsExpected() throws IOException {
        final List<Quest> quests = PokemapQuestProvider.parseQuestsFromQuestData(new String(
                Files.readAllBytes(FileIOUtils.resolvePackageLocalFilePath("quest-query-output.json", PokemapQuestProviderTest.class)),
                StandardCharsets.UTF_8));
        assertEquals(16557, quests.size());
    }

}