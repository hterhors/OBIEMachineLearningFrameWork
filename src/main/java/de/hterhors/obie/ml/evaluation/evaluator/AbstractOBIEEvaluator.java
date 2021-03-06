package de.hterhors.obie.ml.evaluation.evaluator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.hterhors.obie.core.evaluation.PRF1;
import de.hterhors.obie.core.ontology.InvestigationRestriction;
import de.hterhors.obie.core.ontology.ReflectionUtils;
import de.hterhors.obie.core.ontology.annotations.DatatypeProperty;
import de.hterhors.obie.core.ontology.annotations.RelationTypeCollection;
import de.hterhors.obie.core.ontology.instances.EmptyOBIEInstance;
import de.hterhors.obie.core.ontology.interfaces.IDatatype;
import de.hterhors.obie.core.ontology.interfaces.IOBIEThing;
import de.hterhors.obie.ml.evaluation.IOrListCondition;

public abstract class AbstractOBIEEvaluator implements IOBIEEvaluator {

	private static final int CLEAN_UP_PERIOD_IN_SEC = 5;

	protected final PRF1 zeroScore = new PRF1();

	protected final PRF1 perfectScore = new PRF1(1, 0, 0);

	private final PRF1 scoreForEmptyOrNullValues;

	protected final boolean penalizeCardinality;

//	protected InvestigationRestriction globalInvestigationRestrictions;

	/**
	 * This condition returns true for lists that matches the "OR"-list paradigm. An
	 * comparison of two "OR"-lists returns a score of 1 if the predicted list
	 * contains at least one element of the gold-list and no fps.
	 */
	protected IOrListCondition orListCondition = l -> false;

	/**
	 * Maximum depth for sub*-properties which should influence the evaluation
	 * score.
	 */
	protected final int maxEvaluationDepth;

	protected Map<CacheKey, PRF1> cache = new ConcurrentHashMap<>(1000000);
//	protected Map<CacheKey, SoftReference<PRF1>> cache = new ConcurrentHashMap<>(1000000);

	protected final boolean enableCaching;

	protected final int maxNumberOfAnnotations;

	/**
	 * Whether empty instances should be removed before evaluating or not.
	 */
	protected final boolean ignoreEmptyInstancesOnEvaluation;

	public static class CacheKey {
		final private IOBIEThing goldClass;
		final private IOBIEThing predictedClass;
//		final InvestigationRestriction samplingRestrictions;

		public CacheKey(IOBIEThing goldClass, IOBIEThing predictedClass
//				,				InvestigationRestriction investigationRestrictions
		) {
			this.goldClass = goldClass;
			this.predictedClass = predictedClass;
//			this.samplingRestrictions = investigationRestrictions;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((goldClass == null) ? 0 : goldClass.hashCode());
//			result = prime * result + ((samplingRestrictions == null) ? 0 : samplingRestrictions.hashCode());
			result = prime * result + ((predictedClass == null) ? 0 : predictedClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CacheKey other = (CacheKey) obj;
			if (goldClass == null) {
				if (other.goldClass != null)
					return false;
			} else if (!goldClass.equals(other.goldClass))
				return false;
//			if (samplingRestrictions == null) {
//				if (other.samplingRestrictions != null)
//					return false;
//			} else if (!samplingRestrictions.equals(other.samplingRestrictions))
//				return false;
			if (predictedClass == null) {
				if (other.predictedClass != null)
					return false;
			} else if (!predictedClass.equals(other.predictedClass))
				return false;
			return true;
		}

	}

	public AbstractOBIEEvaluator(boolean enableCaching, boolean penalizeCardinality
//			,		InvestigationRestriction propertyRestrictions
			, IOrListCondition orListCondition, int maxEvaluationDepth, final int maxNumberOfAnnotations,
			final boolean ignoreEmptyInstancesOnEvaluation) {
		this.enableCaching = enableCaching;
		this.penalizeCardinality = penalizeCardinality;
//		this.globalInvestigationRestrictions = propertyRestrictions;
		this.orListCondition = orListCondition;
		this.maxEvaluationDepth = maxEvaluationDepth;
		this.maxNumberOfAnnotations = maxNumberOfAnnotations;
		this.ignoreEmptyInstancesOnEvaluation = ignoreEmptyInstancesOnEvaluation;
		/*
		 * TODO: Parameter whether equal null values should be rewarded or not.
		 * zeroScore
		 */
		boolean countEmptyOrNullValues = false;

		this.scoreForEmptyOrNullValues = countEmptyOrNullValues ? perfectScore : zeroScore;
//		Thread cleanerThread = new Thread(() -> {
//			while (!Thread.currentThread().isInterrupted()) {
//				try {
//					Thread.sleep(CLEAN_UP_PERIOD_IN_SEC * 1000);
//					System.out.println("Autoremove Cache..");
//					cache.entrySet().removeIf(entry -> entry.getValue() == null);
//					System.out.println("done");
//				} catch (InterruptedException e) {
//					Thread.currentThread().interrupt();
//				}
//			}
//		});
//		cleanerThread.setDaemon(true);
//		cleanerThread.start();

	}

