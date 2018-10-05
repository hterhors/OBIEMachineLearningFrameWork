package de.hterhors.obie.ml.explorer.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.j2objc.annotations.ReflectionSupport;

import de.hterhors.obie.core.ontology.AbstractOBIEIndividual;
import de.hterhors.obie.core.ontology.IndividualFactory;
import de.hterhors.obie.core.ontology.OntologyFieldNames;
import de.hterhors.obie.core.ontology.OntologyInitializer;
import de.hterhors.obie.core.ontology.annotations.DatatypeProperty;
import de.hterhors.obie.core.ontology.annotations.DirectSiblings;
import de.hterhors.obie.core.ontology.annotations.ImplementationClass;
import de.hterhors.obie.core.ontology.annotations.RelationTypeCollection;
import de.hterhors.obie.core.ontology.interfaces.IDatatype;
import de.hterhors.obie.core.ontology.interfaces.IOBIEThing;
import de.hterhors.obie.core.tools.visualization.graphml.templates.NamedIndividual;
import de.hterhors.obie.ml.utils.ReflectionUtils;
import de.hterhors.obie.ml.variables.INERLAnnotation;
import de.hterhors.obie.ml.variables.NERLClassAnnotation;
import de.hterhors.obie.ml.variables.NERLIndividualAnnotation;
import de.hterhors.obie.ml.variables.OBIEInstance;

/**
 * 
 * @author hterhors
 *
 * @date Dec 19, 2017
 */
public class ExplorationUtils {

	private static final Map<Class<? extends IOBIEThing>, Map<Class<? extends IOBIEThing>, Boolean>> isDifferentiableToAllSiblingsCache = new ConcurrentHashMap<>();

	private ExplorationUtils() {

	}

