package de.uni.bielefeld.sc.hterhors.psink.obie.ie.explorer;

import java.util.Objects;

import de.uni.bielefeld.sc.hterhors.psink.obie.core.ontology.interfaces.IOBIEThing;
import de.uni.bielefeld.sc.hterhors.psink.obie.ie.variables.OBIEState;

/**
 * Pair of the new generated state and and easy access to the changed OBIE
 * instance.
 * 
 * @author hterhors
 *
 * @date Apr 26, 2018
 */
final class StateInstancePair {

	public final OBIEState state;

	public final IOBIEThing instance;

	public StateInstancePair(OBIEState state, IOBIEThing instance) {
		Objects.requireNonNull(state);
		this.state = state;
		this.instance = instance;
	}

	@Override
	public String toString() {
		return "StateInstancePair [state=" + state + ", instance=" + instance + "]";
	}

}
