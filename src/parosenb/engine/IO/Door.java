package parosenb.engine.IO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import parosenb.engine.PhysicsEntity;
import parosenb.engine.World;
import parosenb.engine.collision.Shape;
import cs1971.Vec2f;

public class Door extends PhysicsEntity {
	
	private Output onDoorOpen = new Output();
	private Output onDoorClosed = new Output();
	private DoorToggleInput doDoorToggle = new DoorToggleInput();
	public Door(Vec2f position, World w, String name, ArrayList<Shape> s,
			Map<String, String> properties) {
		super(position, w, name, s, properties);
		this.visible = true;
		this.doesCollisions = true;
		this.isMoveable = false;
		namesToInputs.put("doDoorToggle", doDoorToggle);
		namesToOutputs.put("onDoorOpen", onDoorOpen);
		namesToOutputs.put("onDoorClosed", onDoorClosed);
	}
	
	@Override
	public void onCollide(PhysicsEntity e){
		
	}
	
	class DoorToggleInput extends Input {
		@Override
		public void run(HashMap<String, String> args) {
			setVisible(!isVisible());
			setDoesCollisions(!doesCollisions());
			if (doesCollisions()) {
				onDoorOpen.run();
			} else {
				onDoorClosed.run();
			}
		}
	 }

}

