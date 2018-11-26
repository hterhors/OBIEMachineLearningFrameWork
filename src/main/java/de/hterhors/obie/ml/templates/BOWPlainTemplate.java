package de.hterhors.obie.ml.templates;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hterhors.obie.core.ontology.annotations.DatatypeProperty;
import de.hterhors.obie.core.ontology.annotations.OntologyModelContent;
import de.hterhors.obie.core.ontology.annotations.RelationTypeCollection;
import de.hterhors.obie.core.ontology.interfaces.IOBIEThing;
import de.hterhors.obie.core.tokenizer.Token;
import de.hterhors.obie.ml.ner.regex.BasicRegExPattern;
import de.hterhors.obie.ml.run.param.RunParameter;
import de.hterhors.obie.ml.templates.BOWPlainTemplate.Scope;
import de.hterhors.obie.ml.utils.ReflectionUtils;
import de.hterhors.obie.ml.variables.OBIEInstance;
import de.hterhors.obie.ml.variables.OBIEState;
import de.hterhors.obie.ml.variables.TemplateAnnotation;
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
public class BOWPlainTemplate extends AbstractOBIETemplate<Scope> implements Serializable {

	public BOWPlainTemplate(RunParameter parameter) {
		super(parameter);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Logger log = LogManager.getFormatterLogger(BOWPlainTemplate.class.getName());

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

		for (TemplateAnnotation entity : state.getCurrentTemplateAnnotations().getTemplateAnnotations()) {

			addFactorRecursive(factors, entity.rootClassType, state.getInstance(), entity.getThing());

		}
		return factors;
	}

	private void addFactorRecursive(List<Scope> factors, Class<? extends IOBIEThing> entityRootClassType,
			OBIEInstance instance, IOBIEThing scioClass) {

		if (scioClass == null)
			return;

		if (ReflectionUtils.isAnnotationPresent(scioClass.getClass(), DatatypeProperty.class))
			return;

		if (scioClass.getIndividual() != null)
			factors.add(new Scope(entityRootClassType, this, instance, scioClass.getIndividual().name));
		/*
		 * Add factors for object type properties.
		 */

		ReflectionUtils.getSlots(scioClass.getClass()).forEach(slot -> {
			try {
				if (ReflectionUtils.isAnnotationPresent(slot, RelationTypeCollection.class)) {
					for (IOBIEThing element : (List<IOBIEThing>) slot.get(scioClass)) {
						addFactorRecursive(factors, entityRootClassType, instance, element);
					}
				} else {
					addFactorRecursive(factors, entityRootClassType, instance, (IOBIEThing) slot.get(scioClass));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return;
	}

	public static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(""));

	@Override
	public void computeFactor(Factor<Scope> factor) {

		final Vector featureVector = factor.getFeatureVector();

		final String className = factor.getFactorScope().className;

		for (Token token : factor.getFactorScope().instance.getTokens()) {

			if (BasicRegExPattern.STOP_WORDS.contains(token.getText().toLowerCase()))
				continue;
//
//			if (STOP_WORDS.contains(token.getText().toLowerCase()))
//				continue;

//			if (token.getText().matches(BasicRegExPattern.SPECIAL_CHARS))
//				continue;

//			featureVector.set(className + "_" + token.getText().toLowerCase(), true);
			featureVector.set("<"+className + "> " + token.getText(), true);

		}
//		if (BasicRegExPattern.STOP_WORDS.contains(token.getText().toLowerCase()))
//		featureVector.set(className + "_" + token.getText(), true);
//		10 fold cross validation mean: p: 0.670562744140625	r: 0.670562744140625	f1: 0.670562744140625
//		OFF-->0.3766233766233766
//		NOT-->0.8559670781893005
		
		
		//			featureVector.set(className + "_" + token.getText(), true);
//		10 fold cross validation mean: p: 0.737060546875	r: 0.737060546875	f1: 0.737060546875
//		OFF-->0.36363636363636365
//		NOT-->0.8683127572016461

//		if (BasicRegExPattern.STOP_WORDS.contains(token.getText().toLowerCase()))
//			featureVector.set(className + "_" + token.getText().toLowerCase(), true);
//		10 fold cross validation mean: p: 0.708984375	r: 0.708984375	f1: 0.708984375
//		OFF-->0.45454545454545453
//		NOT-->0.8477366255144033

//		featureVector.set(className + "_" + token.getText().toLowerCase(), true);
//		10 fold cross validation mean: p: 0.72796630859375	r: 0.72796630859375	f1: 0.72796630859375
//		OFF-->0.45454545454545453
//		NOT-->0.8600823045267489

	}

}