	public void clearCache() {
		cache.clear();
	}

	public boolean isPenalizeCardinality() {
		return penalizeCardinality;
	}

//	public InvestigationRestriction getInvestigationRestrictions() {
//		return globalInvestigationRestrictions;
//	}
//
//	public void setInvestigationRestrictions(InvestigationRestriction investigationRestrictions) {
//		this.globalInvestigationRestrictions = investigationRestrictions;
//	}

	public IOrListCondition getOrListCondition() {
		return orListCondition;
	}

	public int getMaxEvaluationDepth() {
		return maxEvaluationDepth;
	}

	public boolean isEnableCaching() {
		return enableCaching;
	}

	public int getMaxNumberOfAnnotations() {
		return maxNumberOfAnnotations;
	}

	public boolean isIgnoreEmptyInstancesOnEvaluation() {
		return ignoreEmptyInstancesOnEvaluation;
	}

	/**
	 * 
	 * @param goldInstance
	 * @param predictedInstance
	 * @param rootClassEvaluation whether the current comparison is on the root
	 *                            class.
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	protected PRF1 compareObjectWise(IOBIEThing goldInstance, IOBIEThing predictedInstance, int depth) {

		if (goldInstance == null && predictedInstance == null) {

			return scoreForEmptyOrNullValues;
		}

		/*
		 * Switch on to get only the scores for same amount of templates. Do not
		 * penalize the amount.
		 */
		if (!penalizeCardinality && (depth == 0 && (goldInstance == EmptyOBIEInstance.emptyInstance
				|| predictedInstance == EmptyOBIEInstance.emptyInstance)))
			return zeroScore;

//		System.out.println("******************************");
//		System.out.println("Compare: " + goldInstance);
//		System.out.println("With: " + predictedInstance);

		CacheKey ck;
		PRF1 score;
		if (enableCaching) {
			ck = new CacheKey(goldInstance, predictedInstance
//					, globalInvestigationRestrictions
			);

			if ((score = cache.get(ck)) != null) {
				return score;
			}
		} else {
			ck = null;
		}
//		if (enableCaching) {
//			ck = new CacheKey(goldInstance, predictedInstance, investigationRestrictions);
//			
//			SoftReference<PRF1> o;
//			if ((o = cache.get(ck)) != null && (score = o.get()) != null) {
//				return score;
//			}
//		} else {
//			ck = null;
//		}
		score = new PRF1();

		/*
		 * TODO: Check this necessary?
		 */
//		if (!goldInstance.getInvestigationRestriction().equals(predictedInstance.getInvestigationRestriction())) {
//			return zeroScore;
////			throw new IllegalStateException("Can not compare classes with different investigation restrictions!");
//		}

