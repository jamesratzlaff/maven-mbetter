package org.apache.maven.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CliArgsBuilder {
	private static enum ArgType {
		BOOLEAN, STRING, COLL, MAP
	}

	private static enum ArgBuilder implements Predicate<CliArgsBuilder>, Function<CliArgsBuilder, String> {
		ALTERNATE_POM_FILE(CLIManager.ALTERNATE_POM_FILE, CliArgsBuilder::getAlternatePomFile),
		SET_SYSTEM_PROPERTY(CLIManager.SET_SYSTEM_PROPERTY, CliArgsBuilder::getSystemProperties),
		OFFLINE(CLIManager.OFFLINE, CliArgsBuilder::isOffline), QUIET(CLIManager.QUIET, CliArgsBuilder::isQuiet),
		DEBUG(CLIManager.DEBUG, CliArgsBuilder::isDebug), ERRORS(CLIManager.ERRORS, CliArgsBuilder::isErrors),
		NON_RECURSIVE(CLIManager.NON_RECURSIVE, CliArgsBuilder::isNonRecursive),
		UPDATE_SNAPSHOTS(CLIManager.UPDATE_SNAPSHOTS, CliArgsBuilder::isUpdateSnapshots),
		ACTIVATE_PROFILES(CLIManager.ACTIVATE_PROFILES, CliArgsBuilder::getActivateProfiles),
		BATCH_MODE(CLIManager.BATCH_MODE, CliArgsBuilder::isBatchMode),
		SUPRESS_SNAPSHOT_UPDATES(CLIManager.SUPRESS_SNAPSHOT_UPDATES, CliArgsBuilder::isSupressSnapshotUpdates),
		CHECKSUM_FAILURE_POLICY(CLIManager.CHECKSUM_FAILURE_POLICY, CliArgsBuilder::isChecksumFailurePolicy),
		CHECKSUM_WARNING_POLICY(CLIManager.CHECKSUM_WARNING_POLICY, CliArgsBuilder::isChecksumWarningPolicy),
		ALTERNATE_USER_SETTINGS(CLIManager.ALTERNATE_USER_SETTINGS, CliArgsBuilder::getAlternateUserSettings),
		ALTERNATE_GLOBAL_SETTINGS(CLIManager.ALTERNATE_GLOBAL_SETTINGS, CliArgsBuilder::getAlternateGlobalSettings),
		ALTERNATE_USER_TOOLCHAINS(CLIManager.ALTERNATE_USER_TOOLCHAINS, CliArgsBuilder::getAlternateUserToolchains),
		ALTERNATE_GLOBAL_TOOLCHAINS(CLIManager.ALTERNATE_GLOBAL_TOOLCHAINS,
				CliArgsBuilder::getAlternateGlobalToolchains),
		FAIL_FAST(CLIManager.FAIL_FAST, CliArgsBuilder::isFailFast),
		FAIL_AT_END(CLIManager.FAIL_AT_END, CliArgsBuilder::isFailAtEnd),
		FAIL_NEVER(CLIManager.FAIL_NEVER, CliArgsBuilder::isFailNever),
		RESUME_FROM(CLIManager.RESUME_FROM, CliArgsBuilder::getResumeFrom),
		PROJECT_LIST(CLIManager.PROJECT_LIST, CliArgsBuilder::getProjectList),
		ALSO_MAKE(CLIManager.ALSO_MAKE, CliArgsBuilder::isAlsoMake),
		ALSO_MAKE_DEPENDENTS(CLIManager.ALSO_MAKE_DEPENDENTS, CliArgsBuilder::isAlsoMakeDependents),
		LOG_FILE(CLIManager.LOG_FILE, CliArgsBuilder::getLogFile),
		SHOW_VERSION(CLIManager.SHOW_VERSION, CliArgsBuilder::isShowVersion),
		ENCRYPT_MASTER_PASSWORD(CLIManager.ENCRYPT_MASTER_PASSWORD, CliArgsBuilder::getEncryptMasterPassword),
		ENCRYPT_PASSWORD(CLIManager.ENCRYPT_PASSWORD, CliArgsBuilder::getEncryptPassword),
		THREADS(CLIManager.THREADS, CliArgsBuilder::getThreads),
		LEGACY_LOCAL_REPOSITORY(CLIManager.LEGACY_LOCAL_REPOSITORY, CliArgsBuilder::isLegacyLocalRepository),
		BUILDER(CLIManager.BUILDER, CliArgsBuilder::getBuilder),
		NO_TRANSFER_PROGRESS(CLIManager.NO_TRANSFER_PROGRESS, CliArgsBuilder::isNoTransferProgress);

		private final String shortOpt;
		private final Function<CliArgsBuilder, ?> getter;

		private ArgBuilder(char shortName, Function<CliArgsBuilder, ?> getter) {
			this(Character.toString(shortName), getter);
		}

		private ArgBuilder(String shortName, Function<CliArgsBuilder, ?> getter) {
			this.shortOpt = shortName;
			this.getter = getter;
		}

		private static final Pattern WhiteSpacePattern = Pattern.compile("[\\s]+");
		private static final Predicate<String> whiteSpacePredicate = WhiteSpacePattern.asPredicate();

		@Override
		public String apply(CliArgsBuilder t) {
			StringBuilder sb = new StringBuilder();
			if (test(t)) {
				Object val = getter.apply(t);
				sb.append('-').append(shortOpt);
				if (val instanceof String) {
					sb.append(' ').append(String.valueOf(val));
				}
				if (val instanceof Map) {
					Map<?, ?> asMap = (Map<?, ?>) val;
					val = asMap.entrySet().stream()
							.map(entry -> Map.entry(entry.getKey(),
									whiteSpacePredicate.test(String.valueOf(entry.getValue()))
											? '"' + String.valueOf(entry.getValue()) + '"'
											: String.valueOf(entry.getValue())))
							.collect(Collectors.toSet());

				}
				if (val instanceof Collection) {
					
					String delim = ",";
					if (val instanceof Set) {
						delim = " -" + shortOpt;
					} else {
						sb.append(' ');
					}
					Collection<?> vals = (Collection<?>) val;
					String all = String.join(delim,
							vals.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.toList()));
					sb.append(all);
				}

			}
			return sb.toString();
		}

		public static List<String> toArgs(CliArgsBuilder t) {
			ArrayList<String> result = new ArrayList<String>();
			ArgBuilder[] all = ArgBuilder.values();

			for (int i = 0; i < all.length; i++) {
				String str = all[i].apply(t);
				if (str != null && !str.isEmpty()) {
					result.add(str);
				}
			}
			result.trimToSize();
			return result;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean test(CliArgsBuilder t) {
			Object o = getter.apply(t);
			if (o != null) {
				if (o instanceof Boolean) {
					return !Boolean.FALSE.equals(o);
				}
				if (o instanceof String) {
					return !((String) o).isBlank();
				}
				if (o instanceof Map) {
					return !((Map) o).isEmpty();
				}
				if (o instanceof Collection) {
					return !((Collection) o).isEmpty();
				}
			}
			return false;
		}

	}

	private Map<String, String> properties;

	public Map<String, String> getProperties() {
		if (this.properties == null) {
			this.properties = new LinkedHashMap<String, String>();
		}
		return this.properties;
	}

	private Set<String> goals;
	private Set<String> phases;
	private String alternatePomFile;
	private Map<String, String> systemProperties;
	private boolean offline;
	private boolean quiet;
	private boolean debug;
	private boolean errors;
	private boolean nonRecursive;
	private boolean updateSnapshots;
	private List<String> activateProfiles;
	private boolean batchMode;
	private boolean supressSnapshotUpdates;
	private boolean checksumFailurePolicy;
	private boolean checksumWarningPolicy;
	private String alternateUserSettings;
	private String alternateGlobalSettings;
	private List<String> alternateUserToolchains;
	private List<String> alternateGlobalToolchains;
	private boolean failFast;
	private boolean failAtEnd;
	private boolean failNever;
	private String resumeFrom;
	private List<String> projectList;
	private boolean alsoMake;
	private boolean alsoMakeDependents;
	private String logFile;
	private boolean showVersion;
	private String encryptMasterPassword;
	private String encryptPassword;
	private String threads;
	private boolean legacyLocalRepository;
	private String builder;
	private boolean noTransferProgress;

	public String getAlternatePomFile() {
		return this.alternatePomFile;
	}

	public Map<String, String> getSystemProperties() {
		if (this.systemProperties == null) {
			this.systemProperties = new LinkedHashMap<String, String>();
		}
		return this.systemProperties;
	}

	public boolean isOffline() {
		return this.offline;
	}

	public boolean isQuiet() {
		return this.quiet;
	}

	public boolean isDebug() {
		return this.debug;
	}

	public boolean isErrors() {
		return this.errors;
	}

	public boolean isNonRecursive() {
		return this.nonRecursive;
	}

	public boolean isUpdateSnapshots() {
		return this.updateSnapshots;
	}

	public List<String> getActivateProfiles() {
		return this.activateProfiles;
	}

	public boolean isBatchMode() {
		return this.batchMode;
	}

	public boolean isSupressSnapshotUpdates() {
		return this.supressSnapshotUpdates;
	}

	public boolean isChecksumFailurePolicy() {
		return this.checksumFailurePolicy;
	}

	public boolean isChecksumWarningPolicy() {
		return this.checksumWarningPolicy;
	}

	public String getAlternateUserSettings() {
		return this.alternateUserSettings;
	}

	public String getAlternateGlobalSettings() {
		return this.alternateGlobalSettings;
	}

	public List<String> getAlternateUserToolchains() {
		if (alternateUserToolchains == null) {
			this.alternateUserToolchains = new ArrayList<String>();
		}
		return this.alternateUserToolchains;
	}

	public List<String> getAlternateGlobalToolchains() {
		if (alternateGlobalToolchains == null) {
			this.alternateGlobalToolchains = new ArrayList<String>();
		}
		return this.alternateGlobalToolchains;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public boolean isFailAtEnd() {
		return this.failAtEnd;
	}

	public boolean isFailNever() {
		return this.failNever;
	}

	public String getResumeFrom() {
		return this.resumeFrom;
	}

	public List<String> getProjectList() {
		if (projectList == null) {
			projectList = new ArrayList<String>();
		}
		return this.projectList;
	}

	public boolean isAlsoMake() {
		return this.alsoMake;
	}

	public boolean isAlsoMakeDependents() {
		return this.alsoMakeDependents;
	}

	public String getLogFile() {
		return this.logFile;
	}

	public boolean isShowVersion() {
		return this.showVersion;
	}

	public String getEncryptMasterPassword() {
		return this.encryptMasterPassword;
	}

	public String getEncryptPassword() {
		return this.encryptPassword;
	}

	public String getThreads() {
		return this.threads;
	}

	public boolean isLegacyLocalRepository() {
		return this.legacyLocalRepository;
	}

	public String getBuilder() {
		return this.builder;
	}

	public boolean isNoTransferProgress() {
		return this.noTransferProgress;
	}

	public Set<String> getGoals() {
		if (this.goals == null) {
			this.goals = new LinkedHashSet<String>();
		}
		return this.goals;
	}

	public Set<String> getPhases() {
		if (this.phases == null) {
			this.phases = new LinkedHashSet<String>();
		}
		return this.phases;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public void setGoals(Set<String> goals) {
		this.goals = goals;
	}

	public void setPhases(Set<String> phases) {
		this.phases = phases;
	}

	public void setAlternatePomFile(String alternatePomFile) {
		this.alternatePomFile = alternatePomFile;
	}

	public void setSystemProperties(Map<String, String> systemProperties) {
		this.systemProperties = systemProperties;
	}

	public void setOffline(boolean offline) {
		this.offline = offline;
	}

	public void setQuiet(boolean quiet) {
		this.quiet = quiet;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setErrors(boolean errors) {
		this.errors = errors;
	}

	public void setNonRecursive(boolean nonRecursive) {
		this.nonRecursive = nonRecursive;
	}

	public void setUpdateSnapshots(boolean updateSnapshots) {
		this.updateSnapshots = updateSnapshots;
	}

	public void setActivateProfiles(List<String> activateProfiles) {
		this.activateProfiles = activateProfiles;
	}

	public void setBatchMode(boolean batchMode) {
		this.batchMode = batchMode;
	}

	public void setSupressSnapshotUpdates(boolean supressSnapshotUpdates) {
		this.supressSnapshotUpdates = supressSnapshotUpdates;
	}

	public void setChecksumFailurePolicy(boolean checksumFailurePolicy) {
		this.checksumFailurePolicy = checksumFailurePolicy;
	}

	public void setChecksumWarningPolicy(boolean checksumWarningPolicy) {
		this.checksumWarningPolicy = checksumWarningPolicy;
	}

	public void setAlternateUserSettings(String alternateUserSettings) {
		this.alternateUserSettings = alternateUserSettings;
	}

	public void setAlternateGlobalSettings(String alternateGlobalSettings) {
		this.alternateGlobalSettings = alternateGlobalSettings;
	}

	public void setAlternateUserToolchains(List<String> alternateUserToolchains) {
		this.alternateUserToolchains = alternateUserToolchains;
	}

	public void setAlternateGlobalToolchains(List<String> alternateGlobalToolchains) {
		this.alternateGlobalToolchains = alternateGlobalToolchains;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public void setFailAtEnd(boolean failAtEnd) {
		this.failAtEnd = failAtEnd;
	}

	public void setFailNever(boolean failNever) {
		this.failNever = failNever;
	}

	public void setResumeFrom(String resumeFrom) {
		this.resumeFrom = resumeFrom;
	}

	public void setProjectList(List<String> projectList) {
		this.projectList = projectList;
	}

	public void setAlsoMake(boolean alsoMake) {
		this.alsoMake = alsoMake;
	}

	public void setAlsoMakeDependents(boolean alsoMakeDependents) {
		this.alsoMakeDependents = alsoMakeDependents;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public void setShowVersion(boolean showVersion) {
		this.showVersion = showVersion;
	}

	public void setEncryptMasterPassword(String encryptMasterPassword) {
		this.encryptMasterPassword = encryptMasterPassword;
	}

	public void setEncryptPassword(String encryptPassword) {
		this.encryptPassword = encryptPassword;
	}

	public void setThreads(String threads) {
		this.threads = threads;
	}

	public void setLegacyLocalRepository(boolean legacyLocalRepository) {
		this.legacyLocalRepository = legacyLocalRepository;
	}

	public void setBuilder(String builder) {
		this.builder = builder;
	}

	public void setNoTransferProgress(boolean noTransferProgress) {
		this.noTransferProgress = noTransferProgress;
	}

	public CliArgsBuilder properties(Map<String, String> properties) {
		if (properties != null) {
			getProperties().putAll(properties);
		}
		return this;
	}

	public CliArgsBuilder goals(String... goals) {
		if (goals != null) {
			getGoals().addAll(List.of(goals));
		}
		return this;
	}

	public CliArgsBuilder phases(String... phases) {
		if (phases != null) {
			getPhases().addAll(List.of(phases));
		}
		return this;
	}

	public CliArgsBuilder alternatePomFile(String altPomFile) {
		setAlternatePomFile(altPomFile);
		return this;
	}

	public CliArgsBuilder systemProperties(Map<String, String> properties) {
		if (properties != null) {
			getSystemProperties().putAll(properties);
		}
		return this;
	}

	public CliArgsBuilder offline() {
		setOffline(true);
		return this;
	}

	public CliArgsBuilder quiet() {
		setQuiet(true);
		return this;
	}

	public CliArgsBuilder debug() {
		setDebug(true);
		return this;
	}

	public CliArgsBuilder errors() {
		setErrors(true);
		return this;
	}

	public CliArgsBuilder nonRecursive() {
		setNonRecursive(true);
		return this;
	}

	public CliArgsBuilder updateSnapshots() {
		setUpdateSnapshots(true);
		return this;
	}

	public CliArgsBuilder activateProfiles(String... profiles) {
		getActivateProfiles().addAll(List.of(profiles));
		return this;
	}

	public CliArgsBuilder batchMode() {
		setBatchMode(true);
		return this;
	}

	public CliArgsBuilder supressSnapshotUpdates() {
		setSupressSnapshotUpdates(true);
		return this;
	}

	public CliArgsBuilder checksumFailurePolicy() {
		setChecksumFailurePolicy(true);
		return this;
	}

	public CliArgsBuilder checksumWarningPolicy() {
		setChecksumWarningPolicy(true);
		return this;
	}

	public CliArgsBuilder alternateUserSettings(String userSettings) {
		setAlternateUserSettings(userSettings);
		return this;
	}

	public CliArgsBuilder alternateGlobalSettings(String globalSettings) {
		setAlternateGlobalSettings(globalSettings);
		return this;
	}

	public CliArgsBuilder alternateUserToolchains(String... userToolchains) {
		getAlternateUserToolchains().addAll(List.of(userToolchains));
		return this;
	}

	public CliArgsBuilder alternateGlobalToolchains(String... globalToolchains) {
		getAlternateGlobalToolchains().addAll(List.of(globalToolchains));
		return this;
	}

	public CliArgsBuilder systemProperty(String name, String value) {
		if (name != null) {
			if (value == null) {
				getSystemProperties().remove(name);
			} else {
				getSystemProperties().put(name, value);
			}
		}
		return this;
	}

	public CliArgsBuilder failFast() {
		setFailFast(true);
		return this;
	}

	public CliArgsBuilder failAtEnd() {
		setFailAtEnd(true);
		return this;
	}

	public CliArgsBuilder failNever() {
		setFailNever(true);
		return this;
	}

	public CliArgsBuilder resumeFrom(String builderId) {
		setResumeFrom(builderId);
		return this;
	}

	public CliArgsBuilder projectList(String... projects) {
		getProjectList().addAll(List.of(projects));
		return this;
	}

	public CliArgsBuilder alsoMake() {
		setAlsoMake(true);
		return this;
	}

	public CliArgsBuilder alsoMakeDependents() {
		setAlsoMakeDependents(true);
		return this;
	}

	public CliArgsBuilder logFile(String logFile) {
		setLogFile(logFile);
		return this;
	}

	public CliArgsBuilder showVersion() {
		setShowVersion(true);
		return this;
	}

	public CliArgsBuilder encryptMasterPassword(String pswd) {
		setEncryptMasterPassword(pswd);
		return this;
	}

	public CliArgsBuilder encryptPassword(String pswd) {
		setEncryptPassword(pswd);
		return this;
	}

	public CliArgsBuilder threads(String threads) {
		setThreads(threads);
		return this;
	}

	public CliArgsBuilder legacyLocalRepository() {
		setLegacyLocalRepository(true);
		return this;
	}

	public CliArgsBuilder builder(String builderId) {
		setBuilder(builderId);
		return this;
	}

	public CliArgsBuilder noTransferProgress() {
		setNoTransferProgress(true);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(activateProfiles, alsoMake, alsoMakeDependents, alternateGlobalSettings,
				alternateGlobalToolchains, alternatePomFile, alternateUserSettings, alternateUserToolchains, batchMode,
				builder, checksumFailurePolicy, checksumWarningPolicy, debug, encryptMasterPassword, encryptPassword,
				errors, failAtEnd, failFast, failNever, goals, legacyLocalRepository, logFile, noTransferProgress,
				nonRecursive, offline, phases, projectList, properties, quiet, resumeFrom, showVersion,
				supressSnapshotUpdates, systemProperties, threads, updateSnapshots);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof CliArgsBuilder)) {
			return false;
		}
		CliArgsBuilder other = (CliArgsBuilder) obj;
		return Objects.equals(activateProfiles, other.activateProfiles) && alsoMake == other.alsoMake
				&& alsoMakeDependents == other.alsoMakeDependents
				&& Objects.equals(alternateGlobalSettings, other.alternateGlobalSettings)
				&& Objects.equals(alternateGlobalToolchains, other.alternateGlobalToolchains)
				&& Objects.equals(alternatePomFile, other.alternatePomFile)
				&& Objects.equals(alternateUserSettings, other.alternateUserSettings)
				&& Objects.equals(alternateUserToolchains, other.alternateUserToolchains)
				&& batchMode == other.batchMode && Objects.equals(builder, other.builder)
				&& checksumFailurePolicy == other.checksumFailurePolicy
				&& checksumWarningPolicy == other.checksumWarningPolicy && debug == other.debug
				&& Objects.equals(encryptMasterPassword, other.encryptMasterPassword)
				&& Objects.equals(encryptPassword, other.encryptPassword) && errors == other.errors
				&& failAtEnd == other.failAtEnd && failFast == other.failFast && failNever == other.failNever
				&& Objects.equals(goals, other.goals) && legacyLocalRepository == other.legacyLocalRepository
				&& Objects.equals(logFile, other.logFile) && noTransferProgress == other.noTransferProgress
				&& nonRecursive == other.nonRecursive && offline == other.offline
				&& Objects.equals(phases, other.phases) && Objects.equals(projectList, other.projectList)
				&& Objects.equals(properties, other.properties) && quiet == other.quiet
				&& Objects.equals(resumeFrom, other.resumeFrom) && showVersion == other.showVersion
				&& supressSnapshotUpdates == other.supressSnapshotUpdates
				&& Objects.equals(systemProperties, other.systemProperties) && Objects.equals(threads, other.threads)
				&& updateSnapshots == other.updateSnapshots;
	}

	@Override
	public String toString() {
		List<String> l = ArgBuilder.toArgs(this);
		if (getGoals() != null && !getGoals().isEmpty()) {
			l.addAll(getGoals());
		}
		if (getPhases() != null && !getPhases().isEmpty()) {
			l.addAll(getPhases());
		}
		return String.join(" ", l);
	}

	public static void main(String[] args) {
		CliArgsBuilder b = new CliArgsBuilder();
		System.out.println(b.systemProperty("herp", "derp").systemProperty("jerp", "lerple gerple").goals("herfy","lerfy").quiet());
	}

}
