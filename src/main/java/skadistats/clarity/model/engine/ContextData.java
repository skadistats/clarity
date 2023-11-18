package skadistats.clarity.model.engine;

public class ContextData {

    private int buildNumber = -1;
    private int gameVersion = -1;
    private float millisPerTick = Float.NaN;

    public int getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }

    public int getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(int gameVersion) {
        this.gameVersion = gameVersion;
    }

    public float getMillisPerTick() {
        return millisPerTick;
    }

    public void setMillisPerTick(float millisPerTick) {
        this.millisPerTick = millisPerTick;
    }
}