	/**
	 * This method returns true if the field is used as a auxiliary construct in the
	 * ontology. These are all properties that do not have any meaning but just
	 * collecting entities. One example is SuppliedCompound. This class combines the
	 * Compound and its Supplier but carries no additional information on its own.
	 * An entity is considered as auxiliary class if it is the only class that can
	 * be filled in a property. That means there are no subclasses or sibling
	 * classes. (except of DataTypeProperty and NamedIndividual, those are always no
	 * aux. types).
	 * 
	 * @param field representing the property
	 * @return true if the class is just a auxiliary construct class, else false.
	 */
	@SuppressWarnings("unchecked")
	public static boolean isAuxiliaryProperty(Class<? extends IOBIEThing> propertyInterface) {

		/**
		 * TODO: Check this in PSINK project!
		 * 
		 * WHAT happens if the incoming class is not an interface?
		 *
		 * Why return false if the class is not an interface?
		 */
		if (!propertyInterface.isInterface())
			throw new IllegalArgumentException("Exptected interface but class is of type class:" + propertyInterface);

		if (!propertyInterface.isAnnotationPresent(ImplementationClass.class))
			return false;

		final Class<? extends IOBIEThing> implClass = ReflectionUtils.getImplementationClass(propertyInterface);

		/*
		 * No NamedIndividual
		 */
		if (ReflectionUtils.isAnnotationPresent(implClass, DatatypeProperty.class))
			return false;
		/*
		 * No DataType
		 */
		try {
//			if (!((IndividualFactory<?>) implClass.getField(OntologyInitializer.INDIVIDUAL_FACTORY_FIELD_NAME)
//					.get(null)).getIndividuals().isEmpty())
			if (!((IndividualFactory<AbstractOBIEIndividual>) ReflectionUtils
					.getDeclaredFieldByName(implClass, OntologyInitializer.INDIVIDUAL_FACTORY_FIELD_NAME).get(null))
							.getIndividuals().isEmpty())
//			if (implClass.isAnnotationPresent(NamedIndividual.class))
				return false;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/**
		 * TODO: not just the first interface... or get interfaces by annotation... if a
		 * class if subclass of multiple classes then we need to iterate over all super
		 * classes.
		 */
		final Set<Class<? extends IOBIEThing>> rootImplClasses = ReflectionUtils.getSuperRootClasses(implClass);
		/*
		 * No siblings, grand siblings...
		 */

		/**
		 * TODO: Test multiple root types.
		 */
		for (Class<? extends IOBIEThing> rootImplClass : rootImplClasses) {

			if (!ReflectionUtils.getAssignableSubClasses(rootImplClass).isEmpty())
				return false;
		}
		return true;

	}

	/**
	 * 
	 * This is needed if the root class changes. e.g. from Ratmodel to CatModel
	 * transfer all sharing properties. Copies all fields from copyFromClass to
	 * copyToClass if it contains those fields.
	 * 
	 * Copies only ontologymodel fields. do not copy offset onset textmention and
	 * annotation id.
	 * 
	 * DO NOT COPY datatype value fields although it is content of the ontologymodel
	 * FIXME: WHY WHY WHY ??? !f.isAnnotationPresent(DataTypeProperty.class) &&
	 * 
	 * @param copyToClass
	 * @param copyFromClass
	 * @return
	 */
	public static <B extends IOBIEThing> IOBIEThing copyOntologyModelFields(IOBIEThing copyToClass, B copyFromClass) {

		List<Field> copyToFields = ReflectionUtils.getDeclaredOntologyFields(copyToClass.getClass()).stream()
				.filter(f -> !Modifier.isStatic(f.getModifiers())).collect(Collectors.toList());
		for (Field toField : copyToFields) {
			try {

				if (copyFromClass != null) {
					Field copyFromField = ReflectionUtils.getDeclaredFieldByName(copyFromClass.getClass(),
							toField.getName());
					if (copyFromField != null)
						toField.set(copyToClass, copyFromField.get(copyFromClass));
				} else {
					/*
					 * CHECKME: Set value to null only if it is not OneToMany
					 */
					if (!toField.isAnnotationPresent(RelationTypeCollection.class))
						toField.set(copyToClass, null);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return copyToClass;

	}

	/**
	 * Gets candidates based on the ontology. Returns all candidates that have at
	 * least one evidence in the text or is in the exploreWithoutEvidence list.
	 * 
	 * @param instance
	 * @param slotSuperType_interface              the class-type interface of the
	 *                                             slot
	 * @param exploreClassesWithoutTextualEvidence a set of classes that should be
	 *                                             explored without textual evidence
	 * @param typeCandidates                       if the candidate generation
	 *                                             should be only on ontological
	 *                                             level and not on mention level
	 * @return
	 */
	public static Set<IOBIEThing> getCandidates(OBIEInstance instance,
			Class<? extends IOBIEThing> slotSuperType_interface,
			Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence, boolean typeCandidates,
			boolean restrictExplorationOnConceptsInInstance) {

		final Set<IOBIEThing> candidates = new HashSet<>();

		if (!slotSuperType_interface.isAnnotationPresent(ImplementationClass.class)) {
			return candidates;
		}

		if (!IDatatype.class.isAssignableFrom(slotSuperType_interface)) {
			try {

				final Collection<AbstractOBIEIndividual> individuals = getCollectionOfindividuals(
						slotSuperType_interface);

				/*
				 * Get all possible candidates for individuals and filter by mentions in the
				 * text.
				 */
				for (AbstractOBIEIndividual individual : individuals) {

					if (typeCandidates) {
						addSlotTypeIndividualCandidates(instance,
								ReflectionUtils.getImplementationClass(slotSuperType_interface), candidates, individual,
								exploreClassesWithoutTextualEvidence, restrictExplorationOnConceptsInInstance);
					} else {
						addFillerIndividualCandidates(instance,
								ReflectionUtils.getImplementationClass(slotSuperType_interface), candidates, individual,
								exploreClassesWithoutTextualEvidence);
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * Add candidate for root super class
		 */
		if (typeCandidates) {
			addSlotTypeClassCandidates(instance, slotSuperType_interface, candidates,
					ReflectionUtils.getImplementationClass(slotSuperType_interface),
					exploreClassesWithoutTextualEvidence, restrictExplorationOnConceptsInInstance);
		} else {
			addFillerCandidates(instance, slotSuperType_interface, candidates,
					ReflectionUtils.getImplementationClass(slotSuperType_interface),
					exploreClassesWithoutTextualEvidence);
		}

		/*
		 * Get all possible candidates and filter by mentions in the text.
		 */
		for (Class<? extends IOBIEThing> slotFillerType : ReflectionUtils
				.getAssignableSubInterfaces(slotSuperType_interface)) {
			if (typeCandidates) {
				addSlotTypeClassCandidates(instance, slotSuperType_interface, candidates,
						ReflectionUtils.getImplementationClass(slotFillerType), exploreClassesWithoutTextualEvidence,
						restrictExplorationOnConceptsInInstance);
			} else {
				addFillerCandidates(instance, slotSuperType_interface, candidates,
						ReflectionUtils.getImplementationClass(slotFillerType), exploreClassesWithoutTextualEvidence);
			}
		}
		return candidates;
	}

	/**
	 * Given an ontological class this method returns a collection of all possible
	 * individuals that are of the class type.
	 * 
	 * @param slotSuperType_interface
	 * @return
	 * @throws IllegalAccessException
	 */
	private static Collection<AbstractOBIEIndividual> getCollectionOfindividuals(
			Class<? extends IOBIEThing> slotSuperType_interface) throws IllegalAccessException {
		final Class<? extends IOBIEThing> clazz = ReflectionUtils.getImplementationClass(slotSuperType_interface);

		@SuppressWarnings("unchecked")
		final IndividualFactory<AbstractOBIEIndividual> individualFactory = (IndividualFactory<AbstractOBIEIndividual>) ReflectionUtils
				.getDeclaredFieldByName(clazz, OntologyInitializer.INDIVIDUAL_FACTORY_FIELD_NAME).get(null);

		final Collection<AbstractOBIEIndividual> individuals = individualFactory.getIndividuals();
		return individuals;
	}

	private static void addSlotTypeClassCandidates(OBIEInstance instance, Class<? extends IOBIEThing> slotSuperType,
			Set<IOBIEThing> candidates, Class<? extends IOBIEThing> slotFillerType,
			Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence,
			boolean restrictExplorationOnConceptsInInstance) {

		boolean keepClass = includeClassForSampling(slotFillerType);

		if (!keepClass)
			return;

		/*
		 * If the class can be differentiated between all of its siblings, then we can
		 * create this class without evidence in the text because there is a different
		 * property which makes this class important.
		 *
		 *
		 * If baseClassType is of type that should be sampled anyway even without
		 * textual evidence, add all direct candidates.
		 */

		/*
		 * We need to check whether a child class of that class can be an aux class.
		 * This happens if the direct interface is not an aux class but has subclasses
		 * that are.
		 */
		if (slotFillerType != null && exploreClassesWithoutTextualEvidence.contains(slotFillerType)
				|| (isDifferentiableToAllSiblingClasses(slotFillerType, slotSuperType)
						|| isAuxiliaryProperty(slotSuperType))) {
			/**
			 * TESTME: Is it sufficient to create just a single state with this class.
			 * Otherwise create a state for each mention in the text. (This should be only
			 * necessary if the position or text of this "auxiliary" class is important.
			 */
			try {

				Field annotationIDField = ReflectionUtils.getDeclaredFieldByName(slotFillerType,
						OntologyFieldNames.ANNOTATION_ID_FIELD_NAME);

				if (annotationIDField != null) {

					IOBIEThing newInstance = createNewInstance(slotFillerType);

					annotationIDField.set(newInstance, UUID.randomUUID().toString());

					candidates.add(newInstance);
				}
			} catch (InstantiationException | IllegalAccessException | SecurityException e) {
				e.printStackTrace();
			}
		} else {
			/**
			 * If not we need explicit text mentions to create this class.
			 */
			/*
			 * 
			 * Early pruning! Do not generate state where absolutely no evidence is in the
			 * text. for either classes or individuals
			 *
			 */
			if (restrictExplorationOnConceptsInInstance && (slotFillerType != null
					&& !instance.getNamedEntityLinkingAnnotations().containsClassAnnotations(slotFillerType))) {
				return;
			}

			/*
			 * If the type is data type property then create an annotation instance for each
			 * mention in the text.
			 */
			if (ReflectionUtils.isAnnotationPresent(slotFillerType, DatatypeProperty.class)) {
				/**
				 * 
				 */
				for (NERLClassAnnotation nera : instance.getNamedEntityLinkingAnnotations()
						.getClassAnnotationsBySemanticValues(slotFillerType)) {
					try {
						IOBIEThing newInstance = createNewInstance(slotFillerType);
						fillBasicFields(newInstance, nera);
						fillSemanticInterpretationField(newInstance, nera.getDTValueIfAnyElseTextMention());
						fillIDField(newInstance, nera.annotationID);
						candidates.add(newInstance);
					} catch (InstantiationException | IllegalAccessException | SecurityException
							| NoSuchFieldException e) {
						e.printStackTrace();
					}
				}

			} else {
				/*
				 * Else create exactly one instance without textual reference.
				 */
				try {
					IOBIEThing newInstance = createNewInstance(slotFillerType);
					fillIDField(newInstance, UUID.randomUUID().toString());
					candidates.add(newInstance);
				} catch (InstantiationException | IllegalAccessException | SecurityException | NoSuchFieldException
						| IllegalArgumentException e) {
					e.printStackTrace();
				}
			}

		}
	}

	private static void addFillerCandidates(OBIEInstance psinkDocument,
			Class<? extends IOBIEThing> baseClassType_interface, Set<IOBIEThing> candidates,
			Class<? extends IOBIEThing> candidateType_class,
			Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence) {

		boolean keepClass = includeClassForSampling(candidateType_class);

		if (!keepClass)
			return;

		/*
		 * If the class can be differentiated between all of its siblings, then we can
		 * create this class without evidence in the text because there is a different
		 * property which makes this class important.
		 *
		 *
		 * If baseClassType is of type that should be sampled anyway even without
		 * textual evidence, add all direct candidates.
		 */

		/*
		 * We need to check whether a child class of that class can be an aux class.
		 * This happens if the direct interface is not an aux class but has subclasses
		 * that are.
		 */
		if (exploreClassesWithoutTextualEvidence.contains(candidateType_class)
				|| (isDifferentiableToAllSiblingClasses(candidateType_class, baseClassType_interface)
						|| isAuxiliaryProperty(baseClassType_interface))) {
			/**
			 * TESTME: Is it sufficient to create just a single state with this class.
			 * Otherwise create a state for each mention in the text. (This should be only
			 * necessary if the position or text of this "auxiliary" class is important.
			 */
			try {
				Field annotationIDField = ReflectionUtils.getDeclaredFieldByName(candidateType_class,
						OntologyFieldNames.ANNOTATION_ID_FIELD_NAME);

				if (annotationIDField != null) {

					IOBIEThing newInstance = createNewInstance(candidateType_class);
					annotationIDField.set(newInstance, UUID.randomUUID().toString());

					candidates.add(newInstance);
				}
			} catch (InstantiationException | IllegalAccessException | SecurityException e) {
				e.printStackTrace();
			}
		} else {

			/**
			 * TODO: should this be executed always or just in else? Create annotation for
			 * classes that do not need evidences?
			 */
			/**
			 * If not we need explicit text mentions to create this class.
			 */
			Set<NERLClassAnnotation> possibleNERAnnotations = psinkDocument.getNamedEntityLinkingAnnotations()
					.getClassAnnotations(candidateType_class);
			/*
			 * 
			 * Early pruning! Do not generate state where absolutely no evidence is in the
			 * text.
			 *
			 */
			if (possibleNERAnnotations == null) {
				return;
			}

			for (NERLClassAnnotation nera : possibleNERAnnotations) {
				try {

					IOBIEThing newInstance = createNewInstance(candidateType_class);

					fillBasicFields(newInstance, nera);
					fillIDField(newInstance, nera.annotationID);
					fillSemanticInterpretationField(newInstance, nera.getDTValueIfAnyElseTextMention());
					candidates.add(newInstance);
				} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void addSlotTypeIndividualCandidates(OBIEInstance instance,
			Class<? extends IOBIEThing> slotSuperType, Set<IOBIEThing> candidates,
			AbstractOBIEIndividual individualCandidate,
			Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence,
			final boolean restrictExplorationOnConceptsInInstance) {

		if (individualCandidate == null)
			return;

		boolean keepIndividual = includeIndividualForSampling(individualCandidate);

		if (!keepIndividual)
			return;

		/**
		 * If not we need explicit text mentions to create this class.
		 */

		/*
		 * Early pruning! Do not generate state where absolutely no evidence is in the
		 * text. for either classes or individuals
		 */
		if (restrictExplorationOnConceptsInInstance
				&& !instance.getNamedEntityLinkingAnnotations().containsIndividualAnnotations(individualCandidate)) {
			return;
		}

		/*
		 * Else create exactly one instance without textual reference.
		 */
		try {

			IOBIEThing newInstance = newIndividual(slotSuperType, individualCandidate);
			fillIDField(newInstance, UUID.randomUUID().toString());
			candidates.add(newInstance);

		} catch (InstantiationException | IllegalAccessException | SecurityException | NoSuchFieldException
				| IllegalArgumentException e) {
			e.printStackTrace();
		}

	}

	private static void fillIDField(IOBIEThing newInstance, String annotationID)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// Field annotationIDField =
		// newInstance.getClass().getDeclaredField(AbstractOntologyEnvironment.ANNOTATION_ID_FIELD);
		// annotationIDField.setAccessible(true);

		Field annotationIDField = ReflectionUtils.getDeclaredFieldByName(newInstance.getClass(),
				OntologyFieldNames.ANNOTATION_ID_FIELD_NAME);
		annotationIDField.set(newInstance, annotationID);
	}

	private static void addFillerIndividualCandidates(OBIEInstance instance,
			Class<? extends IOBIEThing> baseClassType_class, Set<IOBIEThing> candidates,
			AbstractOBIEIndividual individual, Set<Class<? extends IOBIEThing>> exploreClassesWithoutTextualEvidence) {

		boolean keepIndividual = includeIndividualForSampling(individual);

		if (!keepIndividual)
			return;

		/**
		 * If not we need explicit text mentions to create this class.
		 */
		Set<NERLIndividualAnnotation> possibleNERAnnotations = instance.getNamedEntityLinkingAnnotations()
				.getIndividualAnnotations(individual);
		/*
		 * 
		 * Early pruning! Do not generate state where absolutely no evidence is in the
		 * text.
		 *
		 */
		if (possibleNERAnnotations == null) {
			return;
		}

		for (NERLIndividualAnnotation nera : possibleNERAnnotations) {
			try {
				IOBIEThing newThing = newIndividual(baseClassType_class, individual);
				fillBasicFields(newThing, nera);
				fillIDField(newThing, nera.annotationID);
				candidates.add(newThing);
			} catch (InstantiationException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
		}
	}

	private static void fillSemanticInterpretationField(IOBIEThing newInstance, String dtOrTextValue)
			throws IllegalArgumentException, IllegalAccessException {
		if (ReflectionUtils.isAnnotationPresent(newInstance.getClass(), DatatypeProperty.class)) {

			Field scioValueField = ReflectionUtils.getDeclaredFieldByName(newInstance.getClass(),
					OntologyFieldNames.SEMANTIC_VALUE_FIELD_NAME);
			scioValueField.set(newInstance, dtOrTextValue);
		}
	}

	/**
	 * Creates a new instance of the given class type. The new instance is then pre
	 * filled with properties that are auxiliarily classes.
	 * 
	 * @param ipsinkThing
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static IOBIEThing createNewInstance(Class<? extends IOBIEThing> ipsinkThing)
			throws InstantiationException, IllegalAccessException {
		IOBIEThing newInstance = (IOBIEThing) ipsinkThing.newInstance();

		for (Field field : ReflectionUtils.getDeclaredOntologyFields(newInstance.getClass())) {

			// if (!field.isAnnotationPresent(OntologyModelContent.class))
			// continue;
			//
			// field.setAccessible(true);
			/*
			 * NOTE: Pre fill auxiliary fields as default.
			 */
			if (isAuxiliaryProperty((Class<? extends IOBIEThing>) field.getType())) {
				field.set(newInstance, ReflectionUtils
						.getImplementationClass((Class<? extends IOBIEThing>) field.getType()).newInstance());
			}
		}
		return newInstance;
	}

	/**
	 * Creates a new instance of the given class type. The new instance is then pre
	 * filled with properties that are auxiliarily classes.
	 * 
	 * @param baseClassType_class
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static IOBIEThing newIndividual(Class<? extends IOBIEThing> baseClassType_class,
			AbstractOBIEIndividual individual) throws InstantiationException, IllegalAccessException {

		IOBIEThing newInstance = (IOBIEThing) baseClassType_class.newInstance();

		Field individualField = ReflectionUtils.getDeclaredFieldByName(baseClassType_class,
				OntologyInitializer.INDIVIDUAL_FIELD_NAME);
		individualField.set(newInstance, individual);

		for (Field field : ReflectionUtils.getDeclaredOntologyFields(newInstance.getClass())) {
			/*
			 * NOTE: Pre fill auxiliary fields as default.
			 */
			if (isAuxiliaryProperty((Class<? extends IOBIEThing>) field.getType())) {
				field.set(newInstance, ReflectionUtils
						.getImplementationClass((Class<? extends IOBIEThing>) field.getType()).newInstance());
			}
		}
		return newInstance;
	}

	private static void fillBasicFields(IOBIEThing genClass, INERLAnnotation nera)
			throws NoSuchFieldException, IllegalAccessException

	{

		// Field textMentionField =
		// genClass.getClass().getDeclaredField(AbstractOntologyEnvironment.SCIO_TEXT_MENTION);
		// textMentionField.setAccessible(true);

		Field textMentionField = ReflectionUtils.getDeclaredFieldByName(genClass.getClass(),
				OntologyFieldNames.TEXT_MENTION_FIELD_NAME);
		textMentionField.set(genClass, nera.getText());

		// Field offsetField =
		// genClass.getClass().getDeclaredField(AbstractOntologyEnvironment.CHARACTER_OFFSET_FIELD);
		// offsetField.setAccessible(true);

		Field offsetField = ReflectionUtils.getDeclaredFieldByName(genClass.getClass(),
				OntologyFieldNames.CHARACTER_OFFSET_FIELD_NAME);
		offsetField.set(genClass, Integer.valueOf(nera.getOnset() + nera.getText().length()));

		// Field onsetField =
		// genClass.getClass().getDeclaredField(AbstractOntologyEnvironment.CHARACTER_ONSET_FIELD);
		// onsetField.setAccessible(true);

		Field onsetField = ReflectionUtils.getDeclaredFieldByName(genClass.getClass(),
				OntologyFieldNames.CHARACTER_ONSET_FIELD_NAME);
		onsetField.set(genClass, Integer.valueOf(nera.getOnset()));
	}

	/**
	 * Checks whether the given class is mainly used as construction class in the
	 * ontology. E.g. CompoundTreatment. The CompoundTreatment carries all
	 * information in its properties but the class name does not have information to
	 * distinguish its purpose as e.g. KetaminTreatment would have.
	 * 
	 * This method returns true if the class has at least one property that has no
	 * sibling classes. This makes it differentiable to its siblings.
	 * 
	 * @param classType
	 * @param baseClassType_interface
	 * @return
	 */
	public static boolean isDifferentiableToAllSiblingClasses(Class<? extends IOBIEThing> classType,
			final Class<? extends IOBIEThing> baseClassType_interface) {

//		if (classType.isAnnotationPresent(NamedIndividual.class))
//			return false;

		if (ReflectionUtils.isAnnotationPresent(classType, DatatypeProperty.class))
			return false;

		if (isAuxiliaryProperty(baseClassType_interface))
			return false;

		if (!classType.isAnnotationPresent(DirectSiblings.class)) {
			return true;
		}

		if (isDifferentiableToAllSiblingsCache.containsKey(classType)
				&& isDifferentiableToAllSiblingsCache.get(classType).containsKey(baseClassType_interface))
			return isDifferentiableToAllSiblingsCache.get(classType).get(baseClassType_interface);

		isDifferentiableToAllSiblingsCache.putIfAbsent(classType, new HashMap<>());

		final List<Class<? extends IOBIEThing>> siblings = new ArrayList<>(
				ReflectionUtils.getDirectSiblings(classType));

		siblings.addAll(ReflectionUtils.getSuperRootClasses(classType).stream()
				.flatMap(c -> ReflectionUtils.getAssignableSubClasses(c).stream()).collect(Collectors.toList()));

		/*
		 * CHECKME: If there are no siblings it is basically distinguishable to all
		 * others.
		 */
		boolean isDifferentiableToAllSiblings = true;

		for (Class<? extends IOBIEThing> sibClass : siblings) {

			if (sibClass == classType)
				continue;

			if (!includeClassForSampling(sibClass))
				continue;

			final Set<String> sibPropertyNames = ReflectionUtils.getDeclaredOntologyFields(sibClass).stream()
					.map(f -> f.getName()).collect(Collectors.toSet());

			final Set<String> diffPropertyNames = ReflectionUtils.getDeclaredOntologyFields(classType).stream()
					.map(f -> f.getName()).filter(n -> !sibPropertyNames.contains(n)).collect(Collectors.toSet());
			// final Set<String> sibPropertyNames =
			// Arrays.stream(sibClass.getDeclaredFields())
			// .filter(f ->
			// f.isAnnotationPresent(OntologyModelContent.class)).map(f ->
			// f.getName())
			// .collect(Collectors.toSet());
			//
			// final Set<String> diffPropertyNames =
			// Arrays.stream(classType.getDeclaredFields())
			// .filter(f ->
			// f.isAnnotationPresent(OntologyModelContent.class)).map(f ->
			// f.getName())
			// .filter(n ->
			// !sibPropertyNames.contains(n)).collect(Collectors.toSet());

			isDifferentiableToAllSiblings &= !diffPropertyNames.isEmpty();

		}

		isDifferentiableToAllSiblingsCache.get(classType).put(baseClassType_interface, isDifferentiableToAllSiblings);
		return isDifferentiableToAllSiblings;

	}

	/**
	 * Hand made rules for including specific classes for sampling. These classes
	 * are all leaf classes from the ontology and some extra classes that are
	 * frequently annotated although they are not leaf classes.
	 * 
	 * EXTEND: Hand made rules for skipping/keeping the candidate classes for
	 * sampling.
	 * 
	 * @param ipsinkThing
	 * @return true if the input class can be used for sampling.
	 */
	private static boolean includeClassForSampling(Class<? extends IOBIEThing> ipsinkThing) {

		/*
		 * TODO: Specify: for now pass all
		 */
		// /*
		// * If no leaf class for input = class
		// */
		// if ((!ipsinkThing.isInterface() &&
		// ipsinkThing.isAnnotationPresent(AssignableSubClasses.class)))
		// return false;
		//
		// /*
		// * If no leaf class for input = interface
		// */
		// if (ipsinkThing.isInterface() &&
		// ipsinkThing.getAnnotation(ImplementationClass.class).implementationClass()
		// .isAnnotationPresent(AssignableSubClasses.class))
		// return false;

		return true;
	}

	private static boolean includeIndividualForSampling(AbstractOBIEIndividual individualCandidate) {
		// TODO Auto-generated method stub
		return true;
	}
}