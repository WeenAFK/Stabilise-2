package com.stabilise.util.shape;

import com.badlogic.gdx.math.Vector2;
import com.stabilise.util.MathUtil;
import com.stabilise.util.Matrix2;

/**
 * A FastAABB is a lightweight and slightly more optimised variant of
 * {@link AxisAlignedBoundingBox}, which is overall generally less expensive to
 * use.
 * 
 * <p>Unlike AxisAlignedBoundingBox, FastAABB is not a member of the
 * Polygon hierarchy as to avoid limitations imposed by superclasses.
 */
public class FastAABB extends Polygon implements AABB {
	
	/** The min vertex (i.e. bottom left) of the AABB. This is exposed for
	 * convenience purposes, and should be treated as if it is immutable. */
	public final Vector2 v00;
	/** The max vertex (i.e. top right) of the AABB. This is exposed for
	 * convenience purposes, and should be treated as if it is immutable.*/
	public final Vector2 v11;
	
	
	/**
	 * Creates a new AABB.
	 * 
	 * @param x The x-coordinate of the AABB's bottom-left vertex.
	 * @param y The y-coordinate of the AABB's bottom-left vertex.
	 * @param width The AABB's width.
	 * @param height The AABB's height.
	 */
	public FastAABB(float x, float y, float width, float height) {
		v00 = new Vector2(x, y);
		v11 = new Vector2(x + width, y + height);
	}
	
