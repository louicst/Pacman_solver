package logic;

import java.util.ArrayList;
import java.util.TreeSet;
import view.Gomme;

/**
 * class used to represent plan. It will provide for a given set of results an action to perform in each result
 */
class Plans{
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
    
    public ArrayList<String> getAction(int index){
        return this.actions.get(index);
    }
}

/**
 * class used to represent a transition function i.e., a set of possible belief states the agent may be in after performing an action
 */
class Result{
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
    
    public ArrayList<BeliefState> getBeliefStates(){
        return this.beliefStates;
    }
}


/**
 * Classe implémentant l'IA avec l'algorithme AND-OR Search (Expectiminimax)
 */
public class AI {
    
    // Profondeur de recherche. 3 est rapide, 4 est plus intelligent mais plus lent.
    private static final int MAX_DEPTH = 3; 

    /**
     * Fonction principale appelée par le jeu pour décider du prochain mouvement.
     */
    public static String findNextMove(BeliefState beliefState) {
        
        // 1. Générer les plans possibles (Branchement OR)
        Plans plans = beliefState.extendsBeliefState();
        
        String bestAction = PacManLauncher.UP; // Valeur par défaut
        double maxVal = Double.NEGATIVE_INFINITY;

        // 2. Parcourir chaque action possible (Racine de l'arbre)
        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);
            
            // Si aucune action associée, on passe
            if (actions.isEmpty()) continue;
            
            // Calculer la valeur de cette branche (Branchement AND)
            double currentVal = evaluateAndNode(result, MAX_DEPTH - 1);
            
            // On garde la meilleure action (MAX)
            if (currentVal > maxVal) {
                maxVal = currentVal;
                // On prend la première action de la liste équivalente (ex: UP)
                bestAction = actions.get(0); 
            }
        }
        
        return bestAction;
    }

    /**
     * Évalue un noeud AND (un Résultat contenant plusieurs BeliefStates possibles).
     * On fait la MOYENNE des valeurs (Espérance), car on ne sait pas quel état va se produire.
     */
    private static double evaluateAndNode(Result result, int depth) {
        if (result.size() == 0) return Double.NEGATIVE_INFINITY;

        double totalScore = 0;
        
        for (BeliefState state : result.getBeliefStates()) {
            totalScore += expectiminimax(state, depth);
        }
        
        // Retourne la moyenne des scores possibles
        return totalScore / result.size();
    }

    /**
     * Fonction récursive principale (Partie OR).
     * Choisit la meilleure action possible à partir d'un état donné.
     */
    private static double expectiminimax(BeliefState state, int depth) {
        // Condition d'arrêt : profondeur atteinte ou mort
        if (depth == 0 || state.getLife() <= 0) {
            return heuristic(state);
        }

        Plans plans = state.extendsBeliefState();
        
        // S'il n'y a plus de plans possibles (bloqué ou fin), on évalue
        if (plans.size() == 0) return heuristic(state);

        double maxVal = Double.NEGATIVE_INFINITY;

        // On cherche l'action qui maximise le score (MAX node)
        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            double val = evaluateAndNode(result, depth - 1);
            if (val > maxVal) {
                maxVal = val;
            }
        }
        
        return maxVal;
    }

    /**
     * Fonction d'évaluation (Heuristique) pour donner un score à un état terminal ou feuille.
     */
    private static double heuristic(BeliefState state) {
        // 1. Priorité absolue : LA SURVIE
        // Si on a perdu une vie par rapport au début (supposé 1 vie min), c'est catastrophique.
        if (state.getLife() <= 0) {
            return -1000000.0; // Mort = Très mauvaise note
        }
        
        double score = 0;

        // 2. Le Score du jeu (Gommes mangées, Fantômes mangés)
        score += state.getScore() * 10.0;

        // 3. Distance à la gomme la plus proche
        // On veut MINIMISER la distance, donc on SOUSTRAIT cette valeur.
        int dist = state.distanceMinToGum();
        if (dist != Integer.MAX_VALUE) {
            score -= (dist * 2.0); // Poids de 2 pour la distance
        } else {
            // Si aucune gomme n'est accessible (bug ou fin de niveau), petite pénalité
            score -= 1000; 
        }

        // 4. Bonus pour les fantômes mangeables (Peur > 0)
        // On encourage Pacman à chasser si les fantômes sont bleus
        int nbrGhosts = state.getNbrOfGhost();
        for(int i=0; i < nbrGhosts; i++) {
             if (state.getCompteurPeur(i) > 0) {
                 score += 200; // Gros bonus si on est dans un état où on peut manger
             }
        }

        // 5. Pénalité de sécurité (optionnelle mais conseillée)
        // Si un fantôme non effrayé est très proche, on baisse le score
        // (Note: C'est complexe à calculer sans accès direct aux positions exactes dans l'heuristique simple,
        // mais le BeliefState gère déjà la mort implicitement via getLife).

        return score;
    }
}