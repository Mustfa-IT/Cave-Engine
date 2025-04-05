package com.engine.particles.emitters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

import com.engine.particles.AbstractParticleEmitter;
import com.engine.particles.Particle;
import com.engine.physics.PhysicsSystem;

/**
 * Emitter that produces particles with real physics interactions.
 * These particles interact with the physics world and can collide with objects.
 */
public class PhysicalParticleEmitter extends AbstractParticleEmitter {

  public static class PhysicalParticle extends Particle {
    private Body physicsBody;
    private float radius;
    private boolean removeOnCollision = false;
    private PhysicsSystem physicsSystem;

    public PhysicalParticle(float x, float y, float radius, Color color, float lifetime,
        Body body, PhysicsSystem physicsSystem) {
      super(x, y, radius * 2, color, lifetime);
      this.physicsBody = body;
      this.radius = radius;
      this.physicsSystem = physicsSystem;
    }

    @Override
    public void update(float deltaTime) {
      if (!isActive())
        return;

      // Update lifetime
      float remainingLifetime = getRemainingLifetime() - deltaTime;
      if (remainingLifetime <= 0) {
        setActive(false);
        if (physicsBody != null && physicsSystem != null) {
          physicsSystem.removeBody(physicsBody);
          physicsBody = null;
        }
        return;
      }

      // If physics body exists, update position from it
      if (physicsBody != null) {
        Vec2 position = physicsBody.getPosition();
        setX(physicsSystem.fromPhysicsWorld(position.x));
        setY(physicsSystem.fromPhysicsWorld(position.y));
        setRotation(physicsBody.getAngle());
      }

      // Call parent update for other properties
      super.update(deltaTime);
    }

    @Override
    public void render(Graphics2D g) {
      if (!isActive())
        return;

      // Save the original transform and composite
      var originalTransform = g.getTransform();
      var originalComposite = g.getComposite();

      // Apply transparency if needed
      if (getAlpha() < 1.0f) {
        g.setComposite(java.awt.AlphaComposite.getInstance(
            java.awt.AlphaComposite.SRC_OVER, getAlpha()));
      }

      // Apply transform for position
      g.translate(getX(), getY());
      g.rotate(getRotation());

      // Draw circular particle
      g.setColor(getColor());
      float size = getSize();
      g.fillOval((int) (-size / 2), (int) (-size / 2), (int) size, (int) size);

      // Restore original transform and composite
      g.setTransform(originalTransform);
      g.setComposite(originalComposite);
    }

    public Body getPhysicsBody() {
      return physicsBody;
    }

    public boolean isRemoveOnCollision() {
      return removeOnCollision;
    }

    public void setRemoveOnCollision(boolean removeOnCollision) {
      this.removeOnCollision = removeOnCollision;
    }
  }

  private final Random random = new Random();
  private PhysicsSystem physicsSystem;
  private World physicsWorld;

  // Particle physics properties
  private float density = 1.0f;
  private float friction = 0.3f;
  private float restitution = 0.5f; // Bounciness
  private boolean removeOnCollision = false;
  private float minRadius = 2.0f;
  private float maxRadius = 6.0f;
  private float minSpeed = 2.0f;
  private float maxSpeed = 10.0f;
  private float minLifetime = 3.0f;
  private float maxLifetime = 6.0f;
  private float spreadAngle = (float) Math.PI * 2; // 360 degrees
  private float baseAngle = 0; // Initial emission direction

  // Map to store particles by their physics body (for collision handling)
  private final Map<Body, PhysicalParticle> bodyToParticle = new HashMap<>();

  public PhysicalParticleEmitter(float x, float y, PhysicsSystem physicsSystem, World physicsWorld) {
    setPosition(x, y);
    this.physicsSystem = physicsSystem;
    this.physicsWorld = physicsWorld;
  }

