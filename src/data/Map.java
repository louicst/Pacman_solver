package data;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import logic.PacManLauncher;
import logic.Pacman;
import logic.BeliefState;
import view.*;


/**
 * Cette classe permet, à partir d'un fichier .map, de créer toutes les figures d'un niveau
 *
 * @author RGM
 * @version 03/03/2014
 *
 * @inv WIDTH == Canvas.WIDTH
 */
public class Map {

	/** Le nombre de case de la map */
	private int nbCases;
	/** La taille de chacune des cases */
	private int tailleCase;
	/** La position en X de pacman */
	private int pacmanX;
	/** La position en Y de pacman */
	private int pacmanY;
	/** Taille de la fenêtre */
	private final int WIDTH = Canvas.WIDTH;
	/** Tableau à deux dimension de figure contenant toute les figures de la map case par case */
	private MapGenerate theMap;
	/** La couleur des mur de la map */
	private String couleurMur;
	/** Le nom du fichier .map */
	private String mapFile;
	/** Le nombre de gomme présent sur la map */
	private int nbrGomme;
	/** La position sur la map de chaque fantôme en début de niveau : Un liste de couple (x,y) */
	private ArrayList<int[]> ghosts;
	/** Paires cases visibles */
	private HashSet<String> visible;
	private PacManLauncher pml;
	private BeliefState state;
	private ArrayList<BeliefState> visibleBeliefState;
	private ArrayList<int[]> gamePositions;

	/**
	 * Constructeur de la classe Map, il creer un niveau du jeu a partir d'un fichier
	 *
	 * @param mapName le numéro de la map a charger
	 * @pre mapNumber > 0
	 */
	public Map(int mapNumber, PacManLauncher pml) {
		this.pml = pml;
		assert mapNumber > 0 : "Precondition non respectée : numéro de la map négatif";
		this.mapFile = "./doc/map"+ mapNumber +".map";
		this.nbrGomme = 0;
		this.ghosts = new ArrayList<int[]>();
		this.visibleBeliefState = new ArrayList<BeliefState>();
		this.gamePositions = new ArrayList<int[]>();
		this.createMap();
		this.invariant();
	}

	/*******************************************************************
  Un fichier map.txt définira un niveau de jeu
  La première ligne contient les parametre nbCase, couleur du mur ...
                      # = un mur
                      . = une gomme
	 * = une super-gomme
                      O = un chemin vide
                      P = PacMan
                      F = fantome
	 *******************************************************************/

