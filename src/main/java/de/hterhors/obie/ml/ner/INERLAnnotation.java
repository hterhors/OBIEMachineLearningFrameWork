package de.hterhors.obie.ml.ner;

import java.io.Serializable;

public interface INERLAnnotation extends Serializable {

	public String getText();

	public int getOnset();

}
