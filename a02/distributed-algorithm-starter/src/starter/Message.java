package starter;

public class Message {

	public static final int TYPE_CALC = 123;
	public static final int TYPE_TERMINATE = 456;
	private final String sender;
	private final int type;
	private final int value;

	public Message(String sender, int type, int value){
		this.sender = sender;
		this.type = type;
		this.value = value;
	}

	@Override
	public String toString() {
		String type = this.type == TYPE_CALC ? "calc" : this.type == TYPE_TERMINATE ? "terminate" : "no-type?!?!";
		return "msg{sender: " + this.sender + "; type: " + type + "; value:" + value + "}";
	}

	public String getSender() {
		return sender;
	}

	public int getType() {
		return type;
	}

	public int getValue() {
		return value;
	}
}