	/**
	 * Cette fonction est appellée par le constructeur afin de lire le fichier .map et d'initialiser tout les parametres
	 *
	 * @post nbrGomme > 0
	 * @post pacmanX > 0
	 * @post pacmanY > 0
	 * @post couleurMur == "blue" || couleurMur == "green" || couleurMur == "pink"
	 */
	private void createMap(){
		try{
			// Ouverture du fichier pour la lecture
			InputStream ips=new FileInputStream(this.mapFile);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br=new BufferedReader(ipsr);

			boolean firstLine = true;        // La premiere ligne contient des parametres spéciaux
			String ligne;                    // La ligne suivante à lire
			int i = 0;                       // La ligne de la map
 
			
			// On lit toute les lignes du fichier
			while ((ligne=br.readLine())!=null){
				//Traitement spéciale pour la première ligne
				if(firstLine) {
					firstLine = false;
					String[] param = ligne.split(";");
					this.nbCases = Integer.parseInt(param[0]);
					this.tailleCase = this.WIDTH / this.nbCases;
					this.couleurMur = param[1];
					this.theMap = new MapGenerate(this.nbCases);
					this.visible = new HashSet<String>();
					this.state = new BeliefState(this.nbCases, this.pml.getPacman() != null? this.pml.getPacman().getScore(): 0, this.pml.getPacman() != null? this.pml.getPacman().getLife(): Pacman.LIFE_START);
				}
				else {
					int j = 0;                   // La colonne de la map
					int tmpx = 0;                // Variable utilisée pour stocké la position en x d'une figure
					int tmpy = 0;                // Variable utilisée pour stocké la position en y d'une figure
					int[] posGhost = null;

					String[] param = ligne.split("");
					for (String str : param) {
						tmpx = j*this.tailleCase;  // Calcule de la position de la figure
						tmpy = i*this.tailleCase;  // Calcule de la postion de la figure
						// Voir description des symboles ci-dessus
						switch (str) {
						case "#" :
							this.theMap.setFigure(i,j,new Wall(this.tailleCase, tmpx, tmpy, this.couleurMur));
							break;
						case "." :
							this.theMap.setFigure(i,j,new Gomme(this.tailleCase, tmpx, tmpy, false));
							this.nbrGomme += 1;
							this.visible.add(i+","+j+";"+i+","+j);
							for(int k = i - 1; k > -1 && this.visible.contains(k+","+j+";"+(i-1)+","+j); k--) {
								this.visible.add(k+","+j+";"+i+","+j);
							}
							for(int k = j - 1; k > -1 && this.visible.contains(i+","+k+";"+i+","+(j-1)); k--) {
								this.visible.add(i+","+k+";"+i+","+j);
							}
							int [] pos1 = {i,j};
							this.gamePositions.add(pos1);
							break;
						case "*" :
							this.theMap.setFigure(i,j,new Gomme(this.tailleCase, tmpx, tmpy, true));
							this.nbrGomme += 1;
							this.visible.add(i+","+j+";"+i+","+j);
							for(int k = i - 1; k > -1 && this.visible.contains(k+","+j+";"+(i-1)+","+j); k--) {
								this.visible.add(k+","+j+";"+i+","+j);
							}
							for(int k = j - 1; k > -1 && this.visible.contains(i+","+k+";"+i+","+(j-1)); k--) {
								this.visible.add(i+","+k+";"+i+","+j);
							}
							int [] pos2 = {i,j};
							this.gamePositions.add(pos2);
							break;
						case "O" :
							this.theMap.setFigure(i,j,new Gomme(this.tailleCase, tmpx, tmpy));
							this.visible.add(i+","+j+";"+i+","+j);
							for(int k = i - 1; k > -1 && this.visible.contains(k+","+j+";"+(i-1)+","+j); k--) {
								this.visible.add(k+","+j+";"+i+","+j);
							}
							for(int k = j - 1; k > -1 && this.visible.contains(i+","+k+";"+i+","+(j-1)); k--) {
								this.visible.add(i+","+k+";"+i+","+j);
							}
							int [] pos3 = {i,j};
							this.gamePositions.add(pos3);
							break;
						case "P" :
							this.theMap.setFigure(i,j,new Gomme(this.tailleCase, tmpx, tmpy));
							this.pacmanX = tmpx;
							this.pacmanY = tmpy;
							this.visible.add(i+","+j+";"+i+","+j);
							for(int k = i - 1; k > -1 && this.visible.contains(k+","+j+";"+(i-1)+","+j); k--) {
								this.visible.add(k+","+j+";"+i+","+j);
							}
							for(int k = j - 1; k > -1 && this.visible.contains(i+","+k+";"+i+","+(j-1)); k--) {
								this.visible.add(i+","+k+";"+i+","+j);
							}
							int [] pos4 = {i,j};
							this.gamePositions.add(pos4);
							break;
						case "F" :
							this.theMap.setFigure(i,j,new Gomme(this.tailleCase, tmpx, tmpy));
							posGhost = new int[2];
							posGhost[0] = tmpx;
							posGhost[1] = tmpy;
							this.ghosts.add(posGhost);
							this.visible.add(i+","+j+";"+i+","+j);
							for(int k = i - 1; k > -1 && this.visible.contains(k+","+j+";"+(i-1)+","+j); k--) {
								this.visible.add(k+","+j+";"+i+","+j);
							}
							for(int k = j - 1; k > -1 && this.visible.contains(i+","+k+";"+i+","+(j-1)); k--) {
								this.visible.add(i+","+k+";"+i+","+j);
							}
							int [] pos5 = {i,j};
							this.gamePositions.add(pos5);
							break;
						}
						this.state.modifyMap(i, j, str.charAt(0));
						j++;
					}
					i++;
				}
			}
			br.close();
		}
		catch (Exception e){
			System.out.println(e.toString());
		}
		assert nbrGomme > 0 : "Post condition non respectée : nombre de gomme nul";
		assert pacmanX > 0 : "Post condition non respectée : pacman non initialisé";
		assert pacmanY > 0 : "Post condition non respectée : pacman non initialisé";
		assert couleurMur == "blue" || couleurMur == "green" || couleurMur == "pink" : "Post condition non respectée : Mauvaise couleur de mur";

		this.invariant();
		BeliefState.setStaticVariables(this.gamePositions, this.visible, this.pacmanX, this.pacmanY, this.ghosts, this.tailleCase, this.nbCases);
		this.visibleBeliefState.add(new BeliefState(this.state, false));
	}
	
