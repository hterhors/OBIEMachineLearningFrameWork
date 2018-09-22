package de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.param;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.AssignableSubClasses;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.AssignableSubInterfaces;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DirectInterface;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.ImplementationClass;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.projects.AbstractOBIEProjectEnvironment;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.utils.ToStringFormatter;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.corpus.distributor.AbstractCorpusDistributor;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.evaluation.evaluator.IEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.explorer.AbstractOBIEExplorer;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.explorer.IExplorationCondition;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.explorer.utils.ExplorationUtils;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.InvestigationRestriction;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.AbstractOBIETemplate;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import learning.optimizer.Optimizer;
import learning.optimizer.SGD;
import learning.regularizer.Regularizer;
import libsvm.svm_parameter;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.AcceptStrategy;
import sampling.samplingstrategies.SamplingStrategies;
import sampling.samplingstrategies.SamplingStrategy;

public class OBIERunParameter implements Serializable {

	/**
	 * TODO: via parameter greedy vs linear.
	 */
//	public final static SamplingStrategy<OBIEState> trainSamplingStrategyObjectiveScore = SamplingStrategies
//			.linearObjectiveSamplingStrategy();
//
//	public final static SamplingStrategy<OBIEState> trainSamplingStrategyModelScore = SamplingStrategies
//			.linearModelSamplingStrategy();

	public final static SamplingStrategy<OBIEState> trainSamplingStrategyObjectiveScore = SamplingStrategies
			.greedyObjectiveStrategy();

	public final static SamplingStrategy<OBIEState> trainSamplingStrategyModelScore = SamplingStrategies
			.greedyModelStrategy();

	/*
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */

	public final static AcceptStrategy<OBIEState> trainAcceptanceStrategyObjectiveScore = AcceptStrategies
			.strictObjectiveAccept();

	public final static AcceptStrategy<OBIEState> trainAcceptanceStrategyModelScore = AcceptStrategies
			.strictModelAccept();

	public final static SamplingStrategy<OBIEState> testSamplingStrategy = SamplingStrategies.greedyModelStrategy();

	public final static AcceptStrategy<OBIEState> testAcceptanceStrategy = AcceptStrategies.strictModelAccept();

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static Logger log = LogManager.getFormatterLogger(OBIERunParameter.class);

	/**
	 * A manually predefined set of class types which should be explored even though
	 * there is no evidence in the text (annotated entities)
	 * 
	 * The set is automatically expanded to all subclasses.
	 * 
	 */
	public final Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence;

	/**
	 * Whether the system should run in multiple threads. Watch our for thread
	 * safety.
	 */
	public final boolean multiThreading;

	/**
	 * The maximum number of sampling steps per instance.
	 */
	public final int maxNumberOfSamplingSteps;

	/**
	 * If {@link #scorerType} is of type internal, this is the L2 regularization
	 * during training.
	 */

	public final Regularizer regularizer;

	/**
	 * The set of factor and feature generating templates.
	 */
	public final Set<Class<? extends AbstractOBIETemplate<?>>> templates;

	/**
	 * The run ID of the current system run.
	 */
	public final String runID;;

	/**
	 * Teh root directoy of output.
	 */
	public final File rootDirectory;

	/**
	 * The number of epochs.
	 */
	public final int epochs;

	/**
	 * If {@link #scorerType} is of type internal, this is the alpha learning rate.
	 */
	public final Optimizer optimizer;

	/**
	 * The scorer type might be internal (BIGRAM-provided) such as EXP, LINEAR,
	 * SOFTPLUS or externally provided such as LIBSVM, LIBLINEAR or even completely
	 * new defined.
	 */
	public final EScorerType scorerType;

	/**
	 * Personal notes that are printed on summary.
	 */
	public final String personalNotes;

	/**
	 * The main classes of interest.
	 */
	public final Set<Class<? extends IOBIEThing>> rootSearchTypes;

	/**
	 * The initializer allows various types of strategies to start an exploration.
	 * This might be RANDOM, EMPTY, PREDEFINED...
	 */
	public final EInstantiationType initializer;

	/**
	 * The projects environment.
	 */
	public final AbstractOBIEProjectEnvironment environment;

