package de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.OntologyAnalyzer;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.OntologyModelContent;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.RelationTypeCollection;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.param.OBIERunParameter;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.PropertyEvidenceForClassTemplate.Scope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.scope.OBIEFactorScope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.EntityAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEInstance;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import factors.Factor;
import learning.Vector;

/**
 * 
 * @author hterhors
 *
 *         Apr 24, 2017
 */
public class PropertyEvidenceForClassTemplate extends AbstractOBIETemplate<Scope> {

	public PropertyEvidenceForClassTemplate(OBIERunParameter parameter) {
		super(parameter);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(PropertyEvidenceForClassTemplate.class.getName());

	/**
	 * Captures cardinality of evidences in text.
	 */
	final private static String TEMPLATE_0 = "Textual_evidence_for_%s_in_%s exists";

	class Scope extends OBIEFactorScope {
		final OBIEInstance instance;
		final Class<? extends IOBIEThing> rootClass;
		final Class<? extends IOBIEThing> relatedClassType;

		public Scope(Set<Class<? extends IOBIEThing>> influencedVariables, Class<? extends IOBIEThing> rootClass,
				AbstractOBIETemplate<?> template, OBIEInstance instance,
				Class<? extends IOBIEThing> relatedClassType) {
			super(influencedVariables, rootClass, template, instance, relatedClassType, rootClass);
			this.instance = instance;
			this.rootClass = rootClass;
			this.relatedClassType = relatedClassType;
		}
	}

	@Override
	public List<Scope> generateFactorScopes(OBIEState state) {
		List<Scope> factors = new ArrayList<>();
		for (EntityAnnotation entity : state.getCurrentPrediction().getEntityAnnotations()) {
			addFactors(factors, entity.rootClassType, state.getInstance(), entity.getAnnotationInstance());
		}
		return factors;
	}

	private void addFactors(List<Scope> factors, Class<? extends IOBIEThing> entityRootClassType,
			OBIEInstance psinkDocument, final IOBIEThing rootClass) {

		Arrays.stream(rootClass.getClass().getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
					try {
						field.setAccessible(true);
						if (field.isAnnotationPresent(RelationTypeCollection.class)) {

							if (field.isAnnotationPresent(DatatypeProperty.class)) {
							} else {

							}

						} else {

							final IOBIEThing property = ((IOBIEThing) field.get(rootClass));

							if (property != null) {

								final Set<Class<? extends IOBIEThing>> influencedVariables = new HashSet<>();
								influencedVariables.add(rootClass.getClass());

								/*
								 * Add feature class type of the field.
								 */
								factors.add(new Scope(influencedVariables, entityRootClassType, this, psinkDocument,
										rootClass.getClass()));
								addFactors(factors, entityRootClassType, psinkDocument, property);
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				});

	}

	@Override
	public void computeFactor(Factor<Scope> factor) {
		Vector featureVector = factor.getFeatureVector();

		final Set<Class<? extends IOBIEThing>> relatedClasses = OntologyAnalyzer
				.getRelatedClassesTypesUnderRoot(factor.getFactorScope().relatedClassType);

		for (Class<? extends IOBIEThing> relatedClassType : relatedClasses) {
			if (relatedClassType.isAnnotationPresent(DatatypeProperty.class))
				continue;
			boolean evidenceExists = factor.getFactorScope().instance.getNamedEntityLinkingAnnotations()
					.containsAnnotations(relatedClassType);

			featureVector.set(String.format(TEMPLATE_0, relatedClassType.getSimpleName(),
					factor.getFactorScope().relatedClassType.getSimpleName()), evidenceExists);

		}

	}

}