		if (goldInstance == null && predictedInstance != null) {
			/*
			 * If the gold instance does not have the specific field but the
			 * predictedInstance has!
			 */
			score.fp++;
		} else if (predictedInstance == null) {
			score.fn++;
		} else if (goldInstance != null && predictedInstance != null) {

			/*
			 * If class is the same and they are both data type properties
			 */
			if (ReflectionUtils.isAnnotationPresent(goldInstance.getClass(), DatatypeProperty.class)
					|| ReflectionUtils.isAnnotationPresent(predictedInstance.getClass(), DatatypeProperty.class)) {
				if (goldInstance.getClass().equals(predictedInstance.getClass())) {

					String predValue = ((IDatatype) predictedInstance).getInterpretedValue();

					if (predValue == null)
						predValue = predictedInstance.getTextMention();

					String goldValue = ((IDatatype) goldInstance).getInterpretedValue();
					if (goldValue == null)
						goldValue = goldInstance.getTextMention();

					if (predValue == null) {
//						if(goldValue==null) {
//							score.add(scoreForEmptyOrNullValues);
//						}else{
						/*
						 * This case happens only if the rootClassType is a data type class.
						 */
						score.fn++;
//						}
						return score;
					} else if (goldValue.equals(predValue)) {
						/*
						 * If both classes are same data type property and have the same value.
						 */
						score.tp++;
						return score;
					} else {
						if ((depth == 0 && goldInstance.getInvestigationRestriction().investigateClassType)
								|| depth != 0) {

							/*
							 * If they have not the same value.
							 */
							score.fp++;
							score.fn++;
							return score;
						}
					}
				} else {
					/*
					 * If they are not the same class type but both are data type properties.
					 */
					score.fp++;
					score.fn++;
					return score;
				}
			}

			/*
			 * If they are not data types investigate individual type
			 */
			if (checkForSameType(goldInstance, predictedInstance)) {
				if (depth == 0 && goldInstance.getInvestigationRestriction().investigateClassType || depth != 0) {
					/*
					 * If both classes are the same and no data type properties.
					 */
					if (ignoreEmptyInstancesOnEvaluation && predictedInstance.isEmpty())
						/*
						 * If the predicted instance is null and we want to ignore empty instances than
						 * deal it as its not existent.
						 */
						score.fn++;
					else
						// otherwise add +1 to true positive
						score.tp++;
				}
			} else {
				if (predictedInstance == EmptyOBIEInstance.emptyInstance) {
					// if (depth == 0 &&
					// investigationRestrictions.investigateClassType || depth
					// != 0) {
					score.fn++;
					// }
				} else if (goldInstance == EmptyOBIEInstance.emptyInstance) {
					// if (depth == 0 &&
					// investigationRestrictions.investigateClassType || depth
					// != 0) {
					if (!(ignoreEmptyInstancesOnEvaluation && predictedInstance.isEmpty()))
						score.fp++;
					// }
				} else if (depth == 0 && goldInstance.getInvestigationRestriction().investigateClassType
						|| depth != 0) {

					if (goldInstance.getIndividual() == null && predictedInstance.getIndividual() != null) {
						score.fp++;
					} else if (goldInstance.getIndividual() != null && predictedInstance.getIndividual() == null) {
						score.fn++;
					} else if (goldInstance.getIndividual() == null && predictedInstance.getIndividual() == null
							|| !goldInstance.getIndividual().equals(predictedInstance.getIndividual())) {
						score.fn++;
						score.fp++;
					}

				}
			}
		}
		// System.out.println("End score = " + score);

		if (depth == maxEvaluationDepth) {
			return score;
		} else {
			depth++;
		}

		/**
		 * TODO: At the moment we assume that two fields are equivalent if the name is
		 * same. This might be a problem if two completely different classes have fields
		 * that have the same name. Better here to check if the name is equal and both
		 * classes have the same super root class. e.g. RatModel and MouseModel.
		 */
		Set<String> goldFields;
		Set<String> predictionFields;

		if (goldInstance != null) {
			goldFields = ReflectionUtils.getSlotNames(goldInstance.getClass(),
					goldInstance.getInvestigationRestriction());
		} else {
			goldFields = new HashSet<>();
		}
		if (predictedInstance != null) {
			predictionFields = ReflectionUtils.getSlotNames(predictedInstance.getClass(),
					predictedInstance.getInvestigationRestriction());
		} else {
			predictionFields = new HashSet<>();
		}

		/*
		 * Loop over all fields that are in the gold class but not in predicted class.
		 */
		for (String fieldName : goldFields) {

			if (predictionFields.contains(fieldName))
				continue;

			Field goldField = ReflectionUtils.getAccessibleFieldByName(goldInstance.getClass(), fieldName);
			Field predictionField;

			if (predictedInstance == EmptyOBIEInstance.emptyInstance || predictedInstance == null) {
				predictionField = null;
			} else {
				predictionField = ReflectionUtils.getAccessibleFieldByName(predictedInstance.getClass(), fieldName);
			}

			score.add(loopOverFields(goldInstance, predictedInstance, goldField, predictionField, depth));
		}

