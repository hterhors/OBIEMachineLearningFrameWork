package de.uni.bielefeld.sc.hterhors.psink.obie.ie.run.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.DatatypeProperty;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.ImplementationClass;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.OntologyModelContent;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.annotations.RelationTypeCollection;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.core.utils.OBIEUtils;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.explorer.utils.ExplorationUtils;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.EntityAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.InstanceEntityAnnotations;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.NELAnnotation;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEInstance;

public class InitializerUtils {

	/*
	 * This is used for wrong initialization of a data type value. Its supposed to
	 * be always wrong.
	 */
	private static final String DEFAULT_WRONG_DT_VALUE = "####";

	private static Random rand = new Random(100L);

	/**
	 * The initial object defines the starting point of the sampling procedure.
	 * usually this should be an instance of the searchType. However you cann
	 * provide knowledge from the beginning.
	 * 
	 */

	public static IOBIEThing getEmptyInstance(Class<? extends IOBIEThing> searchType) {
		try {
			return searchType.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static IOBIEThing getFullRandomInstance(OBIEInstance instance, Class<? extends IOBIEThing> searchType) {
		try {
			IOBIEThing o = searchType.newInstance();
			fillRecursive(instance, o, true);
			return o;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static IOBIEThing getFullWrong(Class<? extends IOBIEThing> searchType) {
		try {
			IOBIEThing o = searchType.newInstance();
			fillRecursive(null, o, false);
			return o;
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Performs a full deep clone-copy of the given gold data.
	 * 
	 * @param instanceAnnotations
	 * @return
	 */
	public static Set<IOBIEThing> getFullCorrect(InstanceEntityAnnotations instanceAnnotations) {
		Set<IOBIEThing> set = new HashSet<>();
		for (EntityAnnotation goldAnnotation : instanceAnnotations.getEntityAnnotations()) {
			set.add(OBIEUtils.deepConstructorClone(goldAnnotation.getAnnotationInstance()));

		}
		return set;

	}

	@SuppressWarnings("unchecked")
	private static void fillRecursive(final OBIEInstance instance, final IOBIEThing object, final boolean random) {

		if (object == null)
			return;
		/*
		 * Add factors for object type properties.
		 */
		Arrays.stream(object.getClass().getDeclaredFields())
				.filter(f -> f.isAnnotationPresent(OntologyModelContent.class)).forEach(field -> {
					field.setAccessible(true);
					try {
						if (field.isAnnotationPresent(RelationTypeCollection.class)) {

							Class<? extends IOBIEThing> slotSuperType = ((Class<? extends IOBIEThing>) ((ParameterizedType) field
									.getGenericType()).getActualTypeArguments()[0]);
							if (slotSuperType.isAnnotationPresent(ImplementationClass.class)) {
								if (field.isAnnotationPresent(DatatypeProperty.class)) {
									slotSuperType = slotSuperType.getAnnotation(ImplementationClass.class).get();
									final IOBIEThing value = getValueForDT(instance, random, slotSuperType);
									((List<IOBIEThing>) field.get(object)).add(value);
								} else {
									final IOBIEThing value = getValueForNonDT(instance, random, slotSuperType);
									((List<IOBIEThing>) field.get(object)).add(value);
									fillRecursive(instance, value, random);
								}
							} else {
								throw new NotImplementedException(
										"Initialization can not be done. Can not handle fields with interface types that do not have an implementation class:"
												+ slotSuperType);
							}
						} else {
							Class<? extends IOBIEThing> slotSuperType = (Class<? extends IOBIEThing>) field.getType();
							if (slotSuperType.isAnnotationPresent(ImplementationClass.class)) {
								if (field.isAnnotationPresent(DatatypeProperty.class)) {
									slotSuperType = slotSuperType.getAnnotation(ImplementationClass.class).get();
									final IOBIEThing value = getValueForDT(instance, random, slotSuperType);
									field.set(object, value);
								} else {
									final IOBIEThing value = getValueForNonDT(instance, random, slotSuperType);
									field.set(object, value);
									fillRecursive(instance, value, random);
								}
							} else {
								throw new NotImplementedException(
										"Initialization can not be done. Can not handle fields with interface types that do not have an implementation class: "
												+ slotSuperType);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

	}

	private static IOBIEThing getValueForDT(OBIEInstance instance, boolean random,
			final Class<? extends IOBIEThing> slotSuperType)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		final String textValue;
		final String dtValue;
		if (random) {
			final Set<NELAnnotation> candidates = instance.getNamedEntityLinkingAnnotations().getAnnotations(slotSuperType);
			if (candidates != null && !candidates.isEmpty()) {
				int index = rand.nextInt(candidates.size());
				Iterator<NELAnnotation> iter = candidates.iterator();
				for (int i = 0; i < index; i++) {
					iter.next();
				}
				final NELAnnotation nera = iter.next();
				textValue = nera.textMention;
				dtValue = nera.getDTValueIfAnyElseTextMention();
			} else {
				textValue = null;
				dtValue = null;
			}
		} else {
			textValue = DEFAULT_WRONG_DT_VALUE;
			dtValue = DEFAULT_WRONG_DT_VALUE;
		}

		IOBIEThing value = slotSuperType.getConstructor(String.class, String.class, String.class)
				.newInstance(UUID.randomUUID().toString(), textValue, dtValue);
		return value;
	}

	private static IOBIEThing getValueForNonDT(OBIEInstance instance, boolean random,
			final Class<? extends IOBIEThing> slotSuperType) throws InstantiationException, IllegalAccessException {
		final IOBIEThing value;
		if (random) {
			Set<IOBIEThing> candidates = ExplorationUtils.getSlotFillerCandidates(instance, slotSuperType,
					new HashSet<>());
			if (candidates != null && !candidates.isEmpty()) {
				int index = rand.nextInt(candidates.size());
				Iterator<IOBIEThing> iter = candidates.iterator();
				for (int i = 0; i < index; i++) {
					iter.next();
				}
				value = iter.next();
			} else {
				value = null;
			}
		} else {
			value = slotSuperType.newInstance();
		}
		return value;
	}

}
