package de.hterhors.obie.ml.scorer;

import java.util.List;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import exceptions.MissingFactorException;
import factors.Factor;
import factors.FactorScope;
import learning.Vector;
import learning.scorer.Scorer;
import utility.Utils;
import variables.AbstractState;

public class OBIEScorer implements Scorer {

	private static Logger log = LogManager.getFormatterLogger(OBIEScorer.class.getName());

	/**
	 * The scorer scores a state w.r.t. the model. It retrieves all factors related
	 * to the state and multiplies their individual scores. These individual factor
	 * scores are basically the exponential of the dot product of the feature values
	 * and the weights of the template of the factor: <i>exp(factor.features *
	 * factor.template.weights)</i> for all factors.
	 * 
	 * @param model
	 */
	public OBIEScorer() {
	}

	/**
	 * Computes a score for each passed state given the individual factors. Scoring
	 * is done is done in parallel if flag is set and scorer implementation does not
	 * override this method.
	 * 
	 * @param states
	 * @param multiThreaded
	 */
	public void score(List<? extends AbstractState<?>> states, boolean multiThreaded) {
//Time needed: 350271
//		for (AbstractState<?> s : states) {
//
//			try {
//				s.setModelScore(scoreSingleState(s.getFactorGraph().getFactors()));
//			} catch (MissingFactorException e) {
//				e.printStackTrace();
//			}
//		}

//		Time needed: 170222
		Stream<? extends AbstractState<?>> stream = Utils.getStream(states, multiThreaded);

		stream.forEach(s -> {
			try {
				s.setModelScore(scoreSingleState(s.getFactorGraph().getFactors()));
			} catch (MissingFactorException e) {
				e.printStackTrace();
			}
//			System.out.println(s);
		});
	}

	/**
	 * Computes the score of this state according to the trained model. The computed
	 * score is returned but also updated in the state objects <i>score</i> field.
	 * 
	 * @param list
	 * @return
	 */

	protected double scoreSingleState(List<Factor<? extends FactorScope>> factors) {

		if (factors.size() <= 0)
			return Double.MIN_VALUE;

//		Collection<Factor<?>> factors = null;
//		try {
//			factors = list.getFactorGraph().getFactors();
//		} catch (MissingFactorException e) {
//			e.printStackTrace();
//		}
		// final Map<Class<? extends IOBIEThing>, Double> influence = new
		// HashMap<>();

		// Map<String, Integer> counter = new HashMap<>();
		// for (Factor<?> factor : factors) {
		// final String key =
		// factor.getFactorScope().getTemplate().getClass().getSimpleName();
		// counter.put(key, counter.getOrDefault(key, 0) + 1);
		// }

		double score = 1;
		for (Factor<?> factor : factors) {
			// log.debug("factors: " + factor.getFeatureVector());
			// System.out.println("factors: " + factor.getFeatureVector());
			// final String key =
			// factor.getFactorScope().getTemplate().getClass().getSimpleName();
			Vector featureVector = factor.getFeatureVector();
			Vector weights = factor.getTemplate().getWeights();
			double dotProduct = featureVector.dotProduct(weights);// /
																	// counter.get(key);
			double factorScore = Math.exp(dotProduct);
			// System.out.println(factorScore);
			// log.debug(factorScore);
			score *= factorScore;// / counter.get(key);
		}
		// if (state instanceof OBIEState) {
		// ((OBIEState) state).setInfluence(influence);
		// }

		return score;

	}

}
