package com.winterwell.jgeoplanet;



/**
 * A length with the unit. This class is immutable.
 * Lengths can be positive or negative (positive is more common).
 * <p>
 * Why? Because we don't want to crash into Mars:
 * http://articles.cnn.com/1999-09-30/tech/9909_30_mars.metric_1_mars-orbiter-climate-orbiter-spacecraft-team?_s=PM:TECH
 * 
 * @author Daniel
 *
 */
public final class Dx implements Comparable<Dx> {
	private static final long serialVersionUID = 1L;
	/**
	 * Right here. No distance at all.
	 */
	public static Dx ZERO() {
		// Tried todo as a constant - but the cyclic ref between KLength and Dx
		// caused an intermittent bug with a null unit in ZERO
		return new Dx(0, LengthUnit.METRE);
	}


	private final LengthUnit unit;
	private final double n;

	/**
	 * Convenience for a Dx measured in metres.
	 * @param metres
	 */
	public Dx(double metres) {
		this(metres, LengthUnit.METRE);
	}
	
	public Dx(double n, LengthUnit unit) {
		this.n = n;
		this.unit = unit;
		assert unit != null;
	}

	/**
	 * @return the number of metres represented by this length.
	 * This rounds if we have a fraction.
	 */
	public double getMetres() {
		return unit.metres*n;
	}

	/**
	 * E.g. the 5 in "5 miles"
	 */
	public double getValue() {
		return n;
	}

	/**
	 * @return the unit of measurement, e.g. metres or miles
	 */
	public LengthUnit geKLength() {
		return unit;
	}

	@Override
	public String toString() {
		return ((float)n) + " " + unit.toString().toLowerCase() + (n != 1 ? "s" : "");
	}

	/**
	 * Compare absolute values. So +1 metre is shorter than -1 mile.
	 * @param Dx2
	 * @return true if this is the shorter Dx
	 */
	public boolean isShorterThan(Dx Dx2) {
		assert Dx2 != null;
		return Math.abs(getMetres()) < Math.abs(Dx2.getMetres());
	}

	/**
	 * Equals if the same to the metre. So 1 day equals 24 hours
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		if (obj==this) return true;
		if (obj.getClass() != Dx.class) return false;
		Dx Dx = (Dx) obj;
		return getMetres() == Dx.getMetres();
	}

	@Override
	public int hashCode() {
		return new Double(getMetres()).hashCode();
	}

	/**
	 * @param x
	 * @return A new Dx which is x times this Dx
	 */
	public Dx multiply(double x) {
		return new Dx(x*n, unit);
	}

	@Override
	public int compareTo(Dx Dx2) {
		double ms = getMetres();
		double ms2 = Dx2.getMetres();
		if (ms==ms2) return 0;
		return ms<ms2 ? -1 : 1;
	}

	/**
	 * Convert this Dx to a different unit.
	 * <p>
	 * Uses {@link #divide(Dx)}
	 * @param unit2
	 * @return
	 */
	public Dx convertTo(LengthUnit unit2) {
		if (this.unit==unit2) {
			return this;
		}
		double n2 = divide(unit2.dx);
		return new Dx(n2, unit2);
	}

	/**
	 * Return the number of the specified length delta that would fit into this one
	 * e.g. KLength.KILOMETRE.getDx().divide(KLength.METRE.getDx()) -> 1000
	 * @param bucketSize
	 */
	public double divide(Dx other) {
		if (n==0) return 0;
		return (n * unit.metres) / (other.n * other.unit.metres);
	}


}
