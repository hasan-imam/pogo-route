package pogo.assistance.ui.console.di;

import dagger.Component;
import javax.inject.Singleton;
import pogo.assistance.data.di.PersistenceModule;
import pogo.assistance.data.di.QuestModule;
import pogo.assistance.ui.console.ConsoleAppRunner;

@Singleton
@Component(modules = { PersistenceModule.class, QuestModule.class })
public interface ConsoleAppComponent {

    ConsoleAppRunner getConsoleAppRunner();

}
