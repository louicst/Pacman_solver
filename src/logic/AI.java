package logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import java.util.TreeSet;

public class AI {
    private static Map<String, Integer> visited = new HashMap<>();
    private static final int MAX_DEPTH = 3; 

    public static String findNextMove(BeliefState currentState) {
        
        // 1. Update History
        Position currentPos = currentState.getPacmanPosition();
        String currentKey = currentPos.getRow() + "," + currentPos.getColumn();
        visited.put(currentKey, visited.getOrDefault(currentKey, 0) + 1);

        // 2. Debug Feedback
        feedback(currentState);

        Plans plans = currentState.extendsBeliefState();
        String bestAction = PacManLauncher.UP; 
        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            if (plans.getAction(i).isEmpty()) continue;
            
            boolean startsWithUncertainty = (result.size() > 1);

            double score = evaluateANDNode(result, currentState, MAX_DEPTH - 1, startsWithUncertainty);

            score += Math.random() * 0.01; 

            if (score > maxScore) {
                maxScore = score;
                bestAction = plans.getAction(i).get(0);
            }
        }
        
        //waitForUserPopup();
        return bestAction;
    }

    private static void waitForUserPopup() {
        try {
            JOptionPane.showMessageDialog(null, "Score calculated. Press OK for next move.", "AI Debugger", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {}
    }

    // --- AND NODE (Environment chooses Min) ---
    private static double evaluateANDNode(Result result, BeliefState parent, int depth, boolean isInvisibleContext) {
        double minScore = Double.POSITIVE_INFINITY;
        boolean hasValidScenario = false;
        boolean currentContextIsInvisible = isInvisibleContext || (result.size() > 1);

        for (BeliefState nextState : result.getBeliefStates()) {
            hasValidScenario = true;
            double val;

            if (nextState.getLife() < parent.getLife() || nextState.getLife() <= 0) {
                if (currentContextIsInvisible) val = -500000.0; 
                else val = -1000000000.0; 
            } else {
                val = deepSearch(nextState, parent, depth, currentContextIsInvisible);
            }

            if (val < minScore) minScore = val;
        }
        return hasValidScenario ? minScore : -1000000000.0;
    }

    // --- OR NODE (Recursive Step) ---
    private static double deepSearch(BeliefState state, BeliefState parent, int depth, boolean isInvisibleContext) {
        if (depth == 0) return heuristic(state, parent, isInvisibleContext);

        Plans futurePlans = state.extendsBeliefState();
        if (futurePlans.size() == 0) return heuristic(state, parent, isInvisibleContext);

        double maxScore = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < futurePlans.size(); i++) {
            Result res = futurePlans.getResult(i);
            double score = evaluateANDNode(res, state, depth - 1, isInvisibleContext);
            if (score > maxScore) maxScore = score;
        }
        return maxScore;
    }

    private static double heuristic(BeliefState state, BeliefState parentState, boolean isUncertain) {
        return getObjectiveScore(state, parentState, isUncertain) - getDangerScore(state);
    }

    private static double getObjectiveScore(BeliefState state, BeliefState parent, boolean isUncertain) {
        double score = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        // 1. COIN DENSITY (Replacing simple "Nearest Coin")
        // Instead of just finding one coin, we check if we are moving INTO a cluster.
        score += getCoinDensityScore(state, pac);
        
        // 2. SMART GHOST HUNTING (Time Calculation)
        // Only if we are in a visible context (don't chase ghosts in fog)
        if (!isUncertain) {
            score += getGhostHuntingScore(state, pac);
        }

        // 3. EXPLORATION BONUS (Only if uncertain/invisible mode)
        // If we can see everything, we don't need to explore blindly, we just hunt coins/ghosts.
        if (isUncertain && visited.getOrDefault(key, 0) == 0) {
            score += 200.0; 
        }

        // 4. SCORE DIFFERENCE
        if (state.getScore() > parent.getScore()) {
            score += (state.getScore() - parent.getScore()) * 10.0;
        }

        return score;
    }

    // --- NEW: COIN DENSITY CALCULATION ---
    private static double getCoinDensityScore(BeliefState state, Position pac) {
        char[][] map = state.getMap();
        int radius = 3; // Check 3 cells around Pacman (7x7 grid)
        int coinCount = 0;
        double minDist = Double.MAX_VALUE;

        // Scan the area around Pacman
        for (int r = pac.getRow() - radius; r <= pac.getRow() + radius; r++) {
            for (int c = pac.getColumn() - radius; c <= pac.getColumn() + radius; c++) {
                // Bounds check
                if (r >= 0 && r < map.length && c >= 0 && c < map[0].length) {
                    char cell = map[r][c];
                    if (cell == '.' || cell == '*') {
                        coinCount++; // Found a coin in the cluster!
                        
                        // Also keep track of nearest coin for basic guidance
                        double d = Math.abs(r - pac.getRow()) + Math.abs(c - pac.getColumn());
                        if (d < minDist) minDist = d;
                    }
                }
            }
        }

        double score = 0;
        
        // Reward 1: Density (Cluster)
        // Walking towards a group of 5 coins is better than walking towards 1 coin.
        if (coinCount > 0) {
            score += coinCount * 100.0; 
        }

        // Reward 2: Proximity (Guidance)
        // Still helpful to know exactly where the closest one is to start the chain.
        if (minDist != Double.MAX_VALUE) {
            score += (2000.0 / (minDist + 1));
        }

        return score;
    }

    // --- NEW: SMART GHOST HUNTING (TIME CHECK) ---
    private static double getGhostHuntingScore(BeliefState state, Position pac) {
        double score = 0;
        
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            int fearTimer = state.getCompteurPeur(i);
            
            // Only calculate if ghost is scared
            if (fearTimer > 0) {
                TreeSet<Position> positions = state.getGhostPositions(i);
                if (positions.isEmpty()) continue;

                // Assume visible context -> usually 1 position. If uncertain, we skip this function in heuristic.
                Position ghostPos = positions.first(); 
                
                int dist = Math.abs(ghostPos.getRow() - pac.getRow()) + Math.abs(ghostPos.getColumn() - pac.getColumn());
                
                // === THE CALCULATION ===
                // We need: Distance to reach him + Safety Buffer (2 moves)
                int requiredTime = dist + 2;

                if (fearTimer >= requiredTime) {
                    // WE HAVE TIME! CHASE HIM!
                    // The closer we are, the higher the score.
                    // Base bonus (5000) + Proximity bonus.
                    score += 5000.0 + (2000.0 / (dist + 1));
                } else {
                    // NOT ENOUGH TIME.
                    // Treat him as normal danger (handled in getDangerScore) or just ignore.
                    // We definitely do NOT give points for walking towards him.
                }
            }
        }
        return score;
    }

    private static double getDangerScore(BeliefState state) {
        double danger = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        int nbrVisits = visited.getOrDefault(key, 0);
        if (nbrVisits > 0) {
            danger += Math.pow(nbrVisits, 2) * 50.0; 
        }

        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            // If scared, ignore danger (unless time is low, but that's handled by not giving bonus)
            if (state.getCompteurPeur(i) > 0) continue; 
            
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) continue;

            for (Position gp : positions) {
                int dist = Math.abs(gp.getRow() - pac.getRow()) + Math.abs(gp.getColumn() - pac.getColumn());
                if (dist < 3) {
                    danger += 1000.0 / (dist + 1);
                }
            }
        }
        return danger;
    }

 // --- UPDATED FEEDBACK WITH GHOST POSITIONS ---
    public static void feedback(BeliefState currentState) {
        System.out.println("\n=== ANALYSE AND-OR (Density & Timing) ===");
        Position cur = currentState.getPacmanPosition();
        System.out.println("Pos Pacman: " + cur.getRow() + "," + cur.getColumn());

        // --- GHOST POSITIONS DISPLAY ---
        System.out.println("--- Ghost Positions ---");
        int nbrGhosts = currentState.getNbrOfGhost();
        for (int i = 0; i < nbrGhosts; i++) {
            System.out.print("Ghost " + i + ": ");
            TreeSet<Position> positions = currentState.getGhostPositions(i);
            if (positions.isEmpty()) {
                System.out.print("Unknown/Dead");
            } else {
                int count = 0;
                for (Position p : positions) {
                    System.out.print("(" + p.getRow() + "," + p.getColumn() + ") ");
                    count++;
                    if (count >= 10) { 
                        System.out.print("... [" + positions.size() + " total]");
                        break; 
                    }
                }
            }
            if (currentState.getCompteurPeur(i) > 0) {
                System.out.print(" [AFRAID: " + currentState.getCompteurPeur(i) + "]");
            }
            System.out.println();
        }
        System.out.println("-----------------------");

        Plans plans = currentState.extendsBeliefState();

        for (int i = 0; i < plans.size(); i++) {
            Result result = plans.getResult(i);
            ArrayList<String> actions = plans.getAction(i);
            if (actions.isEmpty()) continue;
            String direction = actions.get(0); 
            
            boolean startsInvisible = (result.size() > 1);
            double minScore = Double.POSITIVE_INFINITY;
            
            for (BeliefState nextState : result.getBeliefStates()) {
                double val;
                if (nextState.getLife() <= 0) {
                    val = startsInvisible ? -500000.0 : -1000000000.0;
                } else {
                    val = deepSearch(nextState, currentState, MAX_DEPTH - 1, startsInvisible);
                }
                if (val < minScore) minScore = val;
            }

            System.out.println(String.format("Action: %-6s | Score: %14.0f | Scénarios: %d | Type: %s", 
                direction, minScore, result.size(), startsInvisible ? "Invisible" : "Visible"));
        }
        System.out.println("============================================\n");
    }
}