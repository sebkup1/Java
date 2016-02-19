import javax.swing.JFrame;

public class Program {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Server server = new Server();
		server.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		server.startRunning();
	}

}