package pogo.assistance.ui.discord;

import static org.junit.jupiter.api.Assertions.*;

import javax.security.auth.login.LoginException;
import org.junit.jupiter.api.Test;

class PostMessageTest {

    @Test
    void send() throws LoginException {
        new PostMessage().send("Hello world!");
    }
}