package pogo.assistance.data.quest;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.NonNull;
import pogo.assistance.data.model.Map;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.persistence.QuestRWUtils;

public class QuestProviderPersistenceWrapper implements QuestProvider {

    private static final Duration DEFAULT_TTL = Duration.of(1, ChronoUnit.HOURS);

    private final Duration ttl;
    private final QuestProvider backingQuestProvider;
    private final QuestRWUtils questRWUtils;

    public QuestProviderPersistenceWrapper(
            @Nullable final Duration ttl,
            @NonNull final QuestProvider backingQuestProvider,
            @NonNull final QuestRWUtils questRWUtils) {
        this.ttl = (ttl == null) ? DEFAULT_TTL : ttl;
        this.backingQuestProvider = backingQuestProvider;
        this.questRWUtils = questRWUtils;
    }

    @Nonnull
    @Override
    public List<Quest> getQuests(final Map map) {
        final Optional<Date> latestDataFetchTime = questRWUtils.getLatestDataFetchTime(map);
        final boolean latestFileIsWithinTtl = latestDataFetchTime
                .map(date -> Duration.between(date.toInstant(), new Date().toInstant()))
                .filter(duration -> duration.compareTo(ttl) <= 0)
                .isPresent();

        final List<Quest> latestQuests;
        if (latestFileIsWithinTtl) {
            latestQuests = questRWUtils.getLatestQuests(map);
            if (!latestQuests.isEmpty()) {
                return latestQuests;
            }
        }

        final List<Quest> fetchedQuests = backingQuestProvider.getQuests(map);
        if (!fetchedQuests.isEmpty()) {
            questRWUtils.writeQuests(fetchedQuests, map);
        }
        return fetchedQuests;
    }
}