	/**
	 * Tasks specific exploration condition that goes beyond the
	 * {@link #investigationRestriction}. This allows to set hard exploration
	 * constrains.
	 * 
	 * Even more specified exploration conditions. This does not affect the
	 * evaluation! Use this interface to specify if specific fields should be
	 * explored under given conditions. If the childClass can not be used in that
	 * slot for that parent by external specification or limitations.
	 * 
	 * Set this to always return true if no further exploration conditions should be
	 * defined.
	 */
	public final IExplorationCondition explorationCondition;
	/**
	 * The set of explorers that define the sampling.
	 */
	public final Set<Class<? extends AbstractOBIEExplorer>> explorers;

	public final svm_parameter svmParam;

	/**
	 * The investigation restriction allows to focus only on specific slots. It
	 * influences the sampling procedure and the evaluation.
	 */
	public final InvestigationRestriction investigationRestriction;

	/**
	 * A predefined set of initial objects if {@link #initializer} is set
	 * PREDEFINED.
	 * 
	 * @see #initializer
	 */
	public final Map<Class<? extends IOBIEThing>, List<IOBIEThing>> initializationObjects;

	/**
	 * Whether the exploration should consider existing templates. If set to true it
	 * is possible to assign previously explored or gold annotated templates.
	 */
	public final boolean exploreExistingTemplates;

	/**
	 * Whether the sampling procedure should take the discourse progression into
	 * account. Can only be set to true if {@link #exploreOnOntologyLevel} is set to
	 * false. If this is set to true, it restricts the sampling to entities that
	 * follows the discourse progression based on the properties of each root class
	 * type.
	 * 
	 * E.g. only slot fillers are explored that are textually after the entity of
	 * interest.
	 */
	public final boolean enableDiscourseProgression;

	/**
	 * Whether the sampling procedure is restricted to explore on the ontology level
	 * rather than on the actual entities found in the text. If this is set to true
	 * just a single change is performed based on the ontology, if there is at least
	 * one entity of that type in the text.
	 * 
	 * This sampling is much faster but groups all existing entities of the same
	 * type into a single. This needs to be handled in all templates.
	 */
	public final boolean exploreOnOntologyLevel;

	/**
	 * The number of initially generated entities for a new state. This may be
	 * random, fixed to specific positive number or aligned to gold data.
	 */
	public final IInitializeNumberOfObjects numberOfInitializedObjects;

	/**
	 * The maximum number of entity elements (non data types) within a list.
	 * 
	 * @see #maxNumberOfDataTypeElements
	 */
	public final int maxNumberOfEntityElements;

	/**
	 * The evaluator which is used during training (within the objective function)
	 * and afterwards evaluating on test set.
	 */
	public final IEvaluator evaluator;

	/**
	 * Restricts the maximum number of data type elements within a list. This is
	 * important to set an upper limit during sampling and for heavy evaluation.
	 *
	 * @see #maxNumberOfEntityElements
	 */
	public final int maxNumberOfDataTypeElements;

	/**
	 * The random object to get deterministic sampling. Per default this is
	 * unseeded, thus random every time.
	 */
	public final Random rndForSampling;

	/**
	 * Whether empty instances should be ignored during evaluation. This includes
	 * the objective function during training! It is realized by removing all empty
	 * instances before evaluation.
	 */
	public final boolean ignoreEmptyInstancesonEvaluation;

	/**
	 * Whether empty instances, instances without any annotations, should be
	 * excluded from the corpus while training and predicting.
	 */
	public final boolean excludeEmptyInstancesFromCorpus;

	public final String corpusNamePrefix;

	public final AbstractCorpusDistributor corpusDistributor;

