package hudson.plugins.bap_publisher;

import hudson.FilePath;
import hudson.Util;
import hudson.model.TaskListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Map;

public class BPBuildInfo implements Serializable {

    static final long serialVersionUID = 1L;
    public static final String PROMOTION_ENV_VARS_PREFIX = "promotion_";
    private static final String ENV_JOB_NAME = "JOB_NAME";
    private static final String ENV_BUILD_NUMBER = "BUILD_NUMBER";
    
    private Map<String, String> envVars;
    private FilePath baseDirectory;
    private Calendar buildTime;
    private TaskListener listener;
    private boolean verbose;
    private String consoleMsgPrefix;

    public BPBuildInfo() {}

    public BPBuildInfo(Map<String, String> envVars, FilePath baseDirectory, Calendar buildTime, TaskListener listener, String consoleMsgPrefix) {
        this.envVars = envVars;
        this.baseDirectory = baseDirectory;
        this.buildTime = buildTime;
        this.listener = listener;
        this.consoleMsgPrefix = consoleMsgPrefix;
    }

    public void setEnvVars(Map<String, String> envVars) { this.envVars = envVars; }
    public Map<String, String> getEnvVars() { return envVars; }

    public FilePath getBaseDirectory() { return baseDirectory; }
    public void setBaseDirectory(FilePath baseDirectory) { this.baseDirectory = baseDirectory; }

    public Calendar getBuildTime() { return buildTime; }
    public void setBuildTime(Calendar buildTime) { this.buildTime = buildTime; }

    public TaskListener getListener() { return listener; }
    public void setListener(TaskListener listener) { this.listener = listener; }

    public String getConsoleMsgPrefix() { return consoleMsgPrefix; }
    public void setConsoleMsgPrefix(String consoleMsgPrefix) { this.consoleMsgPrefix = consoleMsgPrefix; }

    public boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    public String getNormalizedBaseDirectory() {
        try {
            return baseDirectory.toURI().normalize().getPath();
        } catch (Exception e) {
            throw new RuntimeException(Messages.exception_normalizeDirectory(baseDirectory), e);
        }
    }

    public String getRelativePath(FilePath filePath, String removePrefix) throws IOException, InterruptedException {
        String normalizedPathToFile = filePath.toURI().normalize().getPath();
        String relativePathToFile = normalizedPathToFile.replace(getNormalizedBaseDirectory(), "");
        if ((removePrefix != null) && !"".equals(removePrefix.trim())) {
            String expanded = Util.replaceMacro(removePrefix, envVars);
            String toRemove = FilenameUtils.separatorsToUnix(FilenameUtils.normalize(expanded + "/"));
            if ((toRemove != null) && (toRemove.startsWith("/")))
                toRemove = toRemove.substring(1);
            if (!relativePathToFile.startsWith(toRemove)) {
                throw new BapPublisherException(Messages.exception_removePrefix_noMatch(relativePathToFile, toRemove));
            }
            relativePathToFile = relativePathToFile.substring(toRemove.length());
        }
        int lastDirIdx = relativePathToFile.lastIndexOf("/");
        if (lastDirIdx == -1)
            return "";
        else
            return relativePathToFile.substring(0, lastDirIdx);
    }
    
    public void println(String message) {
        if (listener != null) {
            listener.getLogger().println(consoleMsgPrefix + message);
        }
    }

    public void printIfVerbose(String message) {
        if (verbose) {
            println(message);
        }
    }
    
    private String safeGetNormalizedBaseDirectory() {
        try {
            return getNormalizedBaseDirectory();
        } catch (RuntimeException re) {
            return re.getLocalizedMessage();
        }
    }
    
    private String safeGetBuildTime() {
        try {
            return DateFormat.getDateTimeInstance().format(buildTime.getTime());
        } catch (RuntimeException re) {
            return re.getLocalizedMessage();
        }
    }
    
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
               .append(ENV_JOB_NAME, envVars.get(ENV_JOB_NAME))
               .append(ENV_BUILD_NUMBER, envVars.get(ENV_BUILD_NUMBER))
               .append(PROMOTION_ENV_VARS_PREFIX + ENV_JOB_NAME, envVars.get(PROMOTION_ENV_VARS_PREFIX + ENV_JOB_NAME))
               .append(PROMOTION_ENV_VARS_PREFIX + ENV_BUILD_NUMBER, envVars.get(PROMOTION_ENV_VARS_PREFIX + ENV_BUILD_NUMBER))
               .append("baseDirectory", safeGetNormalizedBaseDirectory())
               .append("buildTime", safeGetBuildTime())
               .toString();
	}

}
