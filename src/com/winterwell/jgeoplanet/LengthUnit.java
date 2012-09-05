package com.winterwell.jgeoplanet;

/**
 * @see Dx
 * @author Daniel
 */
public enum LengthUnit {

	METRE(1),
	KILOMETRE(1000),
	MILE(1609.344);

	/**
	 * Metres are the SI unit, so we use it for preference.
	 */
	public final double metres;
	
	/**
	 * The length period for 1 unit. Equivalent to new Dx(1, unit)
	 */
	public final Dx dx;

	private LengthUnit(double metres) {
		this.metres = metres;
		this.dx = new Dx(1, this);
	}

	/**
	 * @return The time period for 1 unit. Equivalent to new Dx(1, unit)
	 */
	public Dx getDx() {
		return dx;
	}


	public double getMetres() {
		return metres;
	}

	/**
	 * @deprecated Use {@link Dx#convertTo(LengthUnit)} for preference.
	 */
	@Deprecated
	public double convert(double amount, LengthUnit otherUnit) {
		Dx Dx2 = new Dx(amount, otherUnit);
		return Dx2.convertTo(this).getValue();
	}

}
