package de.hterhors.obie.ml.evaluation.evaluator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.hterhors.obie.core.evaluation.PRF1;
import de.hterhors.obie.core.ontology.InvestigationRestriction;
import de.hterhors.obie.core.ontology.interfaces.IOBIEThing;
import de.hterhors.obie.ml.evaluation.IOrListCondition;

/**
 * How to use this as evaluation but not extends abstract evaluator implementing
 * all methods that are not necessary here.
 * 
 * @author hterhors
 *
 */
public class StrictNamedEntityLinkingEvaluator implements IOBIEEvaluator {

	public StrictNamedEntityLinkingEvaluator() {
	}

	public static <E> double microPrecision(double tp, double fp, double fn) {
		return tp / (tp + fp);
	}

	public static <E> double microRecall(double tp, double fp, double fn) {
		return tp / (tp + fn);
	}

	public static <E> double microF1(double tp, double fp, double fn) {
		double p = microPrecision(tp, fp, fn);
		double r = microRecall(tp, fp, fn);
		double f1 = 2 * (p * r) / (p + r);
		return f1;
	}

	public static <E> int getTruePositives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return intersection.size();
	}

	public static <E> int getFalsePositives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);

		return result.size() - intersection.size();

	}

	public static <E> int getFalseNegatives(Set<E> gold, Set<E> result) {
		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);

		return gold.size() - intersection.size();

	}

	public static <E> double precision(Set<E> gold, Set<E> result) {

		if (result.size() == 0) {
			return 0;
		}

		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return (double) intersection.size() / result.size();

	}

	public static <E> double recall(Set<E> gold, Set<E> result) {

		if (gold.size() == 0) {
			return 0;
		}

		Set<E> intersection = new HashSet<E>(result);
		intersection.retainAll(gold);
		return (double) intersection.size() / gold.size();

	}

	public static <E> double f1(Set<E> gold, Set<E> result) {
		double p = precision(gold, result);
		double r = recall(gold, result);
		if (p == 0 && r == 0) {
			return 0;
		}
		double f1 = 2 * ((p * r) / (p + r));
		return f1;

	}

	@Override
	public PRF1 prf1(List<? extends IOBIEThing> gold, List<? extends IOBIEThing> predictions) {
		// Set<IOBIEThing> g = new HashSet<>(gold);
		// Set<IOBIEThing> p = new HashSet<>(predictions);

		int tp;
		int fp;
		int fn;
		// int intersectionSize = (int) gold.stream().filter(g ->
		// predictions.contains(g)).count();

		int intersectionSize = 0;

		for (IOBIEThing goldThing : gold) {
			intersectionSize += predictions.contains(goldThing) ? 1 : 0;
		}

		// final Set<IOBIEThing> intersection = new HashSet<>(p);
		// intersection.retainAll(g);

		// tp = intersection.size();
		// fp = p.size() - intersection.size();
		// fn = g.size() - intersection.size();

		tp = intersectionSize;
		fp = predictions.size() - intersectionSize;
		fn = gold.size() - intersectionSize;

		return new PRF1(tp, fp, fn);
	}

	@Override
	public PRF1 prf1(IOBIEThing gold, IOBIEThing predictions) {
		return prf1(Arrays.asList(gold), Arrays.asList(predictions));
	}

	@Override
	public double f1(IOBIEThing gold, IOBIEThing prediction) {
		return f1(new HashSet<>(Arrays.asList(gold)), new HashSet<>(Arrays.asList(prediction)));
	}

	@Override
	public double recall(IOBIEThing gold, IOBIEThing prediction) {
		return recall(new HashSet<>(Arrays.asList(gold)), new HashSet<>(Arrays.asList(prediction)));
	}

	@Override
	public double precision(IOBIEThing gold, IOBIEThing prediction) {
		return precision(new HashSet<>(Arrays.asList(gold)), new HashSet<>(Arrays.asList(prediction)));
	}

	@Override
	public double recall(List<? extends IOBIEThing> gold, List<? extends IOBIEThing> predictions) {
		return recall(new HashSet<>(gold), new HashSet<>(predictions));
	}

	@Override
	public double precision(List<? extends IOBIEThing> gold, List<? extends IOBIEThing> predictions) {
		return precision(new HashSet<>(gold), new HashSet<>(predictions));
	}

	@Override
	public double f1(List<? extends IOBIEThing> gold, List<? extends IOBIEThing> predictions) {
		return f1(new HashSet<>(gold), new HashSet<>(predictions));
	}

	/**
	 * TODO: they are obsolete here but how to model?
	 */

	@Override
	public boolean isIgnoreEmptyInstancesOnEvaluation() {
		return false;
	}

	@Override
	public boolean isPenalizeCardinality() {
		return false;
	}

	@Override
	public IOrListCondition getOrListCondition() {
		return null;
	}

	@Override
	public int getMaxEvaluationDepth() {
		return 0;
	}

	@Override
	public boolean isEnableCaching() {
		return false;
	}

	@Override
	public int getMaxNumberOfAnnotations() {
		return 0;
	}

	@Override
	public void clearCache() {
	}

}