	/**
	 * Creates a new AABB.
	 * 
	 * @param v00 The min vertex (i.e. bottom left) of the AABB.
	 * @param v11 The max vertex (i.e. top right) of the AABB.
	 */
	public FastAABB(Vector2 v00, Vector2 v11) {
		this.v00 = v00;
		this.v11 = v11;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <p>Note that as a FastAABB is defined in terms of two vertices, the
	 * returned FastAABB will retain the properties of an AABB, but its min
	 * and max vertices will be transformed as per the matrix.
	 */
	@Override
	public FastAABB transform(Matrix2 matrix) {
		return newInstance(
				matrix.transform(v00),
				matrix.transform(v11)
		);
	}
	
	@Override
	public FastAABB translate(float x, float y) {
		return newInstance(
				new Vector2(v00.x + x, v00.y + y),
				new Vector2(v11.x + x, v11.y + y)
		);
	}
	
	@Override
	public FastAABB reflect() {
		return newInstance(
				new Vector2(-v11.x, v00.y),
				new Vector2(-v00.x, v11.y)
		);
	}
	
	@Override
	protected Vector2[] getVertices() {
		return new Vector2[] {
				v00,
				new Vector2(v11.x, v00.y),//v10
				v11,
				new Vector2(v00.x, v11.y) //v01
		};
	}
	
	@Override
	public boolean intersects(AbstractPolygon p) {
		if(p.isAABB())
			return intersects((AABB)p);
		return super.intersects(p);
	}
	
	@Override
	protected boolean intersectsOnOwnAxes(Shape s) {
		return getHorizontalProjection().overlaps(s.getHorizontalProjection()) &&
				getVerticalProjection().overlaps(s.getVerticalProjection());
	}
	
	/**
	 * Calculates whether or not two axis-aligned bounding boxes intersect.
	 * 
	 * @param a The AABB with which to test intersection.
	 * 
	 * @return {@code true} if the two AABBs intersect; {@code false}
	 * otherwise.
	 */
	private boolean intersects(AABB a) {
		return intersectsAABB(a.getV00(), a.getV11());
	}
	
	/**
	 * Calculates whether or not two AABBs intersect based on the min and max
	 * vertices of the other.
	 * 
	 * @param v00 The min vertex (i.e. bottom left) of the other AABB.
	 * @param v11 The max vertex (i.e. top right) of the other AABB.
	 * 
	 * @return {@code true} if the two AABBs intersect; {@code false}
	 * otherwise.
	 */
	private boolean intersectsAABB(Vector2 o00, Vector2 o11) {
		return v00.x <= o11.x && v11.x >= o00.x && v00.y <= o11.y && v11.y >= o00.y;
	}
	
	@Override
	public boolean containsPoint(float x, float y) {
		return x >= v00.x && x <= v11.x && y >= v00.y && y <= v11.y;
	}
	
	@Override
	protected Vector2[] generateAxes() {
		return MathUtil.UNIT_VECTORS;
	}
	
	@Override
	public ShapeProjection getHorizontalProjection() {
		return new ShapeProjection(v00.x, v11.x);
	}
	
	@Override
	public ShapeProjection getVerticalProjection() {
		return new ShapeProjection(v00.y, v11.y);
	}
	
	@Override
	protected boolean isAABB() {
		return true;
	}
	
	@Override
	public Vector2 getV00() {
		return v00;
	}
	
	@Override
	public Vector2 getV11() {
		return v11;
	}
	
	/**
	 * Gets the x-coordinate of the bottom-left vertex - or the origin - of
	 * this AABB.
	 * 
	 * @return The x-coordinate of this AABB's origin.
	 */
	public float getOriginX() {
		return v00.x;
	}
	
	/**
	 * Gets the y-coordinate of the bottom-left vertex - or the origin - of
	 * this AABB.
	 * 
	 * @return The y-coordinate of this AABB's origin.
	 */
	public float getOriginY() {
		return v00.y;
	}
	
	/**
	 * Calculates the width of this AABB.
	 * 
	 * @return The width of this AABB.
	 */
	public float getWidth() {
		return v11.x - v00.x;
	}
	
	/**
	 * Calculates the height of this AABB.
	 * 
	 * @return The height of this AABB.
	 */
	public float getHeight() {
		return v11.y - v00.y;
	}
	
	@Override
	public FastAABB precomputed() {
		return new Precomputed(this);
	}
	
	/**
	 * Creates a new FastAABB for duplication purposes. This is used to
	 * generate a new FastAABB whenever a duplicate is needed (i.e.,
	 * {@link #transform(Matrix2f)}, {@link #translate(float, float)},
	 * {@link #reflect()}, etc).
	 * 
	 * @return The new AABB.
	 */
	protected FastAABB newInstance(Vector2 v00, Vector2 v11) {
		return new FastAABB(v00, v11);
	}
	
	//--------------------==========--------------------
	//-------------=====Nested Classes=====-------------
	//--------------------==========--------------------
	
	/**
	 * The precomputed variant of an AABB.
	 * 
	 * <p>Though an instance of this class may be instantiated directly, its
	 * declared type should simply be that of FastAABB.
	 */
	public static final class Precomputed extends FastAABB {
		
		/** All four of the AABB's vertices. */
		private Vector2[] vertices;
		/** The AABB's own projections */
		private ShapeProjection[] projections;
		
		
		/**
		 * Creates a new precomputed AABB.
		 * 
		 * @param x The x-coordinate of the AABB's bottom-left vertex.
		 * @param y The y-coordinate of the AABB's bottom-left vertex.
		 * @param width The AABB's width.
		 * @param height The AABB's height.
		 */
		public Precomputed(float x, float y, float width, float height) {
			super(x, y, width, height);
			precompute();
		}
		
		/**
		 * Creates a new precomputed AABB.
		 * 
		 * @param v00 The min vertex (i.e. bottom left) of the AABB.
		 * @param v11 The max vertex (i.e. top right) of the AABB.
		 */
		public Precomputed(Vector2 v00, Vector2 v11) {
			super(v00, v11);
			precompute();
		}
		
		/**
		 * Constructor to be used by FastAABB.
		 */
		private Precomputed(FastAABB a) {
			super(a.v00, a.v11);
			precompute();
		}
		
		/**
		 * Calculates the AABB's vertices and projections.
		 */
		private void precompute() {
			vertices = super.getVertices();
			projections = new ShapeProjection[] {
				super.getHorizontalProjection(),
				super.getVerticalProjection()
			};
		}
		
		@Override
		protected Vector2[] getVertices() {
			return vertices;
		}
		
		@Override
		public ShapeProjection getHorizontalProjection() {
			return projections[0];
		}
		
		@Override
		public ShapeProjection getVerticalProjection() {
			return projections[1];
		}
		
		@Override
		public FastAABB notPrecomputed() {
			return new FastAABB(v00, v11);
		}
		
		@Override
		protected FastAABB newInstance(Vector2 v00, Vector2 v11) {
			return new Precomputed(v00, v11);
		}
		
	}
	
}
