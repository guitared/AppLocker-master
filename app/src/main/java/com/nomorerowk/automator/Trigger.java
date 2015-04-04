package com.nomorerowk.automator;

public abstract class Trigger {

	/**
	 * 
	 * @return True if this trigger matches the criteria
	 */
	public abstract boolean matches(String[] criteria);

	/**
	 * The type of trigger represents<br>
	 * When searching for matching triggers, only triggers of the same type will
	 * be called
	 */
	public abstract String getType();

}
