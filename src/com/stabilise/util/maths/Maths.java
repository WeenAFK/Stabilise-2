package com.stabilise.util.maths;

/**
 * This class provides some utility methods and fields with mathematical
 * purpose.
 */
public class Maths {
	
	// non-instantiable
	private Maths() {}
	
	/** Holds the value given by Math.sqrt(2). */
	public static final double SQRT_2 = Math.sqrt(2);
	/** Holds the value given by Math.PI * 2. */
	public static final double TAU = 2 * Math.PI;
	/** Holds the value given by Math.PI / 2. */
	public static final double PI_OVER_2 = Math.PI / 2;
	
	/** The x-axis unit vector with components (1,0). */
	public static final Vec2 VEC_X = new Vec2(1f, 0f);
	/** The y-axis unit vector with components (0,1). */
	public static final Vec2 VEC_Y = new Vec2(0f, 1f);
	/** A vector with components (1, 1). */
	public static final Vec2 VEC_1_1 = new Vec2(1f, 1f);
	/** An array containing the unit vectors {@link #VEC_X} and {@link #VEC_Y}. */
	public static final Vec2[] UNIT_VECTORS = new Vec2[] { VEC_X, VEC_Y };
	
	/** The maximum value which can be held by an unsigned byte
	 * (<tt>2<sup><font size=-1>8</font></sup>-1</tt>). */
	public static final int UBYTE_MAX_VALUE = 0xFF; //(1 << Byte.SIZE) - 1;
	/** The maximum value which can be held by an unsigned short
	 * (<tt>2<sup><font size=-1>16</font></sup>-1</tt>). */
	public static final int USHORT_MAX_VALUE = 0xFFFF; //(1 << Short.SIZE) - 1;
	/** The maximum value which can be held by an unsigned int
	 * (<tt>2<sup><font size=-1>16</font></sup>-1</tt>). */
	public static final int UINT_MAX_VALUE = 0xFFFFFFFF; //(int)((1L << Integer.SIZE) - 1);
	
	
	/**
	 * Calculates whether or not a number is a power of two. Note that this
	 * method will erroneously report {@code 0} as a power of two.
	 * 
	 * @return {@code true} if {@code n} is a power of two; {@code false}
	 * otherwise.
	 */
	public static boolean isPowerOfTwo(int n) {
		return (n & -n) == n; // alternatively, return (n & (n-1)) == 0;
	}
	
	/**
	 * Calculates the remainder of a division operation; negative remainders
	 * are wrapped by adding {@code div} to such remainders.
	 * 
	 * @param num The numerator.
	 * @param div The divisor.
	 * 
	 * @return The wrapped remainder of {@code num % div}.
	 * @throws ArithmeticException if {@code div == 0}.
	 */
	public static int wrappedRem(int num, int div) {
		num %= div;
		return num >= 0 ? num : num + div;
	}
	
	/**
	 * Calculates the remainder of a division operation; negative remainders
	 * are wrapped by adding {@code div} to such remainders.
	 * 
	 * @param num The numerator.
	 * @param div The divisor.
	 * 
	 * @return The wrapped remainder of {@code num % div}.
	 * @throws ArithmeticException if {@code div == 0}.
	 */
	public static long wrappedRem(long num, long div) {
		num %= div;
		return num >= 0 ? num : num + div;
	}
	
	/**
	 * Calculates the remainder of a division operation; negative remainders
	 * are wrapped by adding {@code div} to such remainders.
	 * 
	 * @param num The numerator.
	 * @param div The divisor.
	 * 
	 * @return The wrapped remainder of {@code num % div}.
	 * @throws ArithmeticException if {@code div == 0}.
	 */
	public static double wrappedRem(double num, double div) {
		num %= div;
		return num >= 0 ? num : num + div;
	}
	
	/**
	 * Calculates the remainder of a division operation; negative remainders
	 * are wrapped as if by adding {@code div} to such remainders.
	 * 
	 * <p><b>Note</b>: This method is faster than {@link
	 * #wrappedRem(int, int)}, but this only works if {@code div} is a
	 * positive power of 2. As such, this method functions as a faster
	 * alternative to the modulus operator for valid divisors.
	 * 
	 * @param num The numerator.
	 * @param div The divisor, which should be a power of 2.
	 * 
	 * @return The wrapped remainder of {@code num % div}. If {@code div} is
	 * not a power of 2, the result may be incorrect.
	 */
	public static int wrappedRem2(int num, int div) {
		// This is a bit-level hack which uses a bitmask to get the wrapped
		// remainder. Remember: this only works with powers of 2 since
		// decrementing a Po2 results in a consistent set of 1s to the right
		// of the single 1 in div
		return num & (div - 1);
		// Equivalently (though with what looks like one more instruction):
		//return num & -(~div);
	}
	
