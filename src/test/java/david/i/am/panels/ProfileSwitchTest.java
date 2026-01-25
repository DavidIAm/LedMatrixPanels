package david.i.am.panels;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProfileSwitchTest {

    @Autowired
    private PanelDelegator delegator;

    @Autowired
    private ProfileState profileState;

    @Test
    @SuppressWarnings({"java:S2925", "BusyWait"})
    void testProfileSwitchInSameRun() throws Exception {
        // Prepare the profile file to start with cpu
        String profileFile = System.getProperty("user.home") + "/.ledmatrix-profile";
        Path path = Paths.get(profileFile);
        Files.writeString(path, "cpu");

        // Wait for ProfileMonitor to sync (it runs every 1s)
        Thread.sleep(2000);

        // Assert initial state
        assertEquals("cpu", delegator.getActiveProfile(), "Delegator should initially be on cpu");
        assertEquals("cpu", profileState.getActiveProfile());
        
        try {
            // Trigger the switch
            Files.writeString(path, "shimmer");

            // Wait for the switch to happen (ProfileMonitor has 1s fixed rate)
            long start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 15000) { // 15s timeout
                if ("shimmer".equals(delegator.getActiveProfile())) {
                    break;
                }
                Thread.sleep(500);
            }

            assertEquals("shimmer", delegator.getActiveProfile(), "Delegator should have switched to shimmer");
            assertEquals("shimmer", profileState.getActiveProfile());
            
            // Re-trigger switch to ram
            Files.writeString(path, "ram");
            
            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 10000) { // 10s timeout
                if ("ram".equals(delegator.getActiveProfile())) {
                    break;
                }
                Thread.sleep(500);
            }
            
            assertEquals("ram", delegator.getActiveProfile(), "Delegator should have switched to ram");
            assertEquals("ram", profileState.getActiveProfile());

            // Trigger switch to dasblinkenlights
            Files.writeString(path, "dasblinkenlights");

            start = System.currentTimeMillis();
            while (System.currentTimeMillis() - start < 10000) { // 10s timeout
                if ("dasblinkenlights".equals(delegator.getActiveProfile())) {
                    break;
                }
                Thread.sleep(500);
            }

            assertEquals("dasblinkenlights", delegator.getActiveProfile(), "Delegator should have switched to dasblinkenlights");

        } finally {
            // Cleanup: set back to cpu
            Files.writeString(path, "cpu");
        }
    }
}