	private OBIERunParameter(final String corpusNamePrefix, final boolean excludeEmptyInstancesFromCorpus,
			Set<Class<? extends AbstractOBIETemplate<?>>> templates, File rootDirectory, int epochs,
			Optimizer optimizer, EScorerType scorerType, String personalNotes,
			Set<Class<? extends IOBIEThing>> rootSearchTypes, EInstantiationType initializer, final String runID,
			boolean multiThreading, AbstractOBIEProjectEnvironment environment,
			Class<? extends IOBIEThing>[] manualExploreClassesWithoutEvidence,
			IExplorationCondition explorationCondition, Set<Class<? extends AbstractOBIEExplorer>> explorersTypes,
			svm_parameter svmParam, InvestigationRestriction investigationRestriction,
			Map<Class<? extends IOBIEThing>, List<IOBIEThing>> initializationObjects, boolean exploreExistingTemplates,
			boolean exploreOnOntologyLevel, boolean enableDiscourseProgression,
			IInitializeNumberOfObjects numberOfInitializedObjects, IEvaluator evaluator,
			final int maxNumberOfEntityElements, final int maxNumberOfDataTypeElements, Regularizer regularizer,
			int maxNumberOfSamplingSteps, Random rndForSampling, boolean ignoreEmptyInstancesonEvaluation,
			final AbstractCorpusDistributor corpusConfiguration) {

		if (!validate()) {
			throw new IllegalStateException("The given paramters do not match.");
		}

		requireElements(templates);
		requireGreaterThanZero(epochs);
		Objects.requireNonNull(rootDirectory);
		requireElements(rootSearchTypes);
		Objects.requireNonNull(environment);
		Objects.requireNonNull(corpusConfiguration);

		Objects.requireNonNull(optimizer);

		Objects.requireNonNull(rndForSampling);

		Objects.requireNonNull(manualExploreClassesWithoutEvidence);
		Objects.requireNonNull(explorationCondition);
		requireElements(explorersTypes);

		if (scorerType == EScorerType.LIB_SVM)
			Objects.requireNonNull(svmParam);

		Objects.requireNonNull(investigationRestriction);

		if (initializer == EInstantiationType.SPECIFIED) {
			requireElements(initializationObjects);
			for (Class<? extends IOBIEThing> initObjectType : initializationObjects.keySet()) {
				if (!rootSearchTypes.contains(initObjectType)) {
					throw new IllegalStateException(
							"Initialization object does not match specification of root search types: " + initObjectType
									+ " not in " + rootSearchTypes + "!");
				}
			}
		}

		if (rootSearchTypes.size() > 1) {
			throw new IllegalStateException("System does not support multiple root search types: " + rootSearchTypes);
		}

		Objects.requireNonNull(corpusNamePrefix);
		Objects.requireNonNull(numberOfInitializedObjects);
		Objects.requireNonNull(evaluator);

		requireGreaterThanZero(maxNumberOfEntityElements);
		requireGreaterThanZero(maxNumberOfDataTypeElements);
		requireGreaterThanZero(maxNumberOfSamplingSteps);

		this.corpusDistributor = corpusConfiguration;
		this.corpusNamePrefix = corpusNamePrefix;
		this.excludeEmptyInstancesFromCorpus = excludeEmptyInstancesFromCorpus;
		this.rndForSampling = rndForSampling;
		this.regularizer = regularizer;
		this.maxNumberOfSamplingSteps = maxNumberOfSamplingSteps;
		this.templates = templates;
		this.rootDirectory = rootDirectory;
		this.epochs = epochs;
		this.optimizer = optimizer;
		this.scorerType = scorerType;
		this.personalNotes = personalNotes;
		this.rootSearchTypes = rootSearchTypes;
		this.initializer = initializer;

		this.ignoreEmptyInstancesonEvaluation = ignoreEmptyInstancesonEvaluation;
		this.runID = runID;
		this.multiThreading = multiThreading;
		this.environment = environment;

		this.exploreClassesWithoutTextualEvidence = autoExpand(environment,
				manualExpand(manualExploreClassesWithoutEvidence));

		this.explorationCondition = explorationCondition;
		this.explorers = explorersTypes;
		this.svmParam = svmParam;
		this.investigationRestriction = investigationRestriction;
		this.initializationObjects = initializationObjects;
		this.exploreExistingTemplates = exploreExistingTemplates;
		this.exploreOnOntologyLevel = exploreOnOntologyLevel;
		this.enableDiscourseProgression = enableDiscourseProgression;
		this.numberOfInitializedObjects = numberOfInitializedObjects;

		this.evaluator = evaluator;

		this.maxNumberOfEntityElements = maxNumberOfEntityElements;
		this.maxNumberOfDataTypeElements = maxNumberOfDataTypeElements;
	}

