package pogo.assistance.data.quest;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.NonNull;
import pogo.assistance.data.model.Quest;
import pogo.assistance.data.quest.QuestProviderFactory.QuestMap;
import pogo.assistance.data.quest.persistence.QuestRWUtils;

public class QuestProviderPersistenceWrapper implements QuestProvider {

    private static final Duration DEFAULT_TTL = Duration.of(1, ChronoUnit.HOURS);

    private final Duration ttl;
    private final QuestProvider backingQuestProvider;

    public QuestProviderPersistenceWrapper(
            @Nullable final Duration ttl,
            @NonNull QuestProvider backingQuestProvider) {
        this.ttl = (ttl == null) ? DEFAULT_TTL : ttl;
        this.backingQuestProvider = backingQuestProvider;
    }

    @Nonnull
    @Override
    public QuestMap getMap() {
        return backingQuestProvider.getMap();
    }

    @Nonnull
    @Override
    public List<Quest> getQuests() {
        final Optional<Date> latestDataFetchTime = QuestRWUtils.getLatestDataFetchTime(getMap());
        final boolean latestFileIsWithinTtl = latestDataFetchTime
                .map(date -> Duration.between(date.toInstant(), new Date().toInstant()))
                .filter(duration -> duration.compareTo(ttl) <= 0)
                .isPresent();

        final List<Quest> latestQuests;
        if (latestFileIsWithinTtl) {
            latestQuests = QuestRWUtils.getLatestQuests(getMap());
            if (!latestQuests.isEmpty()) {
                return latestQuests;
            }
        }

        final List<Quest> fetchedQuests = backingQuestProvider.getQuests();
        if (!fetchedQuests.isEmpty()) {
            QuestRWUtils.writeQuests(fetchedQuests, getMap());
        }
        return fetchedQuests;
    }
}
