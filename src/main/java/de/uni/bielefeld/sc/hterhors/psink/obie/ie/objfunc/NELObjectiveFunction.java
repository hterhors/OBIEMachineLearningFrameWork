package de.uni.bielefeld.sc.hterhors.psink.obie.ie.objfunc;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IDataType;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.evaluation.evaluator.NamedEntityLinkingEvaluator;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.InstanceEntityAnnotations;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;
import learning.ObjectiveFunction;

public class NELObjectiveFunction extends ObjectiveFunction<OBIEState, InstanceEntityAnnotations>
		implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getFormatterLogger(NELObjectiveFunction.class.getName());

	@Override
	public double computeScore(OBIEState state, InstanceEntityAnnotations goldResult) {

		Set<String> gold = goldResult.getEntityAnnotations().stream().map(s -> {
			if (s.getAnnotationInstance().getClass().isAnnotationPresent(DatatypeProperty.class)) {
				return ((IDataType) s.getAnnotationInstance()).getSemanticValue();
			} else {
				return s.getAnnotationInstance().getTextMention();
			}
		}).collect(Collectors.toSet());

		Set<String> predictions = state.getCurrentPrediction().getEntityAnnotations().stream().map(s -> {
			if (s.getAnnotationInstance().getClass().isAnnotationPresent(DatatypeProperty.class)) {
				return ((IDataType) s.getAnnotationInstance()).getSemanticValue();
			} else {
				return s.getAnnotationInstance().getTextMention();
			}
		}).collect(Collectors.toSet());

		// System.out.println("gold = ");
		// gold.forEach(g -> System.out.println(g));
		// System.out.println("predictions = ");
//		predictions.forEach(p -> System.out.println(p));
		double s = NamedEntityLinkingEvaluator.f1(gold, predictions);
		// System.out.println("SCORE= " + s);

		return s;

	}

}
