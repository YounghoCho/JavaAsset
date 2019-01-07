import java.util.ArrayList;
import java.util.List;

public class AnalyzedDoc {
	private String id;
	private String name;
	private ArrayList<String> list = new ArrayList<>();
			
	public ArrayList<String> getList() {
		return list;
	}
	public void setList(ArrayList<String> list) {
		this.list = list;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}