	/**
	 * Calculates the remainder of a division operation; negative remainders
	 * are wrapped as if by adding {@code div} to such remainders.
	 * 
	 * <p><b>Note</b>: This method is faster than {@link
	 * #wrappedRem(long, long)}, but this only works if {@code div} is a
	 * positive power of 2. As such, this method functions as a faster
	 * alternative to the modulus operator for valid divisors.
	 * 
	 * @param num The numerator.
	 * @param div The divisor, which should be a power of 2.
	 * 
	 * @return The wrapped remainder of {@code num % div}. If {@code div} is
	 * not a power of 2, the result may be incorrect.
	 */
	public static long wrappedRem2(long num, long div) {
		return num & (div - 1);
	}
	
	/**
	 * Gets the whole number nearest to {@code x}, in integer form. This
	 * method is equivalent to, but faster than {@code (int)Math.round(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The rounded value of x.
	 * @see Math#round(double)
	 */
	public static int round(double x) {
		return floor(x + 0.5D);
	}
	
	/**
	 * Gets the whole number nearest to {@code x}, in integer form. This
	 * method is equivalent to, but faster than {@code (int)Math.round(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The rounded value of x.
	 * @see Math#round(double)
	 */
	public static int round(float x) {
		// The fact that duplicating for floats is necessary annoys me
		return floor(x + 0.5f);
	}
	
	/**
	 * Gets the greatest whole number less than or equal to {@code x}, in
	 * integer form. This method is equivalent to, but faster than
	 * {@code (int)Math.floor(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The floored value of x.
	 * @see Math#floor(double)
	 */
	public static int floor(double x) {
		int xi = (int)x;
		return x < xi ? xi-1 : xi;
	}
	
	/**
	 * Gets the greatest whole number less than or equal to {@code x}, in
	 * integer form. This method is equivalent to, but faster than
	 * {@code (int)Math.floor(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The floored value of x.
	 * @see Math#floor(double)
	 */
	public static int floor(float x) {
		int xi = (int)x;
		return x < xi ? xi-1 : xi;
	}
	
	/**
	 * Gets the smallest whole number greater than or equal to {@code x}, in
	 * integer form. This method is equivalent to, but faster than
	 * {@code (int)Math.ceil(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The ceil'd value of x.
	 * @see Math#ceil(double)
	 */
	public static int ceil(double x) {
		int xi = (int)x;
		return x > xi ? xi+1 : xi;
	}
	
	/**
	 * Gets the smallest whole number greater than or equal to {@code x}, in
	 * integer form. This method is equivalent to, but faster than
	 * {@code (int)Math.ceil(x)}.
	 * 
	 * @param x The number.
	 * 
	 * @return The ceil'd value of x.
	 * @see Math#ceil(double)
	 */
	public static int ceil(float x) {
		int xi = (int)x;
		return x > xi ? xi+1 : xi;
	}
	
	/**
	 * Calculates the arithmetic mean of {@code a} and {@code b}, rounded
	 * towards negative infinity.
	 * 
	 * @param a The first number.
	 * @param b The second number.
	 */
	public static int meanFloor(int a, int b) {
		// How this works:
		// Binary addition can be described as such (do your research if you don't know this):
		// a + b = ((a & b) << 1) + (a ^ b)
		// The arithmetic mean is defined as (a + b)/2
		// (a+b)/2 = (((a & b) << 1) + (a ^ b)) / 2
		//         = (((a & b) << 1) + (a ^ b)) >> 1	[division by 2 is equivalent to a shift right]
		//         = (a & b) + ((a ^ b) >> 1)			[the shifts cancel out]
		// return (a & b) + ((a ^ b) >> 1);
		return (a & b) + ((a ^ b) >> 1);
	}
	
	/**
	 * Returns the arithmetic mean of {@code a} and {@code b}, rounded towards
	 * positive infinity.
	 */
	public static int meanCeil(int a, int b) {
		// I haven't quite figured out how this works yet. Taken from hacker's
		// delight
		return (a | b) - ((a ^ b) >> 1);
	}
	
	/**
	 * Returns the lesser of the two provided numbers.
	 * 
	 * <p>Unlike {@link Math#min(float, float)}, this method does not perform
	 * any 'unnecessary' computation (refer to Math.min() source to determine
	 * which is preferable).
	 */
	public static float min(float a, float b) {
		return a < b ? a : b;
	}
	
	/**
	 * Returns the greater of the two provided numbers.
	 * 
	 * <p>Unlike {@link Math#max(float, float)}, this method does not perform
	 * any 'unnecessary' computation (refer to Math.max() source to determine
	 * which is preferable).
	 */
	public static float max(float a, float b) {
		return a > b ? a : b;
	}
	
	/*
	 * Calculates the inverse square root of {@code x}, or
	 * <tt>x<font size=-1><sup>-1/2</sup></font></tt>.
	 * 
	 * @param x The number.
	 * @return <tt>x<font size=-1><sup>-1/2</sup></font></tt>
	 */
	/*
	public static float invSqrt(float x) {
	    float xhalf = 0.5f*x;
	    int i = Float.floatToIntBits(x);
	    i = 0x5f3759df - (i>>1);
	    x = Float.intBitsToFloat(i);
	    x = x*(1.5f - xhalf*x*x);
	    return x;
	}
	*/
	
