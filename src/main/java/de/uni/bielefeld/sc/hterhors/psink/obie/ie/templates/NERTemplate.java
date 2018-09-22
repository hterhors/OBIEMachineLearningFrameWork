package de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.OntologyModelContent;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.RelationTypeCollection;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.param.OBIERunParameter;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.NERTemplate.Scope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.scope.OBIEFactorScope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.EntityAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEInstance;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import factors.Factor;
import learning.Vector;

/**
 * Checks whether the annotated surface form was found by the NER-module. As
 * annotations for classes are only set by the NER findings, this template makes
 * no sense for the linking task. This template makes only sense for the NER
 * tasks in combination with the MultipleTokenBoundaryExplorer.
 * 
 * @author hterhors
 *
 * @date Nov 15, 2017
 */
public class NERTemplate extends AbstractOBIETemplate<Scope> {
	public NERTemplate(OBIERunParameter parameter) {
		super(parameter);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(NERTemplate.class.getName());

	class Scope extends OBIEFactorScope {

		public Class<? extends IOBIEThing> classType;
		public String surfaceForm;
		public OBIEInstance instance;

		public Scope(Set<Class<? extends IOBIEThing>> influencedVariables,
				Class<? extends IOBIEThing> annotationRootClassType, AbstractOBIETemplate<?> template,
				OBIEInstance instance, Class<? extends IOBIEThing> classType, String surfaceForm) {
			super(influencedVariables, annotationRootClassType, template, annotationRootClassType, instance, classType,
					surfaceForm);
			this.instance = instance;
			this.classType = classType;
			this.surfaceForm = surfaceForm;
		}

	}

	@Override
	public List<Scope> generateFactorScopes(OBIEState state) {
		List<Scope> factors = new ArrayList<>();
		for (EntityAnnotation entity : state.getCurrentPrediction().getEntityAnnotations()) {
			factors.addAll(
					addFactorRecursive(state.getInstance(), entity.rootClassType, entity.getAnnotationInstance()));
		}
		return factors;
	}

	private List<Scope> addFactorRecursive(OBIEInstance internalInstance,
			Class<? extends IOBIEThing> entityRootClassType, IOBIEThing scioClass) {
		List<Scope> factors = new ArrayList<>();

		if (scioClass == null)
			return factors;

		final String surfaceForm = scioClass.getTextMention();
		// System.out.println("--> in template: " + surfaceForm);

		if (surfaceForm != null) {
			final Set<Class<? extends IOBIEThing>> influencedVariables = new HashSet<>();
//			influencedVariables.add(scioClass.getClass());
			factors.add(new Scope(influencedVariables, entityRootClassType, this, internalInstance,
					scioClass.getClass(), surfaceForm));
		}

		/*
		 * Add factors for object type properties.
		 */
		if (!scioClass.getClass().isAnnotationPresent(DatatypeProperty.class))
			Arrays.stream(scioClass.getClass().getDeclaredFields())
					.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
						field.setAccessible(true);
						try {
							if (field.isAnnotationPresent(RelationTypeCollection.class)) {
								for (IOBIEThing element : (List<IOBIEThing>) field.get(scioClass)) {
									factors.addAll(addFactorRecursive(internalInstance, entityRootClassType, element));
								}
							} else {
								factors.addAll(addFactorRecursive(internalInstance, entityRootClassType,
										(IOBIEThing) field.get(scioClass)));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
		return factors;

	}

	@Override
	public void computeFactor(Factor<Scope> factor) {

		Vector featureVector = factor.getFeatureVector();

		if (!factor.getFactorScope().instance.getNamedEntityLinkingAnnotations()
				.containsAnnotations(factor.getFactorScope().classType))
			return;
		boolean foundByNER = factor.getFactorScope().instance.getNamedEntityLinkingAnnotations()
				.getAnnotations(factor.getFactorScope().classType).stream().map(e -> e.getDTValueIfAnyElseTextMention())
				.collect(Collectors.toSet()).contains(factor.getFactorScope().surfaceForm);

		featureVector.set(factor.getFactorScope().getEntityRootClassType().getSimpleName() + " - FoundByNER",
				foundByNER);
		featureVector.set(factor.getFactorScope().getEntityRootClassType().getSimpleName() + " - NotFoundByNER",
				!foundByNER);
	}

}