		/*
		 * Loop over remaining fields
		 */
		for (String fieldName : predictionFields) {

			Field goldField;
			if (goldInstance == EmptyOBIEInstance.emptyInstance || goldInstance == null) {
				goldField = null;
			} else {
				goldField = ReflectionUtils.getAccessibleFieldByName(goldInstance.getClass(), fieldName);
			}
			final Field predictionField = ReflectionUtils.getAccessibleFieldByName(predictedInstance.getClass(),
					fieldName);

			score.add(loopOverFields(goldInstance, predictedInstance, goldField, predictionField, depth));

		}

		if (enableCaching) {
			cache.put(ck, score);
//			cache.put(ck, new SoftReference<PRF1>(score));
		}

		return score;
	}

	/**
	 * Checks whether the sampled class is of the same type.
	 *
	 * this is either if the classes are equal or if we work with individuals they
	 * need to be equal. As we use factories to get individuals we are able to
	 * compare them with == .
	 * 
	 * @param goldInstance
	 * @param predictedInstance
	 * @return
	 */
	private boolean checkForSameType(IOBIEThing goldInstance, IOBIEThing predictedInstance) {

		if (!goldInstance.getClass().equals(predictedInstance.getClass()))
			return false;

		if (goldInstance.getIndividual() == null && predictedInstance.getIndividual() == null)
			return true;

		if (goldInstance.getIndividual() == null && predictedInstance.getIndividual() != null)
			return false;

		if (goldInstance.getIndividual() != null && predictedInstance.getIndividual() == null)
			return false;

		return goldInstance.getIndividual().equals(predictedInstance.getIndividual());
	}