	public ArrayList<int[]> getGamePosition() {
		return this.gamePositions;
	}

	/**
	 * Decremente le nombre de gomme
	 */
	public void pickGom () {
		this.nbrGomme -= 1;
	}

	/**
	 * Getter pour le nombre courrant de gomme
	 *
	 * @return Le nombre de gomme
	 */
	public int getNbGom () {
		return this.nbrGomme;
	}

	/**
	 * Getter pour le tableau des figure de la map
	 *
	 * @return le tableau des figures
	 */
	public Figure[][] getMap(){
		return this.theMap.getTheMap();
	}

	public void draw () {
		this.theMap.draw();
	}

	/**
	 * Getter pour le nombre de cases de la map
	 *
	 * @return Le nombre de case
	 */
	public int getNbCases() {
		return this.nbCases;
	}

	/**
	 * Getter pour la taille des cases de la map
	 *
	 * @return La taille des cases
	 */
	public int getTailleCase() {
		return this.tailleCase;
	}

	/**
	 * Getter pour la position en X de pacman sur la map
	 *
	 * @return La position en X de pacman
	 */
	public int getPMX() {
		return this.pacmanX;
	}

	/**
	 * Getter pour la position en Y de pacman sur la map
	 *
	 * @return La position en Y de pacman
	 */
	public int getPMY() {
		return this.pacmanY;
	}


	/**
	 * Getter pour la liste des positions des fantomes sur la map
	 *
	 * @return La liste des postion des fantomes
	 */
	public ArrayList<int[]> getPGhost() {
		return this.ghosts;
	}

	protected void invariant() {
		assert this.WIDTH == Canvas.WIDTH : "Invariant violé : WIDTH a changé";
	}
	
	public boolean isVisible(int row1, int column1, int row2, int column2) {
		//System.out.println("isVisible " + row1 + "," + column1 + ";" + row2 + "," + column2);
		if(row1 == row2) {
			//System.out.println("row " + row1 + " columns " + column1 + "," + column2);
			return this.visible.contains(row1+","+Math.min(column1,column2)+";"+row2+","+Math.max(column1, column2));
		}
		if(column1 == column2) {
			//System.out.println("column " + column1 + " rows " + row1 + "," + row2);
			return this.visible.contains(Math.min(row1, row2)+","+column1+";"+Math.max(row1, row2)+","+column2);
		}
		return false;
	}
	
	public PacManLauncher getPml() {
		return this.pml;
	}
	
	public BeliefState getBeliefState() {
		return this.state;
	}
	
	public void setBeliefState(BeliefState state) {
		this.state = state;
	}
	
	public ArrayList<BeliefState> getVisibleBeliefState() {
		return this.visibleBeliefState;
	}
	
	public void setVisibleBeliefState(ArrayList<BeliefState> visibleBeliefState) {
		this.visibleBeliefState = visibleBeliefState;
	}
	
	public BeliefState getState() {
		return this.state;
	}
	
	public ArrayList<BeliefState> getVisibleState(){
		return this.visibleBeliefState;
	}

}
