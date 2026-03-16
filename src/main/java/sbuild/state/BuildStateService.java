package sbuild.state;

/**
 * Tracks current build session state shared across modules.
 */
public final class BuildStateService {
    private boolean activeBuild;

    public void initialize() {
        activeBuild = false;
    }

    public boolean hasActiveBuild() {
        return activeBuild;
    }
}
