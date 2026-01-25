package david.i.am.panels;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProfileMonitor {

    private static final String PROFILE_FILE = System.getProperty("user.home") + "/.ledmatrix-profile";
    private static final Set<String> KNOWN_PROFILES = Set.of("cpu", "ram", "wifibattery", "shimmer", "dasblinkenlights");

    private final ProfileState profileState;
    private String lastProfile;

    public ProfileMonitor(ProfileState profileState) {
        this.profileState = profileState;
    }

    @PostConstruct
    public void init() {
        log.info("Checking for profile file at: {}", PROFILE_FILE);
        
        // Initial sync with the file
        Path path = Paths.get(PROFILE_FILE);
        String profileFromFile = null;
        if (Files.exists(path)) {
            log.info("Profile file found at: {}", path.toAbsolutePath());
            try {
                String content = Files.readString(path).trim().toLowerCase();
                log.info("Profile file content: '{}' (length: {})", content, content.length());
                if (content.isEmpty()) {
                    log.warn("Profile file is empty, ignoring.");
                    return;
                }
                if (KNOWN_PROFILES.contains(content)) {
                    profileFromFile = content;
                } else {
                    log.warn("Profile '{}' from file is not in KNOWN_PROFILES: {}", content, KNOWN_PROFILES);
                }
            } catch (IOException e) {
                log.error("Error reading profile file during init", e);
            }
        } else {
            log.info("Profile file NOT found at: {}", path.toAbsolutePath());
        }

        if (profileFromFile != null) {
            log.info("Initial profile from file: {}", profileFromFile);
            updateProfile(profileFromFile);
        } else {
            lastProfile = "cpu"; // Default fallback
            profileState.setActiveProfile(lastProfile);
        }

        log.info("ProfileMonitor initialized. Current lastProfile: {}", lastProfile);
    }

    @Scheduled(fixedRate = 1000)
    public void monitorProfileFile() {
        Path path = Paths.get(PROFILE_FILE);
        if (!Files.exists(path)) {
            log.trace("Profile file does not exist: {}", PROFILE_FILE);
            return;
        }

        try {
            String content = Files.readString(path).trim().toLowerCase();
            if (!content.equals(lastProfile)) {
                if (KNOWN_PROFILES.contains(content)) {
                    log.info("Profile change detected in {}: {} -> {}", PROFILE_FILE, lastProfile, content);
                    updateProfile(content);
                } else if (!content.isEmpty()) {
                    log.warn("Unknown profile detected in {}: {}", PROFILE_FILE, content);
                }
            }
        } catch (IOException e) {
            log.error("Error reading profile file: {}", PROFILE_FILE, e);
        }
    }

    private void updateProfile(String newProfile) {
        if (newProfile != null && newProfile.equals(lastProfile)) {
            return;
        }
        log.info("Applying new profile to state: {}", newProfile);
        lastProfile = newProfile;
        profileState.setActiveProfile(newProfile);
    }
}
