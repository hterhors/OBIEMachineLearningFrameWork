package de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.OntologyModelContent;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.RelationTypeCollection;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.tokenizer.Token;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.param.OBIERunParameter;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.DocumentClassificationTemplate.Scope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.templates.scope.OBIEFactorScope;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.EntityAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEInstance;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import factors.Factor;
import learning.Vector;

/**
 * only this template
 * 
 * Mean(n=138): model=1.248880181258857; objective=0.33594116520360173 Mean real
 * performance: 0.31447035145447844
 * 
 * @author hterhors
 *
 * @date Nov 3, 2017
 */
public class DocumentClassificationTemplate extends AbstractOBIETemplate<Scope> implements Serializable {

	public DocumentClassificationTemplate(OBIERunParameter parameter) {
		super(parameter);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(DocumentClassificationTemplate.class.getName());

	class Scope extends OBIEFactorScope {

		final OBIEInstance instance;
		final String className;

		public Scope(Set<Class<? extends IOBIEThing>> influencedVariable,
				Class<? extends IOBIEThing> entityRootClassType, AbstractOBIETemplate<?> template,
				OBIEInstance instance, final String className) {
			super(influencedVariable, entityRootClassType, template, instance, className, entityRootClassType);
			this.instance = instance;
			this.className = className;
		}

	}

	@Override
	public List<Scope> generateFactorScopes(OBIEState state) {
		List<Scope> factors = new ArrayList<>();
		state.getInstance().getTokens();

		for (EntityAnnotation entity : state.getCurrentPrediction().getEntityAnnotations()) {

			factors.addAll(
					addFactorRecursive(entity.rootClassType, state.getInstance(), entity.getAnnotationInstance()));

		}
		return factors;
	}

	private List<Scope> addFactorRecursive(Class<? extends IOBIEThing> entityRootClassType, OBIEInstance instance,
			IOBIEThing scioClass) {

		if (scioClass == null)
			return Collections.emptyList();

		if (scioClass.getClass().isAnnotationPresent(DatatypeProperty.class))
			return Collections.emptyList();

		List<Scope> factors = new ArrayList<>();

		final String className = scioClass.getClass().getSimpleName();

		final Set<Class<? extends IOBIEThing>> influencedVariables = new HashSet<>();
		// influencedVariables.add(scioClass.getClass());

		factors.add(new Scope(influencedVariables, entityRootClassType, this, instance, className));
		/*
		 * Add factors for object type properties.
		 */
		Arrays.stream(scioClass.getClass().getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
					field.setAccessible(true);
					try {
						if (field.isAnnotationPresent(RelationTypeCollection.class)) {
							for (IOBIEThing element : (List<IOBIEThing>) field.get(scioClass)) {
								factors.addAll(addFactorRecursive(entityRootClassType, instance, element));
							}
						} else {
							factors.addAll(addFactorRecursive(entityRootClassType, instance,
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

		List<Token> tokens = factor.getFactorScope().instance.getTokens();

		String className = factor.getFactorScope().className;

		for (Token token : tokens) {
			featureVector.set(className + "_" + token.getText(), true);
		}

	}

}
