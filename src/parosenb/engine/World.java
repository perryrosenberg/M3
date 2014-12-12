package parosenb.engine;

import java.awt.Graphics2D;
import java.awt.List;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import parosenb.engine.collision.Polygon;
import parosenb.engine.collision.Ray;
import parosenb.engine.collision.Shape;
import parosenb.m.game.GameViewport;
import cs1971.CS1971LevelReader;
import cs1971.CS1971LevelReader.InvalidLevelException;
import cs1971.LevelData;
import cs1971.LevelData.ConnectionData;
import cs1971.LevelData.EntityData;
import cs1971.LevelData.ShapeData;
import cs1971.Vec2f;
import cs1971.Vec2i;

public abstract class World {
	public Vec2f worldBounds;
	protected ArrayList<Entity> entities;
	protected ArrayList<PhysicsEntity> physicsEntities;
	public ArrayList<Entity> toAdd = new ArrayList<Entity>();
	public ArrayList<Entity> toRemove = new ArrayList<Entity>();
	public ArrayList<PhysicsEntity> physicsToRemove = new ArrayList<PhysicsEntity>();
	public ArrayList<Ray> raysToRemove = new ArrayList<Ray>();
	public ArrayList<Ray> rays = new ArrayList<Ray>();
	protected Viewport viewport;
	protected HashMap<String, Entity> entitiesInLevel = new HashMap<String, Entity>();
	private HashMap<String, Class<?>> availableEntities;
	
	
	public ArrayList<PhysicsEntity> getPhysicsEntities() {
		return physicsEntities;
	}
	
	public void addEntity(Entity e){
		entities.add(e);
	}
	
	public void removeEntity(Entity e){
		toRemove.add(e);
	}
	
	public void addPhysicsEntity(PhysicsEntity e){
		physicsEntities.add(e);
	}
	
	public void removePhysicsEntity(PhysicsEntity e){
		physicsToRemove.add(e);
	}
	
	public Entity getEntity(int index){
		return entities.get(index);
	}
	
	public abstract <T extends PhysicsEntity> void onCollision(T s, T e);
	
	public void collide() {
		for (int i = 0; i < physicsEntities.size(); i++){
			PhysicsEntity s = physicsEntities.get(i);
			for (int j = i; j < physicsEntities.size(); j++){
				PhysicsEntity e = physicsEntities.get(j);
				shape:
				if (s != e && s.collisionShape != null && e.collisionShape != null &&
						(s.collisionShape.collisionGroupMask & e.collisionShape.collisionGroupMask) == 0) {
					Vec2f mtv = s.collisionShape.collides(e.collisionShape);
					Vec2f mtv2 = e.collisionShape.collides(s.collisionShape);
					s.setLastMTV(mtv2);
					e.setLastMTV(mtv);
					if (mtv != null) {
						if (s.isMoveable && e.isMoveable) {
							s.position = s.position.plus(mtv2.sdiv(2));
							s.collisionShape.updatePosition(s.position);
							e.position = e.position.plus(mtv.sdiv(2));
							e.collisionShape.updatePosition(e.position);
						} else if (s.isMoveable && !e.isMoveable) {
							s.position = s.position.plus(mtv2);
							s.collisionShape.updatePosition(s.position);
						} else if (e.isMoveable && !s.isMoveable) {
							e.position = e.position.plus(mtv);
							e.collisionShape.updatePosition(e.position);
						}
						float COR = (float) Math.sqrt(s.restitution*e.restitution);
						if (mtv2.mag() == 0 || mtv.mag() == 0){
							break shape;
						}
						Vec2f Us = mtv2.normalized().smult(s.velocity.dot(mtv2.normalized()));
						Vec2f Ue = mtv.normalized().smult(e.velocity.dot(mtv.normalized()));
						//Vec2f Vs = (Ue.minus(Us)).smult((e.mass*(1+COR))/s.mass+e.mass).plus(Us);
						//Vec2f Ve = (Ue.minus(Us)).smult((s.mass*(1+COR))/e.mass+s.mass).plus(Ue);
//						Vec2f Vs = (Us.smult(s.mass).plus(Ue.smult(e.mass)).plus(Ue.minus(Us).smult(e.mass*COR))).sdiv(s.mass+e.mass);
//						Vec2f Ve = (Ue.smult(e.mass).plus(Us.smult(s.mass)).plus(Us.minus(Ue).smult(s.mass*COR))).sdiv(e.mass+s.mass);
						Vec2f Is = new Vec2f(0,0), Ie = new Vec2f(0,0);
						if (s.isMoveable && e.isMoveable){
							Is = (Ue.minus(Us)).smult((s.mass*e.mass*(1+COR))/(s.mass+e.mass));
							Ie = (Us.minus(Ue)).smult((e.mass*s.mass*(1+COR))/(e.mass+s.mass));
						} else if (!s.isMoveable && e.isMoveable) {
//							Is = (Ue.minus(Us)).smult(e.mass*(1+COR));
							Ie = (Us.minus(Ue)).smult(e.mass*(1+COR));
						} else if (s.isMoveable && !e.isMoveable){
							Is = (Ue.minus(Us)).smult(s.mass*(1+COR));
//							Ie = (Us.minus(Ue)).smult(s.mass*(1+COR));
						} else {
							//do nothing
							//System.out.println("Should never happen, immovable object collision");
						}
						s.applyImpulse(Is);
						e.applyImpulse(Ie);
						//s.applyImpulse(mtv2.smult(Math.min((Math.abs(s.velocity.x) + (Math.abs(s.velocity.y))), .2f)));
						//e.applyImpulse(mtv.smult(Math.min((Math.abs(s.velocity.x) + (Math.abs(s.velocity.y))), .2f)));
						onCollision(s, e);
					} 
				}
			}
		}
	}
	
