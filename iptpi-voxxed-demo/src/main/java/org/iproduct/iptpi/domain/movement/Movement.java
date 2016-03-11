package org.iproduct.iptpi.domain.movement;

public class Movement {
	private float deltaX = 100; // mm
	private float deltaY = 100; // mm
	private float curvature = 0; // radians/mm
	private float velocity = 40; // mm/sec
	private float angularVelocity = 0; // radians/sec
	private float acceleration = 0; // mm/sec^2
	private float angularAcceleration = 0; // radians/sec^2
	
	public Movement() {
	}
	
	public Movement(float curvature) {
		this.curvature = curvature;
	}

	public Movement(float deltaX, float deltaY) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
	}

	public Movement(float deltaX, float deltaY, float curvature) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.curvature = curvature;
	}

	public Movement(float deltaX, float deltaY, float curvature, float velocity) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.curvature = curvature;
		this.velocity = velocity;
	}

	public Movement(float deltaX, float deltaY, float curvature, float velocity, float angularVelocity,
			float acceleration, float angularAcceleration) {
		this.deltaX = deltaX;
		this.deltaY = deltaY;
		this.curvature = curvature;
		this.velocity = velocity;
		this.angularVelocity = angularVelocity;
		this.acceleration = acceleration;
		this.angularAcceleration = angularAcceleration;
	}

	public float getDeltaX() {
		return deltaX;
	}

	public float getDeltaY() {
		return deltaY;
	}

	public float getCurvature() {
		return curvature;
	}

	public float getVelocity() {
		return velocity;
	}

	public float getAngularVelocity() {
		return angularVelocity;
	}

	public float getAcceleration() {
		return acceleration;
	}

	public float getAngularAcceleration() {
		return angularAcceleration;
	}

	@Override
	public String toString() {
		return "Movement [deltaX=" + deltaX + ", deltaY=" + deltaY + ", curvature=" + curvature + ", velocity="
				+ velocity + ", angularVelocity=" + angularVelocity + ", acceleration=" + acceleration
				+ ", angularacceleration=" + angularAcceleration + "]";
	}

}