	private void requireGreaterThanZero(double number) {
		if (number < 0)
			throw new IllegalStateException("Configuration requires value greater than 0 for :" + number);
	}

	private void requireElements(Map<?, ? extends Collection<?>> map) {
		Objects.requireNonNull(map);

		if (map.isEmpty())
			throw new IllegalStateException("Configuration requires at least one element in: " + map);

		for (Entry<?, ? extends Collection<?>> e : map.entrySet()) {

			if (e.getValue().isEmpty())
				throw new IllegalStateException("Configuration requires at least one element for key"
						+ String.valueOf(e.getKey()) + " in: " + map);
		}

	}

	private void requireElements(Collection<?> collection) {
		Objects.requireNonNull(collection);

		if (collection.isEmpty())
			throw new IllegalStateException("Configuration requires at least one element in: " + collection);

	}

	private boolean validate() {
		return true;
	}

	private Set<Class<? extends IOBIEThing>> manualExpand(Class<? extends IOBIEThing>[] classes) {
		Set<Class<? extends IOBIEThing>> build = new HashSet<>();

		for (Class<? extends IOBIEThing> class1 : classes) {
			for (Class<? extends IOBIEThing> c : class1.getAnnotation(AssignableSubClasses.class).get()) {
				build.add(c);
				build.add(c.getAnnotation(DirectInterface.class).get());
			}
		}
		return build;

	}

	private Set<Class<? extends IOBIEThing>> autoExpand(AbstractOBIEProjectEnvironment scioEnvironment,
			Set<Class<? extends IOBIEThing>> set) {

		for (Class<? extends IOBIEThing> c : scioEnvironment.getOntologyThingInterface()
				.getAnnotation(AssignableSubInterfaces.class).get()) {
			if (ExplorationUtils.isAuxiliaryProperty(c)) {
				if (c.isAnnotationPresent(ImplementationClass.class)) {
					set.add(c.getAnnotation(ImplementationClass.class).get());
				} else {
					log.warn("Can not find implementation class for: " + c.getSimpleName());
				}
			}
		}
		return set;
	}

	public String toInfoString() {
		/*
		 * TODO: implement.
		 */
		return ToStringFormatter.formatString(toString());
	}

	@Override
	public String toString() {
		return "OBIEParameter [trainSamplingStrategyObjectiveScore=" + trainSamplingStrategyObjectiveScore
				+ ", trainSamplingStrategyModelScore=" + trainSamplingStrategyModelScore
				+ ", trainAcceptanceStrategyObjectiveScore=" + trainAcceptanceStrategyObjectiveScore
				+ ", trainAcceptanceStrategyModelScore=" + trainAcceptanceStrategyModelScore + ", testSamplingStrategy="
				+ testSamplingStrategy + ", testAcceptanceStrategy=" + testAcceptanceStrategy
				+ ", exploreClassesWithoutTextualEvidence=" + exploreClassesWithoutTextualEvidence + ", multiThreading="
				+ multiThreading + ", maxNumberOfSamplingSteps=" + maxNumberOfSamplingSteps + ", regularizer="
				+ regularizer + ", templates=" + templates + ", runID=" + runID + ", rootDirectory=" + rootDirectory
				+ ", epochs=" + epochs + ", optimizer=" + optimizer + ", scorerType=" + scorerType + ", personalNotes="
				+ personalNotes + ", rootSearchTypes=" + rootSearchTypes + ", initializer=" + initializer
				+ ", environment=" + environment + ", explorationCondition=" + explorationCondition + ", explorers="
				+ explorers + ", svmParam=" + svmParam + ", investigationRestriction=" + investigationRestriction
				+ ", initializationObjects=" + initializationObjects + ", exploreExistingTemplates="
				+ exploreExistingTemplates + ", enableDiscourseProgression=" + enableDiscourseProgression
				+ ", exploreOnOntologyLevel=" + exploreOnOntologyLevel + ", numberOfInitializedObjects="
				+ numberOfInitializedObjects + ", maxNumberOfEntityElements=" + maxNumberOfEntityElements
				+ ", evaluator=" + evaluator + ", maxNumberOfDataTypeElements=" + maxNumberOfDataTypeElements
				+ ", rndForSampling=" + rndForSampling + ", ignoreEmptyInstancesonEvaluation="
				+ ignoreEmptyInstancesonEvaluation + "]";
	}