	public abstract <T extends PhysicsEntity> void onRayCollision(Ray r, T s, T e, Vec2f closestPoint);
	
	public void collideRays() {
		for (int i = 0; i < rays.size(); i++){
			Ray r = rays.get(i);
			Vec2f closestPointToRay = null;
			int index = 0;
			for (int j = 0; j < physicsEntities.size(); j++){
				PhysicsEntity e = (physicsEntities.get(j));
				Vec2f newPoint = r.collides(physicsEntities.get(j).collisionShape);
				if (newPoint != null && (r.rayCastingGroupMask & e.collisionShape.rayCastingGroupMask) == 0 && r.source != e) {
					if (closestPointToRay == null) {
						closestPointToRay = newPoint;
						index = j;
					} else if (r.position.dist2(closestPointToRay) > r.position.dist2(newPoint)) {
						closestPointToRay = newPoint;
						index = j;
					}
					
				}
			}
			if (physicsEntities.size() > 0 && r.source != physicsEntities.get(index) && 
					(r.rayCastingGroupMask & physicsEntities.get(index).collisionShape.rayCastingGroupMask) == 0){
				onRayCollision(r, r.source, physicsEntities.get(index), closestPointToRay);
			}
		}
	}
	
	public void initializeWorld(String path){
		//"levels/level1.nlf"
		File file = new File(path);
		LevelData leveld = null;
		try {
			leveld = CS1971LevelReader.readLevel(file);
		} catch (FileNotFoundException e) {
			System.out.println("File not found.");
		} catch (InvalidLevelException e) {
			System.out.println("Invalid level.");
		}
		
		//make Connections
		for(ConnectionData d : leveld.getConnections()){
			d.getSource();
		}
		
		//make Entities
		for(EntityData data : leveld.getEntities()) {
			
			//Set up shapes
			ArrayList<Shape> shapes = new ArrayList<Shape>();
			for (ShapeData s : data.getShapes()) {
				if (s.getType() == ShapeData.Type.POLY){
					java.util.List<Vec2f> list = s.getVerts();
					for (Vec2f v : list){
						
					}
					Polygon p = new Polygon(new Vec2f(0,0), s.get, null);
					shapes.add()
				} else if (s.getType() == ShapeData.Type.CIRCLE) {
					
				} else if (s.getType() == ShapeData.Type.BOX) {
					
				}
				
				s.getType().getClass().getConstructor(parameterTypes).newInstance();
			}
			
			
			//availableEntities.get(d.getEntityClass()).getConstructor(String, Shape, HashMap<String, String>).newInstance(d.getName(), s, d.getProperties());
		}
		
	}
	
	public void setView(Viewport view){
		this.viewport = view;
	}
	
	public void onTick(long nanosSincePreviousTick){
		for(Entity e: entities) {
			e.onTick(nanosSincePreviousTick);
		}
		collide();
		collideRays();
		for (Entity e: toAdd) {
			entities.add(e);
		}
		toAdd = new ArrayList<Entity>();
		for (Entity e: toRemove) {
			entities.remove(e);
		}
		for (PhysicsEntity e: physicsToRemove) {
			physicsEntities.remove(e);
		}
		physicsToRemove = new ArrayList<PhysicsEntity>();
		toRemove = new ArrayList<Entity>();
		toAdd = new ArrayList<Entity>();
		for (Ray r: raysToRemove) {
			rays.remove(r);
		}
		raysToRemove = new ArrayList<Ray>();
	}
	
	public void onDraw(Graphics2D g){
		for(Entity e: entities){
			e.onDraw(g);
		}
	};
	
	public abstract void onKeyTyped(KeyEvent e, Viewport gameView);

	public abstract void onKeyPressed(KeyEvent e, Viewport gameView);

	public abstract void onKeyReleased(KeyEvent e, Viewport gameView);

	public abstract void onMouseClicked(MouseEvent e, Viewport gameView);

	public abstract void onMousePressed(MouseEvent e, Viewport gameView);

	public abstract void onMouseReleased(MouseEvent e, Viewport gameView);

	public abstract void onMouseDragged(MouseEvent e, Viewport gameView);

	public abstract void onMouseMoved(MouseEvent e, Viewport gameView);

	public abstract void onMouseWheelMoved(MouseWheelEvent e, Viewport gameView);
	
	public void onResize(Vec2i newSize) {
		for(Entity e: entities){
			e.onResize(newSize);
		}
		this.worldBounds = new Vec2f(newSize.x, newSize.y);
	}
	
}
