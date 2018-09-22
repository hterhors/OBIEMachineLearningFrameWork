package de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.RelationTypeCollection;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.ner.regex.BasicRegExPattern;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.param.OBIERunParameter;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.InterTokenTemplate.Scope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.scope.OBIEFactorScope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.utils.ReflectionUtils;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.EntityAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEInstance;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import factors.Factor;
import learning.Vector;

/**
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class InterTokenTemplate extends AbstractOBIETemplate<Scope>
		implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(InterTokenTemplate.class);

	private static final String TOKEN_SPLITTER_SPACE = " ";

	private static final String END_DOLLAR = "$";

	private static final String START_CIRCUMFLEX = "^";

	private static final int MIN_TOKEN_LENGTH = 2;

	private static final String LEFT = "<";

	private static final String RIGHT = ">";
	/**
	 * Whether distant supervision is enabled for this template or not. This
	 * effects the way of calculating the factors and features!
	 */
	private final boolean enableDistantSupervision;

	private final AbstractOBIETemplate<?> thisTemplate;

	public InterTokenTemplate(OBIERunParameter parameter) {
		super(parameter);
		this.thisTemplate = this;
		this.enableDistantSupervision = parameter.exploreOnOntologyLevel;
	}

	class Scope extends OBIEFactorScope {

		public Class<? extends IOBIEThing> classType;
		public Set<String> surfaceForms;

		public Scope(Set<Class<? extends IOBIEThing>> influencedVariables, Class<? extends IOBIEThing> classType,
				Set<String> surfaceForms, Class<? extends IOBIEThing> entityRootClassType) {
			super(influencedVariables, entityRootClassType, thisTemplate, entityRootClassType, classType, surfaceForms);
			this.classType = classType;
			this.surfaceForms = surfaceForms;
		}

	}

	@Override
	public List<Scope> generateFactorScopes(OBIEState state) {
		List<Scope> factors = new ArrayList<>();

		for (EntityAnnotation entity : state.getCurrentPrediction().getEntityAnnotations()) {
			addFactorRecursive(factors, state.getInstance(), entity.rootClassType, entity.getAnnotationInstance());
		}

		return factors;
	}

	private void addFactorRecursive(List<Scope> factors, OBIEInstance internalInstance,
			Class<? extends IOBIEThing> rootClassType, IOBIEThing scioClass) {

		if (scioClass == null)
			return;

		/*
		 * TODO: include data Type properties?
		 */
		// if
		// (!scioClass.getClass().isAnnotationPresent(DataTypeProperty.class)) {
		final Set<String> surfaceForms = getSurfaceForms(internalInstance, scioClass);

		if (surfaceForms != null) {
			final Set<Class<? extends IOBIEThing>> influencedVariables = new HashSet<>();
			// influencedVariables.add(scioClass.getClass());
			factors.add(new Scope(influencedVariables, scioClass.getClass(), surfaceForms, rootClassType));
		}
		// }

		/*
		 * Add factors for object type properties.
		 */
		ReflectionUtils.getDeclaredOntologyFields(scioClass.getClass()).forEach(field -> {
			try {
				if (field.isAnnotationPresent(RelationTypeCollection.class)) {
					for (IOBIEThing element : (List<IOBIEThing>) field.get(scioClass)) {
						addFactorRecursive(factors, internalInstance, rootClassType, element);
					}
				} else {
					addFactorRecursive(factors, internalInstance, rootClassType, (IOBIEThing) field.get(scioClass));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return;

	}

	/**
	 * Returns the surface forms of the given object. If distant supervision is
	 * enabled all surface forms are returned that belongs to the class type of
	 * the given object.If DV is not enabled the returned set contains only the
	 * annotated surface form of the given object.
	 * 
	 * @param internalInstance
	 * 
	 * @param filler
	 * @return null if there are no annotations for that class
	 */
	private Set<String> getSurfaceForms(OBIEInstance internalInstance, final IOBIEThing filler) {

		if (filler == null)
			return null;

		Set<String> surfaceForms;
		if (enableDistantSupervision) {
			/*
			 * If DV is enabled add all surface forms of that class.
			 */
			if (internalInstance.getNamedEntityLinkingAnnotations().containsAnnotations(filler.getClass())) {
				if (filler.getClass().isAnnotationPresent(DatatypeProperty.class)) {
					surfaceForms = new HashSet<>();
					surfaceForms.add(normalizeSurfaceForm(filler.getTextMention()));
				} else {
					surfaceForms = internalInstance.getNamedEntityLinkingAnnotations().getAnnotations(filler.getClass()).stream()
							.map(nera -> nera.textMention).collect(Collectors.toSet());
				}
			} else {
				return null;
			}
		} else {
			/*
			 * If DV is not enabled add just the surface form of that individual
			 * annotation.
			 */
			surfaceForms = new HashSet<>();
			if (filler.getClass().isAnnotationPresent(DatatypeProperty.class)) {
				// surfaceForms.add(((IDataType) filler).getValue());
				surfaceForms.add(normalizeSurfaceForm(filler.getTextMention()));
			} else {
				surfaceForms.add(filler.getTextMention());
			}

		}
		return surfaceForms;
	}

	private String normalizeSurfaceForm(String textMention) {
		return textMention.replaceAll("[0-9]", "#").replaceAll("[^\\x20-\\x7E]+", "§");
	}

	@Override
	public void computeFactor(Factor<Scope> factor) {

		Vector featureVector = factor.getFeatureVector();

		final Class<? extends IOBIEThing> classType = factor.getFactorScope().classType;

		final Set<String> surfaceForms = factor.getFactorScope().surfaceForms;

		for (String surfaceForm : surfaceForms) {
			getTokenNgrams(featureVector, classType, surfaceForm);
		}

	}

	private void getTokenNgrams(Vector featureVector, Class<? extends IOBIEThing> classType, String cleanedMention) {

		final String cM = START_CIRCUMFLEX + TOKEN_SPLITTER_SPACE + cleanedMention + TOKEN_SPLITTER_SPACE + END_DOLLAR;

		final String[] tokens = cM.split(TOKEN_SPLITTER_SPACE);

		final int maxNgramSize = tokens.length;
		final String className = classType.getSimpleName();

		featureVector.set(LEFT + className + RIGHT + TOKEN_SPLITTER_SPACE + cM, true);

		if (classType.isAnnotationPresent(DatatypeProperty.class))
			return;

		for (int ngram = 1; ngram < maxNgramSize; ngram++) {
			for (int i = 0; i < maxNgramSize - 1; i++) {

				/*
				 * Do not include start symbol.
				 */
				if (i + ngram == 1)
					continue;

				/*
				 * Break if size exceeds token length
				 */
				if (i + ngram > maxNgramSize)
					break;

				StringBuffer fBuffer = new StringBuffer();
				for (int t = i; t < i + ngram; t++) {

					if (tokens[t].isEmpty())
						continue;

					if (BasicRegExPattern.STOP_WORDS.contains(tokens[t].toLowerCase()))
						continue;

					fBuffer.append(tokens[t]).append(TOKEN_SPLITTER_SPACE);

				}

				final String featureName = fBuffer.toString().trim();

				if (featureName.length() < MIN_TOKEN_LENGTH)
					continue;

				if (featureName.isEmpty())
					continue;

				featureVector.set(LEFT + className + RIGHT + TOKEN_SPLITTER_SPACE + featureName, true);

			}
		}

	}

}