	@SuppressWarnings("unchecked")
	protected PRF1 loopOverFields(IOBIEThing goldClass, IOBIEThing predictedClass, Field goldField,
			Field predictionField, final int depth) {

		PRF1 score = new PRF1();

		PRF1 adderScore = new PRF1();

		/*
		 * goldField and predictionField are always equal or one of them is null!
		 */
		if (goldClass != null && goldField != null && predictedClass != null && predictionField != null
				&& goldClass != EmptyOBIEInstance.emptyInstance && predictedClass != EmptyOBIEInstance.emptyInstance
				&& goldClass.getInvestigationRestriction().investigateField(goldField.getName()) != predictedClass
						.getInvestigationRestriction().investigateField(predictionField.getName())) {
//			return FP;
			adderScore.add(FP);
		} else if ((goldField != null && ReflectionUtils.isAnnotationPresent(goldField, RelationTypeCollection.class))
				|| (predictionField != null
						&& ReflectionUtils.isAnnotationPresent(predictionField, RelationTypeCollection.class))) {

			List<IOBIEThing> goldList = null;
			List<IOBIEThing> predictionList = null;
			/*
			 * Might be null!!!
			 */
			if (goldClass != null && goldField != null) {
				try {
					goldList = (List<IOBIEThing>) goldField.get(goldClass);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				goldList = Collections.emptyList();
			}

			if (predictedClass != null && predictionField != null) {
				try {
					predictionList = (List<IOBIEThing>) predictionField.get(predictedClass);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			} else {
				predictionList = Collections.emptyList();
			}

			/*
			 * There are 4 Methods to calculate the F1 score!
			 * 
			 * 1) Cartesian-wise: If the list contains non-data type entities, such as
			 * AnimalModels and all entities are important it is necessary to calculate the
			 * Cartesian-wise.
			 * 
			 * 2) OrListCondition: Just a single element of the list is needed to get a
			 * F-score of 1.0 then we need to compute the orList score.
			 * 
			 * 3) List contains only DataType Properties than we can compute the standard f1
			 * score of sets.
			 *
			 * 4) List contains only data type properties but just a single element is
			 * needed. TODO: NOT IMPL YET
			 */
			/*
			 * If the field belongs to an or-list, we need a different method to calculate
			 * the similarity of those lists.
			 */
			if (goldList.isEmpty() && predictionList.isEmpty()) {
//				return scoreForEmptyOrNullValues;
				adderScore.add(scoreForEmptyOrNullValues);
			} else {

				if (predictionField != null
						&& ReflectionUtils.isAnnotationPresent(predictionField, DatatypeProperty.class)
						|| goldField != null
								&& ReflectionUtils.isAnnotationPresent(goldField, DatatypeProperty.class)) {
//					return standardSimilarity(goldList, predictionList);
					adderScore.add(standardSimilarity(goldList, predictionList));
				} else if ((predictionField != null && orListCondition.isTrue(predictionField))
						|| (goldField != null && orListCondition.isTrue(goldField))) {
//					return orListSimilarity(goldList, predictionList, depth);
					adderScore.add(orListSimilarity(goldList, predictionList, depth));
				} else {
//					return explore(goldList, predictionList, depth);
					adderScore.add(explore(goldList, predictionList, depth));
				}
			}

		} else {

			IOBIEThing gold = null;
			IOBIEThing pred = null;

//			if (goldField == null) {
//				gold = EmptyOBIEInstance.emptyInstance;
//				/*
//				 * TODO: penalize ?
//				 */
////				adderScore.add(FP);
//			} else if (predictionField == null ) {
//				gold = EmptyOBIEInstance.emptyInstance;
////				adderScore.add(FN);
//			} else {
			if (goldClass != null && goldField != null)
				try {
					gold = (IOBIEThing) goldField.get(goldClass);
				} catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			if (predictedClass != null && predictionField != null) {

				try {
					pred = (IOBIEThing) predictionField.get(predictedClass);
				} catch (NullPointerException | IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
//			return compareObjectWise(gold, pred, depth);
//			}
			adderScore.add(compareObjectWise(gold, pred, depth));
		}

//		System.out.println("adderScore = " + adderScore);

		score.add(adderScore);

		return score;

	}

	protected abstract PRF1 explore(List<? extends IOBIEThing> goldList, List<? extends IOBIEThing> predictedList,
			int depth);

	private final PRF1 FP = new PRF1(0, 1, 0);
	private final PRF1 FN = new PRF1(0, 0, 1);

	/**
	 * Compares the objects of two lists. This method calculates the max score
	 * within all pairs of those two lists. Thus it is not necessary to calculate
	 * the full Cartesian permutation but only each element of firstList with each
	 * element of secondList which results in a computational cost of n² instead of
	 * n!
	 * 
	 * Adds a penalty for each element in prediction that can not be found in gold.
	 * 
	 * @param goldList
	 * @param predictedList
	 * @return
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	private PRF1 orListSimilarity(final List<IOBIEThing> goldList, final List<IOBIEThing> predictedList,
			final int depth) {

		if (goldList == null && predictedList == null) {
			return zeroScore;
		}

		if (goldList == null) {
			return FP;
		}

		if (predictedList == null) {
			return FN;
		}

		if (goldList.isEmpty() && predictedList.isEmpty())
			return zeroScore;

		final PRF1 bestPermutationScore = new PRF1(0, 0, 1);
//		bestPermutationScore.fn++;

		int predictionPenalty = 0;

		for (final IOBIEThing pred : predictedList) {
			double bestLocalScore = 0;
			for (final IOBIEThing gold : goldList) {
				final PRF1 currentPermutationScore = compareObjectWise(gold, pred, depth);

				if (bestPermutationScore.getF1() <= currentPermutationScore.getF1()) {
					bestPermutationScore.set(currentPermutationScore);
				}
				bestLocalScore = Math.max(bestLocalScore, currentPermutationScore.getF1());
			}
			if (bestLocalScore == 0) {
				predictionPenalty++;
			}
		}
		bestPermutationScore.fp += predictionPenalty;
		return bestPermutationScore;
	}

	/**
	 * @param goldList
	 * @param predictionList
	 * @return
	 */
	private PRF1 standardSimilarity(List<IOBIEThing> goldList, List<IOBIEThing> predictionList) {

		final List<String> semanticGoldValues = new ArrayList<>();
		final List<String> semanticPredictedValues = new ArrayList<>();

		for (IOBIEThing gV : goldList) {
			semanticGoldValues.add(((IDatatype) gV).getInterpretedValue());
		}
		for (IOBIEThing pV : predictionList) {
			semanticPredictedValues.add(((IDatatype) pV).getInterpretedValue());
		}

		int tp;
		int fp;
		int fn;

		int intersectionSize = 0;

		for (String goldThing : semanticGoldValues) {
			intersectionSize += semanticPredictedValues.contains(goldThing) ? 1 : 0;
		}

		tp = intersectionSize;
		fp = semanticPredictedValues.size() - intersectionSize;
		fn = semanticGoldValues.size() - intersectionSize;

		return new PRF1(tp, fp, fn);

	}

}
