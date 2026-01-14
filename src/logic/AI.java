package logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

// --- Les classes Plans et Result restent inchangées ---
class Plans {
    ArrayList<Result> results;
    ArrayList<ArrayList<String>> actions;

    public Plans() {
        this.results = new ArrayList<Result>();
        this.actions = new ArrayList<ArrayList<String>>();
    }

    public void addPlan(Result beliefBeliefState, ArrayList<String> action) {
        this.results.add(beliefBeliefState);
        this.actions.add(action);
    }

    public int size() {
        return this.results.size();
    }

    public Result getResult(int index) {
        return this.results.get(index);
    }

    public ArrayList<String> getAction(int index) {
        return this.actions.get(index);
    }
}

class Result {
    private ArrayList<BeliefState> beliefStates;

    public Result(ArrayList<BeliefState> states) {
        this.beliefStates = states;
    }

    public int size() {
        return this.beliefStates.size();
    }

    public BeliefState getBeliefState(int index) {
        return this.beliefStates.get(index);
    }

    public ArrayList<BeliefState> getBeliefStates() {
        return this.beliefStates;
    }
}

// --- IA COMPLETE : SURVIE + EXPLORATION ---

public class AI {
    
    // Mémoire des cases visitées "Ligne,Colonne"
    private static Set<String> visited = new HashSet<>();
    private static final int MAX_DEPTH = 1; 

    public static String findNextMove(BeliefState currentState) {
        
        // On mémorise où on est
        Position currentPos = currentState.getPacmanPosition();         //currentPos nous donne la position de pacman
        visited.add(currentPos.getRow() + "," + currentPos.getColumn());

        Plans plans = currentState.extendsBeliefState();                //plans : toutes les action qu'on peut faire et les resultast qu'elles engendrent
        String bestAction = PacManLauncher.UP; 
        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);                        //result : liste pour une action donné, des états potentiel du plateau
            ArrayList<String> actions = plans.getAction(i);            //actions : action donné associé à result
            
            
            // Évaluation du futur
            double score = evaluateResult(result, currentPos);
         

            if (score > maxScore) {
                maxScore = score;
                bestAction = actions.get(0);
            }
        }
        return bestAction;
    }

    private static double evaluateResult(Result result, Position currentPos) {      //result : liste des états potentiel du plateau, currentPOs : celle de pacman
        if (result.size() == 0) return Double.NEGATIVE_INFINITY;
        
        double totalScore = 0;
        for (BeliefState state : result.getBeliefStates()) {
            totalScore += heuristic(state, currentPos);     //state : état d'un plateau de potentiel resultats
        }
        return totalScore;
    }

    /**
     * HEURISTIQUE COMPLETE
     * 1. Mur = PÉNALITÉ MAX
     * 2. Fantôme aligné = PÉNALITÉ MAX
     * 3. Déjà visité = Petite pénalité
     * 4. Gomme à côté = Bonus
     */
    private static double heuristic(BeliefState state, Position originalPos) {
        Position futurePos = state.getPacmanPosition();
        int r = futurePos.getRow();
        int c = futurePos.getColumn();
        double score = 0;

        // --- 1. DÉTECTION MUR (-100 000) ---
        // Si on n'a pas bougé, c'est interdit.
        if (r == originalPos.getRow() && c == originalPos.getColumn()) {
            return -100000.0;
        }
        
        // --- 2. DÉTECTION FANTÔME (La nouveauté) ---
        // On vérifie si ce futur nous met en danger immédiat
        if (isDanger(state, r, c)) {
            return -50000.0; // On fuit cette direction comme la peste !
        }

        // --- 3. SURVIE BASIQUE ---
        // Si on meurt dans cet état, c'est évidemment nul
        if (state.getLife() <= 0) {
            return  Double.NEGATIVE_INFINITY;
        }

        // --- 4. EXPLORATION (-1 si déjà vu) ---
        if (visited.contains(r + "," + c)) {
            score -= 1.0;
        }

        // --- 5. FLAIR (+1 si gomme adjacente) ---
        if (hasGumNeighbor(state, r, c)) {
            score += 1.0;
        }
        
        // On ajoute le score du jeu pour départager
        score += state.getScore();

        return score;
    }

    // Vérifie si un fantôme dangereux est trop proche ou aligné
    private static boolean isDanger(BeliefState state, int pacRow, int pacCol) {
        int nbrGhosts = state.getNbrOfGhost();
        for (int i = 0; i < nbrGhosts; i++) {
            // Si le fantôme est mangeable (bleu), pas de danger
            if (state.getCompteurPeur(i) > 0) continue;
            
            // On regarde toutes les positions possibles de ce fantôme
            TreeSet<Position> ghostPositions = state.getGhostPositions(i);
            for (Position ghostPos : ghostPositions) {
                int gRow = ghostPos.getRow();
                int gCol = ghostPos.getColumn();
                
                // Distance de Manhattan
                int dist = Math.abs(gRow - pacRow) + Math.abs(gCol - pacCol);
                
                // DANGER IMMÉDIAT : Si le fantôme est à 1 case (collision imminente)
                if (dist <= 1) return true;
                
                // DANGER LIGNE DE VUE : Si on est sur la même ligne/colonne et proche (< 5 cases)
                // On suppose qu'il nous voit et qu'il va charger.
                if ((gRow == pacRow || gCol == pacCol) && dist < 5) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasGumNeighbor(BeliefState state, int r, int c) {
        try {
            if (isGum(state.getMap(r+1, c))) return true;
            if (isGum(state.getMap(r-1, c))) return true;
            if (isGum(state.getMap(r, c+1))) return true;
            if (isGum(state.getMap(r, c-1))) return true;
        } catch (Exception e) {}
        return false;
    }

    private static boolean isGum(char cell) {
        return cell == '.' || cell == '*';
    }
}