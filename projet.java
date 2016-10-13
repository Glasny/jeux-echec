class projet{
	public static void main(String[] args){
		partie partie1 = new partie();
		partie1.lancer();
    } 
}

	class partie {
		boolean arret; // true: arrêt de la boucle dans lancer()
		private piece plateau [][]; // position des pièces sur le plateau
		private IGEchecs ige;
		private int[] svgCoordB, svgCoordN; // mémorise déplacement blanc si erreur lors du coup noir
		private piece svgPieceB, svgPieceN; // svg piece supprimée si rollback après attaque
		partie () {
			arret = false;
			plateau = new piece [8][8];
			ige = new IGEchecs();
			svgCoordB = new int[4];
			svgCoordN = new int[4];
			svgPieceB = null;
			svgPieceN = null;
			char [] typePieces = {'T', 'C', 'F', 'D', 'R', 'F', 'C', 'T'};
			char curPiece;
			for (int i=0; i<8; i++) {
				curPiece = typePieces[i];
				generePiece('P', i, 1, true);
				generePiece('P', i, 6, false);
				generePiece(curPiece, i, 0, true);
				generePiece(curPiece, i, 7, false);
				for (int j=2; j<6; j++)
					plateau[i][j] = null;
			}
			ige.affiche();
		}
		private void generePiece (char type, int col, int ligne, boolean couleur) {
			if (type == 'P')
				plateau[col][ligne] = new pion(couleur);
			else if (type == 'T')
				plateau[col][ligne] = new tour(couleur);
			else if (type == 'C')
				plateau[col][ligne] = new cavalier(couleur);
			else if (type == 'F')
				plateau[col][ligne] = new fou(couleur);
			else if (type == 'D')
				plateau[col][ligne] = new dame(couleur);
			else if (type == 'R')
				plateau[col][ligne] = new roi(couleur);
			ige.posePiece(type,couleur,col,ligne+1);
		}
		public void lancer () {
			while (!arret) {
				for (int i=0; i<4; i++) {
					svgCoordB[i] = 9;
					svgCoordN[i] = 9;
				}
				svgPieceB = null;
				svgPieceN = null;
				Terminal.ecrireStringln("Veuillez saisir l'instruction suivante : ");
				String descr = Terminal.lireString();
				try {
					analyserInstruction(descr);
				} catch (erreurInstruction e) { // En cas d'erreur, annule déplacements
					if (svgCoordB[0] != 9 || svgCoordN[0] != 9)
						rollback();
				}
			}
		}
		private void analyserInstruction(String descr) throws erreurInstruction {
			String types = "TFCDR"; // types de pièces
			String [] instr;
			String msg; // message en cas d'erreur, de mat ou d'échec
			boolean attaque; // vérif attaque/déplacement
			boolean joueur = true; // initialisation boucle: joueur blanc
			int [] coord = new int[4];
			if (!verifSyntaxe(descr)) // Vérif syntaxe instruction
				throw new erreurInstruction("Erreur de syntaxe");
			if (descr.indexOf("(=)") != -1) // Vérif match nul demandé
				finPartie("Match nul");
			else {
				instr = descr.split(" ");
				for (int i=1; i<instr.length; i++) { // Boucle info instruction par joueur
					// Vérif en passant et pion à dame pas avec pièce != P
					if ((instr[i].indexOf('D', 1) != -1 || instr[i].indexOf("e.p.") != -1) && types.indexOf(instr[i].charAt(0)) != -1)
						throw new erreurInstruction("Erreur de syntaxe");
					msg = "Tour joueur "; // Message de fin de coup
					msg += (joueur)? "blanc" : "noir";
					msg += " : ";
					if (instr[i].indexOf("O-O") == -1) // coup != roque
						coord = verifCoord(instr[i], joueur, msg); // Vérif + récup coord: 0=col départ, 1=lig départ, 2=col arrivée, 3=lig arrivée
					faireCoup(coord, joueur, instr[i], msg);
					if (arret) // Vérif arret (si mat lors du coup)
						break;
					if (joueur) {// 1er tour de boucle : changement joueur + msg transition
						joueur = false;
						Terminal.ecrireStringln("Pour continuer tapez <entree>");
						Terminal.lireString();
					}
				}
			}
		}
		private static boolean verifSyntaxe(String descr) {
			// Notation complète
			String notC = "[TFCDR]?[a-h][1-8][\\-|x][a-h][1-8]";
			// Notation abrégée : on précise colonne ou ligne si ambiguité avec une autre pièce
			String notA = "[TFCDR]?([a-h]|[1-8])?x?[a-h][1-8]";
			String roque = "O\\-O(\\-O)?"; // Lettre o maj
			// + : échec  -  ++|# : mat  -  e.?p. : prise en passant  -  =?D pion à dame
			String coup = "(((("+notC+")|("+notA+"))(e.?p.|=?D)?)|("+roque+"))(\\+|\\+\\+|#)?";
			// Deuxième coup facultatif : si blancs mettent mat  ou  \(=\) : match nul
			if (descr.matches("^[0-9]+. (("+coup+"( "+coup+")?)|\\(=\\))$"))
				return true;
			return false;
		}
		private int[] verifCoord(String instr, boolean joueur, String msg) throws erreurInstruction  {
			String types = "TFCDR";
			String colonnes = "abcdefgh";
			String lignes = "12345678";
			int [] coord = new int [4]; // ordre: colonne départ, ligne départ, colonne arrivée, ligne arrivée
			char t;
			boolean attaque;
			int i = 0;
			if (types.indexOf(instr.charAt(0)) != -1) { // type de pièce
				t = instr.charAt(0);
				i++;
			} else // pion
				t = 'P';
			attaque = instr.indexOf('x') != -1; // true: attaque, false: déplacement
			 // notation complète: char 0,1,2 ou char 1,2,3 = col,ligne,(-|x)
			if (instr.length() >= i+3 && colonnes.indexOf(instr.charAt(i)) != -1 && lignes.indexOf(instr.charAt(i+1)) != -1 && (instr.charAt(i+2) == 'x' || instr.charAt(i+2) == '-')) {
				coord[0] = colonnes.indexOf(instr.charAt(i)); // col départ
				coord[1] = lignes.indexOf(instr.charAt(i+1)); // lig départ
				coord[2] = colonnes.indexOf(instr.charAt(i+3)); // col arrivée
				coord[3] = lignes.indexOf(instr.charAt(i+4));  // lig arrivée
				piece p = this.plateau[coord[0]][coord[1]];
				if (p == null || p.getType() != t || p.getCouleur() != joueur) // vérif pièce existe, bon type, bonne couleur
					throw new erreurInstruction(msg+"Pas de pièce correspondante");
				if (!p.verifDeplacement(coord[0], coord[1], coord[2], coord[3], attaque, joueur, plateau)) // Vérif déplacement possible
					throw new erreurInstruction(msg+"Déplacement impossible");
			} else // notation abrégée
				coord = trouverPieceAbreg (instr, t, joueur, attaque, msg);
			if (instr.indexOf("p.") != -1) { // prise en passant
				if (instr.indexOf('x') == -1)
					throw new erreurInstruction(msg+"Erreur syntaxe prise en passant: préciser attaque");
				if (!enPassant(coord, joueur)) // effectue mouvement si pas d'erreur
					throw new erreurInstruction(msg+"Prise en passant impossible");
			} else if (!checkArrivee(coord[2], coord[3], joueur, attaque)) { // vérif case arrivée
				msg += (attaque)? "Attaque impossible, pas de pièce adverse sur la case d'arrivée" : "Déplacement impossible, case d'arrivée occupée";
				throw new erreurInstruction(msg);
			}
			return coord;
		}
		// Vérifie si case d'arrivée correspond au type de coup (vide si dépl, occupée par pièce adv si attaque)
		private boolean checkArrivee (int col, int lig, boolean joueur, boolean attaque) {
			piece a = this.plateau[col][lig]; // pièce arrivée
			if (a == null && !attaque) // déplacement
				return true;
			if (a != null && a.getCouleur() != joueur && attaque) // attaque
				return true;
			return false;
		}
		private int[] trouverPieceAbreg (String instr, char type, boolean joueur, boolean attaque, String msg) throws erreurInstruction {
			String types = "TFCDR";
			String colonnes = "abcdefgh";
			String lignes = "12345678";
			int[] coord= new int[4];
			int i=0;
			int debutC = 0; // bornes pour trouver case de départ : modifiées si colonne ou ligne précisée dans instruction
			int debutL = 0; // Par défaut: tout le plateau -> début=0, fin=7
			int finC = 7;
			int finL = 7;
			if (type != 'P') // type précisé dans instruction, passe au carac suivant
				i++;
			if (colonnes.indexOf(instr.charAt(i)) != -1 && lignes.indexOf(instr.charAt(i+1)) == -1) { // carac = colonne départ: pas suivi de ligne
				coord[0] = colonnes.indexOf(instr.charAt(i));
				debutC = coord[0]; // modif bornes recherche
				finC = coord[0];
				i++;
			} else if (lignes.indexOf(instr.charAt(i)) != -1) { // carac = ligne de départ
				coord[1] = lignes.indexOf(instr.charAt(i));
				debutL = coord[1]; // modif bornes recherche
				finL = coord[1];
				i++;
			}
			if (instr.charAt(i) == 'x') // attaque précisée, passage au carac suivant
				i++;
			coord[2] = colonnes.indexOf(instr.charAt(i)); // colonne arrivée
			coord[3] = lignes.indexOf(instr.charAt(i+1)); // ligne arrivée
			for (i=debutC; i<=finC; i++) { // boucle recherche pièce de départ
				for (int j=debutL; j<=finL; j++) {
					// vérif: pièce existe, correspond à type, correspond à joueur, déplacement possible jusqu'à case d'arrivée
					if (this.plateau[i][j] != null && this.plateau[i][j].getType() == type && this.plateau[i][j].getCouleur() == joueur && this.plateau[i][j].verifDeplacement(i, j, coord[2], coord[3], attaque, joueur, plateau)) {
						coord[0] = i;
						coord[1] = j;
						return coord;
					}
				}
			}
			throw new erreurInstruction(msg+"Déplacement impossible"); // pas de pièce correspondante trouvée
		}
		private void faireCoup (int [] coord, boolean joueur, String instr, String msg) throws erreurInstruction {
			if (instr.indexOf("O-O") != -1) { // roque 
				if (!roque(instr, joueur)) // Si pas d'erreur, fait le déplacement
					throw new erreurInstruction(msg+"Roque impossible");
				else
					msg += "Roque. ";
				if (instr.indexOf('+') != -1 || instr.indexOf('#') != -1) { // Svg coordonnées de la tour si échec ou mat pour vérif
					if (joueur) {
						coord[1] = 0;
						coord[3] = 0;
					} else {
						coord[1] = 7;
						coord[3] = 7;
					}
					if (instr.indexOf("O-O-O") != -1) {
						coord[0] = 0;
						coord[2] = 3;
					} else {
						coord[0] = 7;
						coord[2] = 5;
					}
				}
			} else if (instr.indexOf('D', 1) != -1) { // pion à dame
				if (!pionDame(coord, joueur)) // Fait le dépl si pas d'erreur
					throw new erreurInstruction(msg+"Pion à dame impossible");
				msg += "Pion à dame. ";
			} else if (instr.indexOf("p.") != -1) // en passant - appel à enPassant dans verifCoord
				msg += "Prise en passant. ";
			else { // coup normal
				piece p = plateau[coord[0]][coord[1]]; // pièce déplacée
				if (joueur) // svg coord initiale de la pièce déplacée
					svgCoordB = coord;
				else
					svgCoordN = coord;
				p = plateau[coord[2]][coord[3]]; // pièce à la position d'arrivée
				if (p != null) { // attaque
					if (joueur) // svg pièce supprimée
						svgPieceB = p;
					else
						svgPieceN = p;
					supprimerPiece(coord[2], coord[3]);
				}
				deplacerPiece(coord[0], coord[1], coord[2], coord[3]);
			}
			ige.affiche();
			if ((instr.indexOf("++") != -1 || instr.indexOf('#') != -1)) { // mat
				if (!verifMat(coord, joueur))
					throw new erreurInstruction(msg+"Mauvaise déclaration d'échec et mat");
				else 
					finPartie(msg+"Échec et mat");
			} else { 
				if (instr.indexOf('+') != -1) { // échec
					if (!verifEchec(coord, joueur))
						throw new erreurInstruction(msg+"Mauvaise déclaration d'échec");
					msg += "Échec";
				}
				Terminal.ecrireStringln(msg); // affiche msg fin de tour
			}
		}
		private void deplacerPiece(int cD, int lD, int cA, int lA) {
			piece p = this.plateau[cD][lD];
			placerPiece(cA, lA, p); // place à case d'arrivée
			supprimerPiece(cD, lD); // supprime de case de départ
		}
		private void placerPiece(int cA, int lA, piece p) {
			plateau[cA][lA] = p;
			ige.posePiece(p.getType(),p.getCouleur(),cA,lA+1);
		}
		private void supprimerPiece(int c, int l) {
			plateau[c][l] = null;
			ige.libereCase(c,l+1);
		}
		private boolean roque (String instr, boolean joueur) {
			int l = (joueur)? 0:7; // ligne correspondant à départ du joueur -> 0 : blanc  -  7 : noir
			if (plateau[4][l].getType() != 'R' || plateau[4][l].getCouleur() != joueur) // Vérification roi à sa place
				return false;
			if (instr.indexOf("O-O-O") == -1) { // petit roque
				if (plateau[7][l].getType() != 'T' || plateau[7][l].getCouleur() != joueur) // Vérif "petite" tour
					return false;
				if (plateau[5][l] != null || plateau[6][l] != null) // Vérif cases intermédiaires vides
					return false;
				if (joueur)
					svgCoordB[1] = 0; // svg pour rollback
				else
					svgCoordN[1] = 0;
				deplacerPiece(4, l, 6, l);
				deplacerPiece(7, l, 5, l);
			} else { // grand roque
				if (plateau[0][l].getType() != 'T' || plateau[0][l].getCouleur() != joueur) // Vérif "grande" tour
					return false;
				if (plateau[1][l] != null || plateau[2][l] != null || plateau[3][l] != null) // Vérif cases intermédiaires vides
					return false;
				if (joueur)
					svgCoordB[1] = 1; // svg pour rollback
				else
					svgCoordN[1] = 1;
				deplacerPiece(0, l, 3, l);
				deplacerPiece(4, l, 2, l);
			}
			if (joueur)
				svgCoordB[0] = 8; // svg pour rollback
			else
				svgCoordN[0] = 8;
			return true;
		}
		private boolean pionDame (int [] coord, boolean joueur) {
			if (joueur && (coord[1] != 6 || coord[3] != 7) || !joueur && (coord[1] != 1 || coord[3] != 0)) // Vérif position pion
				return false;
			if (joueur) { // svg pour rollback
				svgCoordB[0] = 8; // indique coup spécial
				svgCoordB[1] = 2; // indique pion à dame
				svgCoordB[2] = coord[0]; // colonne initiale du pion
				svgCoordB[3] = coord[2]; // colonne d'arrivée du pion (dame) : change si attaque
			} else {
				svgCoordN[0] = 8;
				svgCoordN[1] = 2;
				svgCoordN[2] = coord[0];
				svgCoordN[3] = coord[2];
			}
			if (plateau[coord[2]][coord[3]] != null) { // pièce à la case d'arrivée: attaque
				if (joueur) // svg pièce attaquée pour rollback
					svgPieceB = plateau[coord[2]][coord[3]];
				else
					svgPieceN = plateau[coord[2]][coord[3]];
				supprimerPiece(coord[2], coord[3]);
			}
			supprimerPiece(coord[0], coord[1]); // Suppression du pion à la case départ
			generePiece('D', coord[2], coord[3], joueur); // Création dame à la case d'arrivée
			return true;
		}
		private boolean enPassant (int[] coord, boolean joueur) {
			if (joueur && coord[1] != 4 || !joueur && coord[1] != 3) // Vérif pion d'attaque sur la bonne ligne
				return false;
			if (!checkArrivee(coord[2], coord[3], joueur, false)) // Vérif case d'arrivée vide
				return false;
			if (!checkArrivee(coord[2], coord[1], joueur, true) || plateau[coord[2]][coord[1]].getType() != 'P') // Vérif position pion adverse
				return false;
			deplacerPiece(coord[0], coord[1], coord[2], coord[3]); // Déplacement du pion
			if (joueur) { // svg pour rollback
				svgPieceB = plateau[coord[2]][coord[1]]; // pion adverse
				svgCoordB[0] = 8; // coup spécial
				svgCoordB[1] = 3; // en passant
				svgCoordB[2] = coord[0]; // colonne départ
				svgCoordB[3] = coord[2]; // colonne arrivée
			} else {
				svgPieceN = plateau[coord[2]][coord[1]]; // pion adverse
				svgCoordN[0] = 8; // coup spécial
				svgCoordN[1] = 3; // en passant
				svgCoordN[2] = coord[0]; // colonne départ
				svgCoordN[3] = coord[2]; // colonne arrivée
			}
			supprimerPiece(coord[2], coord[1]); // suppression pion adverse
			return true;
		}
		private boolean verifEchec (int[] coord, boolean joueur) {
			piece p = plateau[coord[2]][coord[3]];
			int [] r = trouverRoi(!joueur); // 0: col 1: lig
			if (r[0] == -1) // Pas de roi adverse trouvé
				return false;
			if (p.verifDeplacement(coord[2], coord[3], r[0], r[1], true, joueur, plateau)) // Vérif échec par la pièce jouée
				return true;
			if (verifEchecDecouv(coord, r, joueur)) // Vérif échec à la découverte
				return true;
			return false;
		}
		// Vérif échec à la découverte : si roi sur mm col/lig/diago que pièce jouée, on regarde la 1ère pièce "derrière"
		private boolean verifEchecDecouv (int[] coord, int[] r, boolean joueur) {
			int difC = coord[0]-r[0]; // Diff entre col départ et col roi
			int absDifC = Math.abs(difC);
			int difL = coord[1]-r[1]; // Diff entre lig départ et lig roi
			int absDifL = Math.abs(difL);
			// Vérif échec à la découverte: Si pièce sur mm lig/col/diago départ que roi et lig/col/diago arrivée != roi
			if (difL == 0 && coord[1] != coord[3] || difC == 0 && coord[0] != coord[2] || absDifC == absDifL && difC*(coord[3]-r[1]) != difL*(coord[2]-r[0])) {
				int pasC = 0; // changement col à chq ité : -1 ou +1 si difC != 0, -1 sinon (pas de chgt de col)
				int limC = -1; // limite col : 0 ou 7 si difC != 0, -1 sinon (pas de chgt de col)
				int col = coord[0]; // col case à vérifier
				int pasL = 0; // changement lig à chq ité
				int limL = -1; // limite lig
				int lig = coord[1]; // lig case à vérifier
				if (difC != 0) { // roi sur colonne différente que col départ pièce : depl sur col ou diago
					pasC = difC/absDifC; // pas à chq ité (+1 si roi à gauche, -1 si roi à droite)
					limC = (pasC>0)? 7:0; // limite col de recherche (7 si roi à gauche, 0 si à droite)
				}
				if (difL != 0) { // roi sur ligne différente que lig départ pièce: depl sur lig ou diago
					pasL = difL/absDifL; // pas à chq ité (+1 si roi en dessous, -1 si roi au dessus)
					limL = (pasL>0)? 7:0; // limite lig de recherche (7 si roi en dessous, 0 si au dessus)
				}
				while (col != limC && lig != limL) { // boucle case suivante
					col += pasC;
					lig += pasL;
					if (plateau[col][lig] != null) { // Vérif pièce existe
						if (plateau[col][lig].getCouleur() != joueur) // Pièce n'appartient pas à joueur
							return false;
						int[][] depl = plateau[col][lig].getDepl(); // récupération déplacement pièce
						if (depl[0][0] != 7) // portée pièce != 7 : échec découv impossible
							return false;
						for (int i=1; i<depl.length; i++) { // Boucle déplacements possible de pièce
							if (depl[i][0]*absDifL == depl[i][1]*absDifC || depl[i][1]*absDifL == depl[i][0]*absDifC)
								return true; // Déplacement correspond : échec
						}
						return false; // Aucun déplacement ne correspond : pas d'échec
					}
				}
			}
			return false; // col/lig/diago départ pièce != roi : échec découv impossible
		}
		private boolean verifMat (int[] coord, boolean joueur) {
			int [] r = trouverRoi(!joueur); // 0: col 1: lig
			int dC, fC, dL, fL; // coord cases adjacentes
			dC = (r[0]-1 >= 0)? r[0]-1 : 0;
			fC = (r[0]+1 <= 7)? r[0]+1 : 7;
			dL = (r[1]-1 >= 0)? r[1]-1 : 0;
			fL = (r[1]+1 <= 7)? r[1]+1 : 7;
			int [] echec = {0, 0, 0}; // 0: indique nb d'échec, 1: col pièce échec, 2: lig pièce échec
			for (int i=dC; i<=fC; i++) { // boucle cases adj + case roi
				for (int j=dL; j<=fL; j++) {
					if (i == r[0] && j == r[1]) { // Case du roi adv
						echec = verifAcces(i, j, joueur, true); // Nb de pièces qui mettent roi en échec
						if (echec[0] == 0) // Roi adv pas en échec
							return false;
					} else if (plateau[i][j] == null || plateau[i][j].getCouleur() == joueur) { // case adj vide ou contrôlée par pièce adverse
						if (verifAcces(i, j, joueur, false)[0] == 0) // Roi adv peut aller sur une case adj : pas mat
							return false;
					}
				}
			}
			if (echec[0] == 2) // Plus d'une pièce met échec, pas possible d'interposer une pièce : mat
				return true;
			if (verifInterpo(echec[1], echec[2], r, joueur, true)) {
				return false; // Une pièce adv peut attaquer la pièce échec sans provoquer un nouvel échec
			}
			int difC = r[0] - echec[1]; // diff entre col roi et col pièce échec
			int absDifC = Math.abs(difC);
			int difL = r[1] - echec[2]; // diff entre lig roi et lig pièce échec
			int absDifL = Math.abs(difL);
			 // portée pièce échec = 7 et au moins une case entre elle et roi adv, vérif cases intermédiaires
			if (plateau[echec[1]][echec[2]].getDepl()[0][0] == 7 && (absDifC > 1 || absDifL > 1)) {
				int pasC = 0; // changement col à chq ité : -1 ou +1 si difC != 0, -1 sinon (pas de chgt de col)
				int col = echec[1]; // col case à vérifier
				int pasL = 0; // changement lig à chq ité
				int lig = echec[2]; // lig case à vérifier
				if (difC != 0) // roi sur colonne différente que col départ pièce : depl sur col ou diago
					pasC = difC/absDifC; // pas à chq ité (-1 si roi à gauche, +1 si roi à droite)
				if (difL != 0) // roi sur ligne différente que lig départ pièce: depl sur lig ou diago
					pasL = difL/absDifL; // pas à chq ité (-1 si roi en dessous, +1 si roi au dessus)
				while (col != r[0] || lig != r[1]) { // boucle cases intermédiaires
					col += pasC;
					lig += pasL;
					if (verifInterpo(echec[1], echec[2], r, joueur, false))
						return false; // une pièce adv peut s'interposer sans provoquer un nouvel échec : pas de mat
				}
			}
			return true; // pas d'interposition possible : mat
		}
		// Renvoi coordonnées du roi adv (couleur = !joueur)
		private int[] trouverRoi (boolean couleur) {
			int [] coord = {-1,-1}; // val par défaut
			for (int i=0; i<8; i++) {
				for (int j=0; j<8; j++) {
					if (plateau[i][j] != null && plateau[i][j].getType() == 'R' && plateau[i][j].getCouleur() == couleur) {
						coord[0] = i; // col roi
						coord[1] = j; // lig
						return coord;
					}
				}
			}
			return coord; // roi non trouvé, renvoi {-1, -1} -> erreur
		}
		/* Appelé dans verifMat: - si verifEchec = true : vérif nb de pièces qui mettent roi en échec + renvoie les coordonnée de la 1ère 
		   pièce qui met échec pour vérif si pièce adv peut s'interposer: nb[0] : nb d'échecs - nb[1] : col pièce échec - nb[2] : lig pièce échec
		 - sinon: vérif si case adj au roi controlée
		*/
		private int[] verifAcces(int c, int l, boolean joueur, boolean verifEchec) {
			int [] nb = {0, 0, 0}; // Calcul nb de pièces qui mettent en échec
			for (int i=0; i<8; i++) { // boucle col
				for (int j=0; j<8; j++) { // boucle lig
					if (plateau[i][j] != null && plateau[i][j].getCouleur() == joueur) { // case non vide et appartient à joueur
						if (plateau[i][j].verifDeplacement(i, j, c, l, true, joueur, plateau)) { // Case (c,l) attaquée par pièce de joueur
							nb[0]++; // si verifEchec, nb de pièces qui mettent échec
							if (verifEchec && nb[0] == 1) { // Si verifEchec et 1 seule pièce met échec : svg coordonnées pièce
								nb[1] = i; // col
								nb[2] = j; // lig
							} else // case adj pas accessible ou 2 pièces trouvées qui mettent échec
								return nb;
						}
					}
				}
			}
			return nb; // Si verifEchec : nb de pièces qui mettent échec (0 ou 1) + coordonnée 1ère pièce, sinon case adj accessible
		}
		// Vérif si pièce adv à joueur peut s'interposer à un mat : attaque si case pièce échec, sinon case intermédiaire
		private boolean verifInterpo (int c, int l, int[] r, boolean joueur, boolean attaque) {
			int [] coord = new int[4]; // coord pour vérif si interposition pièce adv ne provoque pas un autre échec
			coord[2] = c; // col case d'interposition
			coord[3] = l; // ligne
			for (int i=0; i<8; i++) { // boucle col
				for (int j=0; j<8; j++) { // boucle lig
					if (plateau[i][j] != null && plateau[i][j].getCouleur() == !joueur && (i != r[0] || j != r[1])) { // case occupée par pièce adv != roi
						if (plateau[i][j].verifDeplacement(i, j, c, l, attaque, !joueur, plateau)) { // Pièce adv peut s'interposer
							coord[0] = i;
							coord[1] = j;
							if (!verifEchecDecouv(coord, r, !joueur)) // Vérif dépl pièce adv ne provoque pas un nouvel échec
								return true; // Pièce peut s'interposer au mat
						}
					}
				}
			}
			return false; // Pas d'interposition possible
		}
		private void finPartie (String msg) {
			arret = true; // arrêt boucle lancer
			Terminal.ecrireStringln(msg); // msg de fin
		}
		private void rollback () {
			int [] coord = svgCoordB; // Initialisation joueur blanc
			piece svgP = svgPieceB;
			int lR = 0; // ligne roque
			int lPd = 6; // pion dame: avant dernière ligne
			int lPd2 = 7; // pion dame: dernière ligne
			int lEp1 = 4; // en passant: ligne départ
			int lEp2 = 5; // en passant: ligne arrivée
			for (int i=0; i<2; i++) {
				if (coord[0] == 8) { // coup spécial
					if (coord[1] == 0) { // petit roque
						deplacerPiece(6, lR, 4, lR);
						deplacerPiece(5, lR, 7, lR);
					} else if(coord[1] == 1) { // grand roque
						deplacerPiece(3, lR, 0, lR);
						deplacerPiece(2, lR, 4, lR);
					} else if(coord[1] == 2) { // pion à dame
						generePiece('P', coord[2], lPd, true); // Replace le pion
						supprimerPiece(coord[3], lPd2); // Supprime la dame
						if (svgP != null) // Si pièce attaquée, la replace
							placerPiece(coord[3], lPd2, svgP);
					} else if(coord[1] == 3) { // prise en passant
						deplacerPiece(coord[3], lEp2, coord[2], lEp1);
						placerPiece(coord[3], lEp1, svgP);
					}
				} else if (coord[0] != 9) {
					deplacerPiece(coord[2], coord[3], coord[0], coord[1]); // Replace pièce déplacée
					if (svgP != null) // Si pièce attaquée, la replace
						placerPiece(coord[2], coord[3], svgP);
				}
				coord = svgCoordN; // Initialisation joueur noir
				svgP = svgPieceN;
				lR = 7;
				lPd = 1;
				lPd2 = 0;
				lEp1 = 3;
				lEp2 = 2;
			}
			ige.affiche();
		}
	}
	
	class piece {
		private char type;
		private boolean couleur;
		private int[][] depl;
		piece(char t, boolean c) {
			type = t;
			couleur = c;
		}
		public boolean verifDeplacement (int cD, int lD, int cA, int lA, boolean attaque, boolean couleur, piece[][] plateau) {
			if (cD == cA && lD == lA) // pas de déplacement : traitement plus rapide pour verifAcces
				return false;
			int [][] depl = this.getDepl();
			int dist = depl[0][0]; // distance maximale parcourable
			int difC = Math.abs(cA-cD); // distance verticale entre arrivée et départ
			int difL = Math.abs(lA-lD); // distance horizontale entre arrivée et départ
			if (difC > dist || difL > dist) // distance à parcourir supérieur à distance max
				return false;
			boolean verif = false;
			for (int i=1; i<depl.length; i++) { // Vérif déplacement
				if (difC*depl[i][0] == difL*depl[i][1] || difL*depl[i][0] == difC*depl[i][1]) {
					verif = true; // déplacement déclaré correspond au type de pièce
					break;
				}
			}
			if (!verif) // Pas de déplacement correspondant dans depl
				return false;
			if (dist == 7 && (difC >= 2 || difL >= 2)) { // Mouvement sur plusieurs case: vérification pas de pièce intermédiaire
				int l = lD;
				int c = cD;
				if (difC >= 2) {
					for (int j=1; j<difC; j++) {
						if (difL != 0) // Si déplacement diagonal : ligne suivante
							l += (lA-lD)/difL;
						c += (cA-cD)/difC; // colonne suivante
						if (plateau[c][l] != null)
							return false;
					}
				} else {
					for (int j=1; j<difL; j++) {
						if (difC != 0) // Si déplacement diagonal: colonne suivante
							c += (cA-cD)/difC;
						l += (lA-lD)/difL; // ligne suivante
						if (plateau[c][l] != null)
							return false;
					}
				}
			}
			return true;
		}
		public boolean getCouleur () {
			return this.couleur;
		}
		public char getType () {
			return this.type;
		}
		public int[][] getDepl() {
			return this.depl;
		}
		protected void setDepl(int[][] d) {
			this.depl = d;
		}
	}
	
	class pion extends piece {
		private int[][] depl = {{0}};
		pion (boolean c) {
			super('P', c);
			super.setDepl(depl);
		}
		// Déplacement pion incompatible avec verifDeplacement dans classe piece, redéfinition méthode
		public boolean verifDeplacement (int cD, int lD, int cA, int lA, boolean attaque, boolean couleur, piece[][] plateau) {
			if (Math.abs(lD-lA) > 2 || Math.abs(cD-cA) > 1 || lD == lA) // Déplacement impossible: traitement plus rapide pour verifAccess
				return false;
			int difL = (couleur)? 1:-1; // changement de ligne
			boolean depart = (couleur && lD == 1 || !couleur && lD == 6); // Vérification ligne de départ
			if (lD+difL != lA) { // Vérif ligne
				if (!depart || depart && lD+2*difL != lA) // vérif pion pas sur ligne départ ou ligne départ et difL > 2
					return false;
			}
			 // si coup de départ = double déplacement: vérif pas de pièce intermédiaire
			if (lA-lD == 2 && plateau[cD][2] != null || lD-lA == 2 && plateau[cD][5] != null)
				return false;
			if (attaque && (Math.abs(cD-cA) != 1 || Math.abs(lA-lD) != 1)) // Vérif attaque
				return false;
			if (!attaque && cD != cA) // Vérif déplacement
				return false;
			return true;
		}
	}
	
	class tour extends piece {
		private int[][] depl = {{7}, {0,1}}; // 0: portée  1: dépl horiz ou verti
		tour (boolean c) {
			super('T', c);
			super.setDepl(depl);
		}
	}
	
	class cavalier extends piece {
		private int[][] depl = {{2}, {2,1}};
		cavalier (boolean c) {
			super('C', c);
			super.setDepl(depl);
		}
	}
	
	class fou extends piece {
		private int[][] depl = {{7}, {1,1}}; // 1: Déplacement diago
		fou (boolean c) {
			super('F', c);
			super.setDepl(depl);
		}
	}
	
	class dame extends piece {
		private int[][] depl = {{7}, {1,1}, {0,1}};
		dame (boolean c) {
			super('D', c);
			super.setDepl(depl);
		}
	}
	
	class roi extends piece {
		private int[][] depl = {{1}, {1,1}, {0,1}};
		roi (boolean c) {
			super('R', c);
			super.setDepl(depl);
		}
	}
	
	class erreurInstruction extends Exception {
		erreurInstruction(String descr) {
			Terminal.ecrireStringln(descr);
		}
	}