	public static class OBIEParameterBuilder {
		/**
		 * Even more specified exploration conditions. This does not affect the
		 * evaluation! Use this interface to specify if specific fields should be
		 * explored under given conditions. If the childClass can not be used in that
		 * slot for that parent by external specification or limitations.
		 * 
		 * Set this to always return true if no further exploration conditions should be
		 * defined.
		 */
		private IExplorationCondition explorationCondition = (baseClass, baseClassFieldName, candidateClass) -> true;

		private Map<Class<? extends IOBIEThing>, List<IOBIEThing>> initializationObjects = new HashMap<>();

		@SuppressWarnings("unchecked")
		private Class<? extends IOBIEThing>[] manualExploreClassesWithoutEvidence = new Class[] {};

		private Set<Class<? extends AbstractOBIEExplorer>> explorers = new HashSet<>();

		private Set<Class<? extends AbstractOBIETemplate<?>>> templates = new HashSet<>();

		private IInitializeNumberOfObjects numberOfInitializedObjects = instance -> 1;

		private Set<Class<? extends IOBIEThing>> rootSearchTypes = new HashSet<>();

		private InvestigationRestriction investigationRestriction = InvestigationRestriction.noRestrictionInstance;

		private EInstantiationType initializer = EInstantiationType.EMPTY;

		private IEvaluator evaluator = null;

		private boolean enableDiscourseProgression = false;

		private boolean exploreExistingTemplates = false;

		private EScorerType scorerType = EScorerType.EXP;

		private boolean exploreOnOntologyLevel = false;

		private AbstractOBIEProjectEnvironment environment;

		private String personalNotes = "Default Notes";

		private String runID = String.valueOf("RND_" + new Random().nextInt());

		private int maxNumberOfSamplingSteps = 100;

		private Regularizer regularizer = null;

		private boolean multiThreading = true;

		private svm_parameter svmParam;

		private File rootDirectory;

		private Random rndForSampling = new Random();

		private Optimizer optimizer = new SGD(0.001, 0, 0.0001, false);
		private int epochs = 100;

		private int maxNumberOfEntityElements = 7;

		private int maxNumberOfDataTypeElements = 20;

		private boolean ignoreEmptyInstancesonEvaluation = false;
		/**
		 * Whether empty instances, instances without any annotations, should be
		 * excluded from the corpus while training and predicting.
		 */
		private boolean excludeEmptyInstancesFromCorpus = true;

		private String corpusNamePrefix = "";

		private AbstractCorpusDistributor corpusConfiguration = null;

		public OBIEParameterBuilder() {
		}

		public AbstractCorpusDistributor getCorpusConfiguration() {
			return corpusConfiguration;
		}

		public OBIEParameterBuilder setCorpusDistributor(AbstractCorpusDistributor corpusConfiguration) {
			this.corpusConfiguration = corpusConfiguration;
			return this;
		}

		public String getCorpusNamePrefix() {
			return corpusNamePrefix;
		}

		public OBIEParameterBuilder setCorpusNamePrefix(String corpusNamePrefix) {
			this.corpusNamePrefix = corpusNamePrefix;
			return this;
		}

		public boolean isExcludeEmptyInstancesFromCorpus() {
			return excludeEmptyInstancesFromCorpus;
		}

		public OBIEParameterBuilder setExcludeEmptyInstancesFromCorpus(boolean excludeEmptyInstancesFromCorpus) {
			this.excludeEmptyInstancesFromCorpus = excludeEmptyInstancesFromCorpus;
			return this;
		}

		public IEvaluator getEvaluator() {
			return evaluator;
		}

		public OBIEParameterBuilder setEvaluator(IEvaluator evaluator) {
			this.evaluator = evaluator;
			return this;
		}

		public Class<? extends IOBIEThing>[] getManualExploreClassesWithoutEvidence() {
			return manualExploreClassesWithoutEvidence;
		}

		public OBIEParameterBuilder setManualExploreClassesWithoutEvidence(
				Class<? extends IOBIEThing>[] exploreClassesWithoutEvidence) {
			this.manualExploreClassesWithoutEvidence = exploreClassesWithoutEvidence;
			return this;
		}