	/**
	 * Linearly interpolates between two given values. Though {@code x} should
	 * be between 0.0 and 1.0, an exception will not be thrown if it is outside
	 * of these bounds.
	 * 
	 * @param y0 The y value when x=0;
	 * @param y1 The y value when x=1.
	 * @param x The x-coordinate at which to find the value, from 0.0 to 1.0.
	 * 
	 * @return A value linearly interpolated between y0 and y1.
	 */
	public static double interpolateLinear(double y0, double y1, double x) {
		return y0 + (y1 - y0)*x;
	}
	
	/**
	 * Sinusoidally interpolates between two given values. Though {@code x}
	 * should be between 0.0 and 1.0, an exception will not be thrown if it is
	 * outside of these bounds.
	 * 
	 * @param y0 The y value when x=0;
	 * @param y1 The y value when x=1.
	 * @param x The x-coordinate at which to find the value, from 0.0 to 1.0.
	 * 
	 * @return A value sinusoidally interpolated between y0 and y1.
	 */
	public static double interpolateSinusoidal(double y0, double y1, double x) {
		return interpolateLinear(y0, y1, 3*x*x - 2*x*x*x);
		
		// Better because both first and second derivatives are 0 at endpoints
		//return interpolateLinear(y0, y1, 6*x*x*x*x*x - 15*x*x*x*x + 10*x*x*x);
		
		// Almost identical graph, but possibly more computationally expensive
		//return interpolateLinear(y0, y1, (1-Math.cos(x*Math.PI))*0.5D);
	}
	
	/**
	 * Linearly interpolates between the four vertices of a unit square. Though
	 * {@code x} and {@code y} should be between 0.0 and 1.0, an exception
	 * will not be thrown if they are outside of these bounds.
	 * 
	 * @param z00 The z value when x=0 and y=0.
	 * @param z01 The z value when x=0 and y=1.
	 * @param z10 The z value when x=1 and y=0.
	 * @param z11 The z value when x=1 and y=1.
	 * @param x The x-coordinate at which to find the value, from 0.0 to 1.0.
	 * @param y The y-coordinate at which to find the value, from 0.0 to 1.0.
	 * 
	 * @return A value linearly interpolated between all four vertices at the
	 * given point.
	 */
	public static double interpolateBilinear(double z00, double z01, double z10, double z11, double x, double y) {
		double z0 = interpolateLinear(z00, z10, x);
		double z1 = interpolateLinear(z01, z11, x);
		return interpolateLinear(z0, z1, y);
	}
	
	/**
	 * Sinusoidally interpolates between the four vertices of a unit square.
	 * Though {@code x} and {@code y} should be between 0.0 and 1.0, an
	 * exception will not be thrown if they are outside of these bounds.
	 * 
	 * @param z00 The z value when x=0 and y=0.
	 * @param z01 The z value when x=0 and y=1.
	 * @param z10 The z value when x=1 and y=0.
	 * @param z11 The z value when x=1 and y=1.
	 * @param x The x-coordinate at which to find the value, from 0.0 to 1.0.
	 * @param y The y-coordinate at which to find the value, from 0.0 to 1.0.
	 * 
	 * @return A value sinusoidally interpolated between all four vertices at
	 * the given point.
	 */
	public static double interpolateBisinusoidal(double z00, double z01, double z10, double z11, double x, double y) {
		double z0 = interpolateSinusoidal(z00, z10, x);
		double z1 = interpolateSinusoidal(z01, z11, x);
		return interpolateSinusoidal(z0, z1, y);
	}
	
	/**
	 * Compacts two integer values into one, as if by:
	 * <pre>(x << 16) | (y & 0xFFFF)</pre>
	 * 
	 * <p>The returned value is susceptible to collisions for any values 
	 * {@code x0}, {@code y0} and {@code x1}, {@code y1} of {@code x} and
	 * {@code y} which satisfy:
	 * 
	 * <pre>
	 * wrappedRem(x0, 65536) == wrappedRem(x1, 65536) &&
	 * wrappedRem(y0, 65536) == wrappedRem(y1, 65536)</pre>
	 * 
	 * @param x The first value.
	 * @param y The second value.
	 * 
	 * @return The compacted value.
	 */
	public static int compactInt(int x, int y) {
		return (x << 16) | (y & 0xFFFF);
	}
	
	/**
	 * Compacts two integer values into a long, as if by:
	 * <pre>(x << 32) | y</pre>
	 * 
	 * @param x The first value.
	 * @param y The second value.
	 * 
	 * @return The compacted value.
	 */
	public static long compactLong(int x, int y) {
		return ((long)x << 32) | (long)y;
	}
	
	/**
	 * Returns {@code x*x}.
	 */
	public static int square(int x) {
		return x*x;
	}
	
}