  @Override
  protected Particle createParticle() {
    // Randomize lifetime
    float lifetime = minLifetime + random.nextFloat() * (maxLifetime - minLifetime);

    // Randomize radius
    float radius = minRadius + random.nextFloat() * (maxRadius - minRadius);

    // Create physics body
    BodyDef bodyDef = new BodyDef();
    bodyDef.type = BodyType.DYNAMIC;
    bodyDef.position.set(
        physicsSystem.toPhysicsWorld(x),
        physicsSystem.toPhysicsWorld(y));

    Body body = physicsWorld.createBody(bodyDef);

    // Create circle shape
    CircleShape shape = new CircleShape();
    shape.setRadius(physicsSystem.toPhysicsWorld(radius));

    // Create fixture
    FixtureDef fixtureDef = new FixtureDef();
    fixtureDef.shape = shape;
    fixtureDef.density = density;
    fixtureDef.friction = friction;
    fixtureDef.restitution = restitution;

    body.createFixture(fixtureDef);

    // Randomize velocity based on angle and speed
    float angle = baseAngle + (random.nextFloat() - 0.5f) * spreadAngle;
    float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);

    Vec2 velocity = new Vec2(
        (float) Math.cos(angle) * speed,
        (float) Math.sin(angle) * speed);

    body.setLinearVelocity(velocity);

    // Create the physical particle
    PhysicalParticle p = new PhysicalParticle(
        x, y, radius,
        new Color(random.nextFloat(), random.nextFloat(), random.nextFloat()),
        lifetime, body, physicsSystem);
    p.setRemoveOnCollision(removeOnCollision);

    // Store reference for collision handling
    bodyToParticle.put(body, p);
    body.setUserData(p);

    return p;
  }

  @Override
  public void update(float deltaTime) {
    // Regular update
    super.update(deltaTime);

    // Clean up destroyed particles
    particles.removeIf(p -> {
      if (!p.isActive() && p instanceof PhysicalParticle) {
        PhysicalParticle pp = (PhysicalParticle) p;
        Body body = pp.getPhysicsBody();
        if (body != null) {
          physicsSystem.removeBody(body);
          bodyToParticle.remove(body);
        }
        return true;
      }
      return false;
    });
  }

  /**
   * Handle collisions for physical particles
   * 
   * @param body The physics body that collided
   */
  public void handleCollision(Body body) {
    PhysicalParticle particle = bodyToParticle.get(body);
    if (particle != null && particle.isRemoveOnCollision()) {
      particle.setActive(false);
    }
  }

  @Override
  public void configure(Map<String, Object> params) {
    super.configure(params);

    if (params.containsKey("density"))
      this.density = ((Number) params.get("density")).floatValue();
    if (params.containsKey("friction"))
      this.friction = ((Number) params.get("friction")).floatValue();
    if (params.containsKey("restitution"))
      this.restitution = ((Number) params.get("restitution")).floatValue();
    if (params.containsKey("removeOnCollision"))
      this.removeOnCollision = (Boolean) params.get("removeOnCollision");
    if (params.containsKey("minRadius"))
      this.minRadius = ((Number) params.get("minRadius")).floatValue();
    if (params.containsKey("maxRadius"))
      this.maxRadius = ((Number) params.get("maxRadius")).floatValue();
    if (params.containsKey("minSpeed"))
      this.minSpeed = ((Number) params.get("minSpeed")).floatValue();
    if (params.containsKey("maxSpeed"))
      this.maxSpeed = ((Number) params.get("maxSpeed")).floatValue();
    if (params.containsKey("minLifetime"))
      this.minLifetime = ((Number) params.get("minLifetime")).floatValue();
    if (params.containsKey("maxLifetime"))
      this.maxLifetime = ((Number) params.get("maxLifetime")).floatValue();
    if (params.containsKey("spreadAngle"))
      this.spreadAngle = ((Number) params.get("spreadAngle")).floatValue();
    if (params.containsKey("baseAngle"))
      this.baseAngle = ((Number) params.get("baseAngle")).floatValue();
  }
}
