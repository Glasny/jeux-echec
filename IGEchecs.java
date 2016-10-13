import java.awt.*;
import javax.swing.*;
import java.util.*;

class SpecialPanel extends JPanel{
    char[][] jeu;
    Color blanc = Color.white;
    Color noir = new Color(150,120,120);
    HashMap<Character,ImageIcon>  images = new  HashMap<Character,ImageIcon>();
    SpecialPanel(char[][]  je){
	jeu = new char[8][8];
	for (int i = 0; i<8; i++){
	    for (int j=0; j<8; j++){
		jeu[i][j]=je[i][j];
	    }
	}
	images.put('p',new ImageIcon("images/0.gif"));
	images.put('c',new ImageIcon("images/1.gif"));
	images.put('f',new ImageIcon("images/2.gif"));
	images.put('t',new ImageIcon("images/3.gif"));
	images.put('d',new ImageIcon("images/4.gif"));
	images.put('r',new ImageIcon("images/5.gif"));
	images.put('P',new ImageIcon("images/6.gif"));
	images.put('C',new ImageIcon("images/7.gif"));
	images.put('F',new ImageIcon("images/8.gif"));
	images.put('T',new ImageIcon("images/9.gif"));
	images.put('D',new ImageIcon("images/10.gif"));
	images.put('R',new ImageIcon("images/11.gif"));
    }
    void paintBorders(Graphics g){
	g.setColor(new Color(255,246,200));
	g.fillRect(0,0,362,20);
	g.fillRect(0,342,362,20);
	g.fillRect(0,0,20,362);
	g.fillRect(342,0,20,362);
	g.setColor(Color.black);
	for (int i=0; i<8; i++){
	    g.drawString(""+(i+1),6,(8-i)*40+4);
	    g.drawString(""+(i+1),348,(8-i)*40+4);
	    g.drawString(""+(char)('a'+i),(i+1)*40-3,14);
	    g.drawString(""+(char)('a'+i),(i+1)*40-3,354);
	}

    }
    public void paintComponent(Graphics g) {
	super.paintComponent(g);   // Required
	paintBorders(g);
	boolean caseBlanche = true;
	for (int i=0; i<8; i++){
	    for (int j=0; j<8; j++){
		if (caseBlanche){
		    g.setColor(blanc);
		}else{
		    g.setColor(noir);
		}
		g.fillRect(i*40+22,j*40+22,38,38);
		if ((jeu[j][i]!='.')&&(jeu[j][i]!='*')&&(jeu[j][i]!='_')){
		    images.get(jeu[j][i]).paintIcon(this,g,i*40+21,j*40+22);
		}
		caseBlanche = !caseBlanche;
	    }
	    caseBlanche = !caseBlanche;
	}
    }
    void pose(char p, int ni, int nj){
	jeu[8-nj][ni] = p;
    }
    void libere(int ni, int nj){
	jeu[8-nj][ni] = '*';
    }
}


public class IGEchecs extends JFrame{
    private SpecialPanel jpane;
    boolean visible = false;
    private static final char[][] initial = 
                               {{'_','*','_','*','_','*','_','*'},
				{'*','_','*','_','*','_','*','_'},
				{'_','*','_','*','_','*','_','*'},
				{'*','_','*','_','*','_','*','_'},
				{'_','*','_','*','_','*','_','*'},
				{'*','_','*','_','*','_','*','_'},
				{'_','*','_','*','_','*','_','*'},
				{'*','_','*','_','*','_','*','_'}};

    IGEchecs(){
	this.setTitle("Echiquier");
	this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	jpane = new SpecialPanel(initial);
	this.setContentPane(jpane);
	jpane.setPreferredSize(new Dimension(362,362));
	jpane.setBackground(Color.black);
     }
    public void posePiece(char p, boolean blanc, int lig, int col){
	char c;
	if (blanc)
	    c = Character.toLowerCase(p);
	else
	    c = Character.toUpperCase(p);
	jpane.pose(c,lig,col);
    }
    public void libereCase(int lig, int col){
	jpane.libere(lig,col);
    }
    public void affiche(){
	if (!visible){
	    this.pack();
	    this.setVisible(true);
	    visible = true;
	}
	repaint();
    }
    public void ferme(){
	this.dispose();
    }
}

