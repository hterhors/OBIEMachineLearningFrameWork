package de.hterhors.obie.ml.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.obie.core.ontology.ReflectionUtils;
import de.hterhors.obie.core.ontology.annotations.DatatypeProperty;
import de.hterhors.obie.core.ontology.annotations.OntologyModelContent;
import de.hterhors.obie.core.ontology.annotations.RelationTypeCollection;
import de.hterhors.obie.core.ontology.interfaces.IOBIEThing;
import de.hterhors.obie.core.tokenizer.Token;
import de.hterhors.obie.ml.run.AbstractOBIERunner;
import de.hterhors.obie.ml.templates.DocumentClassificationTemplate.Scope;
import de.hterhors.obie.ml.variables.IETmplateAnnotation;
import de.hterhors.obie.ml.variables.OBIEInstance;
import de.hterhors.obie.ml.variables.OBIEState;
import factors.Factor;
import factors.FactorScope;
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

	public DocumentClassificationTemplate(AbstractOBIERunner runner) {
		super(runner);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(DocumentClassificationTemplate.class.getName());

	class Scope extends FactorScope {

		final OBIEInstance instance;
		final String className;

		public Scope(Class<? extends IOBIEThing> entityRootClassType, AbstractOBIETemplate<?> template,
				OBIEInstance instance, final String className) {
			super(template, instance, className, entityRootClassType);
			this.instance = instance;
			this.className = className;
		}

	}

	@Override
	public List<Scope> generateFactorScopes(OBIEState state) {
		List<Scope> factors = new ArrayList<>();
		state.getInstance().getTokens();

		for (IETmplateAnnotation entity : state.getCurrentIETemplateAnnotations().getAnnotations()) {

			factors.addAll(addFactorRecursive(entity.rootClassType, state.getInstance(), entity.getThing()));

		}
		return factors;
	}

	private List<Scope> addFactorRecursive(Class<? extends IOBIEThing> entityRootClassType, OBIEInstance instance,
			IOBIEThing scioClass) {

		if (scioClass == null)
			return Collections.emptyList();

		if (scioClass.getIndividual() == null)
			return Collections.emptyList();

		if (ReflectionUtils.isAnnotationPresent(scioClass.getClass(), DatatypeProperty.class))
			return Collections.emptyList();

		List<Scope> factors = new ArrayList<>();

		final String className = scioClass.getIndividual().name;

		factors.add(new Scope(entityRootClassType, this, instance, className));
		/*
		 * Add factors for object type properties.
		 */
		Arrays.stream(scioClass.getClass().getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
					field.setAccessible(true);
					try {
						if (ReflectionUtils.isAnnotationPresent(field, RelationTypeCollection.class)) {
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