		public boolean isMultiThreading() {
			return multiThreading;
		}

		public OBIEParameterBuilder setMultiThreading(boolean multiThreading) {
			this.multiThreading = multiThreading;
			return this;
		}

		public int getNumberOfMaxSamplingSteps() {
			return maxNumberOfSamplingSteps;
		}

		public OBIEParameterBuilder setNumberOfMaxSamplingSteps(int numberOfMaxSamplingSteps) {
			this.maxNumberOfSamplingSteps = numberOfMaxSamplingSteps;
			return this;
		}

		public Regularizer getRegularizer() {
			return regularizer;
		}

		public OBIEParameterBuilder setRegularizer(Regularizer regularizer) {
			this.regularizer = regularizer;
			return this;
		}

		public Set<Class<? extends AbstractOBIETemplate<?>>> getTemplates() {
			return templates;
		}

		public OBIEParameterBuilder setTemplates(Set<Class<? extends AbstractOBIETemplate<?>>> templates) {
			this.templates = templates;
			return this;
		}

		public OBIEParameterBuilder addTemplate(Class<? extends AbstractOBIETemplate<?>> template) {
			this.templates.add(template);
			return this;
		}

		public String getRunID() {
			return runID;
		}

		public OBIEParameterBuilder setRunID(String runID) {
			this.runID = runID;
			return this;
		}

		public File getRootDirectory() {
			return rootDirectory;
		}

		public OBIEParameterBuilder setRootDirectory(File rootDirectory) {
			this.rootDirectory = rootDirectory;
			return this;
		}

		public int getEpochs() {
			return epochs;
		}

		public OBIEParameterBuilder setEpochs(int epochs) {
			this.epochs = epochs;
			return this;
		}

		public Optimizer getOptimizer() {
			return optimizer;
		}

		public OBIEParameterBuilder setOptimizer(Optimizer optimizer) {
			this.optimizer = optimizer;
			return this;
		}

		public EScorerType getScorerType() {
			return scorerType;
		}

		public OBIEParameterBuilder setScorerType(EScorerType scorerType) {
			this.scorerType = scorerType;
			return this;
		}

		public String getPersonalNotes() {
			return personalNotes;
		}

		public OBIEParameterBuilder setPersonalNotes(String personalNotes) {
			this.personalNotes = personalNotes;
			return this;
		}

		public Set<Class<? extends IOBIEThing>> getRootSearchTypes() {
			return rootSearchTypes;
		}

		public OBIEParameterBuilder setRootSearchTypes(Set<Class<? extends IOBIEThing>> rootSearchTypes) {
			this.rootSearchTypes = rootSearchTypes;
			return this;
		}

		public OBIEParameterBuilder addRootSearchType(Class<? extends IOBIEThing> rootSearchType) {
			this.rootSearchTypes.add(rootSearchType);
			return this;
		}

		public EInstantiationType getInitializer() {
			return initializer;
		}

		public OBIEParameterBuilder setInitializer(EInstantiationType initializer) {
			this.initializer = initializer;
			return this;
		}

		public AbstractOBIEProjectEnvironment getEnvironment() {
			return environment;
		}

		public OBIEParameterBuilder setEnvironment(AbstractOBIEProjectEnvironment environment) {
			this.environment = environment;
			return this;
		}

		public IExplorationCondition getExplorationCondition() {
			return explorationCondition;
		}

		/**
		 * Even more specified exploration conditions. This does not affect the
		 * evaluation! Use this interface to specify if specific fields should be
		 * explored under given conditions. If the childClass can not be used in that
		 * slot for that parent by external specification or limitations.
		 * 
		 * Set this to always return true if no further exploration conditions should be
		 * defined.
		 */
		public OBIEParameterBuilder setExplorationCondition(IExplorationCondition explorationCondition) {
			this.explorationCondition = explorationCondition;
			return this;
		}

		public Set<Class<? extends AbstractOBIEExplorer>> getExplorers() {
			return explorers;
		}

		public OBIEParameterBuilder setExplorers(Set<Class<? extends AbstractOBIEExplorer>> explorers) {
			this.explorers = explorers;
			return this;
		}

