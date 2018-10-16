package de.hterhors.obie.ml.variables;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import de.hterhors.obie.ml.utils.OBIEClassFormatter;

public class InstanceTemplateAnnotations implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Since Entities only hold weak pointer via references to one another, using a
	 * Map is sensible to enable an efficient access to the entities.
	 */
	private final Map<String, TemplateAnnotation> entities = new HashMap<>();

	public InstanceTemplateAnnotations() {
	}

	/**
	 * Clone Constructor!
	 * 
	 * @param cloneFrom
	 */
	public InstanceTemplateAnnotations(InstanceTemplateAnnotations cloneFrom) {
		for (TemplateAnnotation e : cloneFrom.entities.values()) {
			this.entities.put(e.annotationID, new TemplateAnnotation(e));
		}
	}

	public TemplateAnnotation getEntity(String entityID) {
		return entities.get(entityID);
	}

	/**
	 * Returns the unmodifiable collection of entities.
	 * 
	 * @return
	 */
	public Collection<TemplateAnnotation> getTemplateAnnotations() {
		/**
		 * unmodifiableCollection
		 */
		return entities.values();
	}

	public void addAnnotation(TemplateAnnotation entity) {
		entities.put(entity.annotationID, entity);
	}

	public void removeEntity(TemplateAnnotation entity) {
		entities.remove(entity.annotationID);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entities == null) ? 0 : entities.hashCode());
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
		InstanceTemplateAnnotations other = (InstanceTemplateAnnotations) obj;
		if (entities == null) {
			if (other.entities != null)
				return false;
		} else if (!entities.equals(other.entities))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		for (TemplateAnnotation e : entities.values()) {
			builder.append("\n\t");
			builder.append(OBIEClassFormatter.format(e.getThing()
//					, investigationRestriction
			));
			builder.append("\n");
		}
		return builder.toString();

	}

}
