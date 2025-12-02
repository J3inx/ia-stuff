import javax.swing.JFrame;
import javax.swing.JLabel;

public class main(){
    public static void main(String [] args){
        JFrame gui = new JFrame("Train Planner");
        gui.setSize(400, 300);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        gui.add(new JLabel("Hello from Swing!"));
        gui.setVisible(true);
    }
}