		public InvestigationRestriction getInvestigationRestriction() {
			return investigationRestriction;
		}

		public OBIEParameterBuilder setInvestigationRestriction(InvestigationRestriction investigationRestriction) {
			this.investigationRestriction = investigationRestriction;
			return this;
		}

		public Map<Class<? extends IOBIEThing>, List<IOBIEThing>> getInitializationObjects() {
			return initializationObjects;
		}

		public OBIEParameterBuilder setInitializationObjects(
				Map<Class<? extends IOBIEThing>, List<IOBIEThing>> initializationObjects) {
			this.initializationObjects = initializationObjects;
			return this;
		}

		public svm_parameter getSvmParam() {
			return svmParam;
		}

		public OBIEParameterBuilder setSvmParam(svm_parameter svmParam) {
			this.svmParam = svmParam;
			return this;
		}

		public boolean isExploreExistingTemplates() {
			return exploreExistingTemplates;
		}

		public OBIEParameterBuilder setExploreExistingTemplates(boolean exploreExistingTemplates) {
			this.exploreExistingTemplates = exploreExistingTemplates;
			return this;
		}

		public boolean isEnableDiscourseProgression() {
			return enableDiscourseProgression;
		}

		public OBIEParameterBuilder setEnableDiscourseProgression(boolean enableDiscourseProgression) {
			this.enableDiscourseProgression = enableDiscourseProgression;
			return this;
		}

		public boolean isIgnoreEmptyInstancesonEvaluation() {
			return ignoreEmptyInstancesonEvaluation;
		}

		public OBIEParameterBuilder setIgnoreEmptyInstancesonEvaluation(boolean ignoreEmptyInstancesonEvaluation) {
			this.ignoreEmptyInstancesonEvaluation = ignoreEmptyInstancesonEvaluation;
			return this;
		}

		public boolean isExploreOnOntologyLevel() {
			return exploreOnOntologyLevel;
		}

		public OBIEParameterBuilder setExploreOnOntologyLevel(boolean exploreOnOntologyLevel) {
			this.exploreOnOntologyLevel = exploreOnOntologyLevel;
			return this;
		}

		public IInitializeNumberOfObjects getNumberOfInitializedObjects() {
			return numberOfInitializedObjects;
		}

		public OBIEParameterBuilder setNumberOfInitializedObjects(
				IInitializeNumberOfObjects numberOfInitializedObjects) {
			this.numberOfInitializedObjects = numberOfInitializedObjects;
			return this;
		}

		public int getMaxNumberOfEntityElements() {
			return maxNumberOfEntityElements;
		}

		public OBIEParameterBuilder setMaxNumberOfEntityElements(int maxNumberOfEntityElements) {
			this.maxNumberOfEntityElements = maxNumberOfEntityElements;
			return this;
		}

		public int getMaxNumberOfDataTypeElements() {
			return maxNumberOfDataTypeElements;
		}

		public Random getRandomForSampling() {
			return rndForSampling;
		}

		public OBIEParameterBuilder setRandomForSampling(Random rndForSampling) {
			this.rndForSampling = rndForSampling;
			return this;
		}

		public OBIEParameterBuilder setMaxNumberOfDataTypeElements(int maxNumberOfDataTypeElements) {
			this.maxNumberOfDataTypeElements = maxNumberOfDataTypeElements;
			return this;
		}

		public OBIERunParameter build() {

			return new OBIERunParameter(corpusNamePrefix, excludeEmptyInstancesFromCorpus, templates, rootDirectory,
					epochs, optimizer, scorerType, personalNotes, rootSearchTypes, initializer, runID, multiThreading,
					environment, manualExploreClassesWithoutEvidence, explorationCondition, explorers, svmParam,
					investigationRestriction, initializationObjects, exploreExistingTemplates, exploreOnOntologyLevel,
					enableDiscourseProgression, numberOfInitializedObjects, evaluator, maxNumberOfEntityElements,
					maxNumberOfDataTypeElements, regularizer, maxNumberOfSamplingSteps, rndForSampling,
					ignoreEmptyInstancesonEvaluation, corpusConfiguration);
		}

	}

}
