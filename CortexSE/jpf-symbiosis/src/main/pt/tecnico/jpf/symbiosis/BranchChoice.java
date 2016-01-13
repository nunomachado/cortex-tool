package pt.tecnico.jpf.symbiosis;

public class BranchChoice {
	
	public int choice; 		//indicates the choice taken by JPF when exploring a new branch
	public String state;	//indicates the state id (i.e. "thread id_state id") which issued the branch choice
	
	public BranchChoice(){
		choice = -1;
		state = "";
	}

	public BranchChoice(int choice, String issuerState){
		this.choice = choice;
		this.state = issuerState;
	}
	
	public String getIssuerState(){
		return this.state;
	}
	
	public int getChoice(){
		return this.choice;
	}
}
