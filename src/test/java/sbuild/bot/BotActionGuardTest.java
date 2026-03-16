package sbuild.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotActionGuardTest {

    @Test
    void startsRetryCounterWhenActionChanges() {
        BotActionGuard guard = new BotActionGuard(2, 0);
        Object actionA = new Object();
        Object actionB = new Object();

        guard.begin(actionA, 10);
        guard.onFailure(11);
        assertEquals(1, guard.retryCount());

        guard.begin(actionB, 12);
        assertEquals(0, guard.retryCount());
    }

    @Test
    void retriesUntilMaxThenFails() {
        BotActionGuard guard = new BotActionGuard(2, 0);
        Object action = new Object();
        guard.begin(action, 0);

        assertEquals(BotActionGuard.FailureDecision.RETRY, guard.onFailure(1));
        assertEquals(BotActionGuard.FailureDecision.RETRY, guard.onFailure(2));
        assertEquals(BotActionGuard.FailureDecision.FAIL, guard.onFailure(3));
        assertEquals(0, guard.retryCount());
    }

    @Test
    void timeoutDependsOnElapsedTicks() {
        BotActionGuard guard = new BotActionGuard(1, 0);
        Object action = new Object();
        guard.begin(action, 100);

        assertFalse(guard.isTimedOut(109, 10));
        assertTrue(guard.isTimedOut(111, 10));
    }
}
