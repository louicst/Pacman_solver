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
        
        // 1. UPDATE HISTORY
        Position currentPos = currentState.getPacmanPosition();
        String currentKey = currentPos.getRow() + "," + currentPos.getColumn();
        visited.put(currentKey, visited.getOrDefault(currentKey, 0) + 1);

        // 2. DEBUG FEEDBACK
        feedback(currentState);

        // --- REFLEX LAYER (INSTINCT DE TUEUR) ---
        // If a ghost is scared, visible, and 1 step away, KILL IT.
        String killMove = checkImmediateKill(currentState);
        if (killMove != null) {
            System.out.println(">>> KILL REFLEX ACTIVATED: " + killMove);
            return killMove; 
        }
        // ---------------------------------------------

        // 3. NORMAL SEARCH (AND-OR)
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
        
        // waitForUserPopup();
        return bestAction;
    }

    private static void waitForUserPopup() {
        try {
            JOptionPane.showMessageDialog(null, "Score calculated. Press OK for next move.", "AI Debugger", JOptionPane.PLAIN_MESSAGE);
        } catch (Exception e) {}
    }

    // --- REFLEX KILL CHECKER ---
    private static String checkImmediateKill(BeliefState state) {
        Position pac = state.getPacmanPosition();
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) {
                TreeSet<Position> positions = state.getGhostPositions(i);
                if (positions.size() == 1) {
                    Position ghostPos = positions.first();
                    int dRow = ghostPos.getRow() - pac.getRow();
                    int dCol = ghostPos.getColumn() - pac.getColumn();
                    if (Math.abs(dRow) + Math.abs(dCol) == 1) {
                        if (dRow == -1) return PacManLauncher.UP;
                        if (dRow == 1)  return PacManLauncher.DOWN;
                        if (dCol == -1) return PacManLauncher.LEFT;
                        if (dCol == 1)  return PacManLauncher.RIGHT;
                    }
                }
            }
        }
        return null; 
    }

    // --- AND NODE ---
    private static double evaluateANDNode(Result result, BeliefState parent, int depth, boolean isInvisibleContext) {
        double minScore = Double.POSITIVE_INFINITY;
        boolean hasValidScenario = false;
        boolean currentContextIsInvisible = isInvisibleContext || (result.size() > 1);

        // Note: The "100% Death Check" has been removed as requested.
        // We proceed directly to checking each scenario.

        for (BeliefState nextState : result.getBeliefStates()) {
            hasValidScenario = true;
            double val;

            if (nextState.getLife() < parent.getLife() || nextState.getLife() <= 0) {
                // If the context is invisible, we treat death as a risk (-500k).
                // If visible, it's a certainty (-1B).
                if (currentContextIsInvisible) val = -500000.0; 
                else val = -1000000000.0; 
            } else {
                val = deepSearch(nextState, parent, depth, currentContextIsInvisible);
            }

            if (val < minScore) minScore = val;
        }
        return hasValidScenario ? minScore : -1000000000.0;
    }

    // --- OR NODE ---
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

        // 1. SCORE DIFFERENCE (Kill Confirmed > Super Gum > Gum)
        if (state.getScore() > parent.getScore()) {
            double diff = state.getScore() - parent.getScore();
            if (diff > 100) {
                score += diff * 10000.0; // Ghost Eaten
            } else {
                // Regular Coin
                score += diff * 20.0;
                
                // --- GREEDING LOGIC ---
                // If ALL dangerous ghosts are close (Dist <= 2), we know the map is safe.
                // We boost the value of eating regular coins here.
                if (areAllGhostsClose(state)) {
                    score += diff * 500.0; 
                }
            }
        }

        // 2. COIN DENSITY
        score += getCoinDensityScore(state, pac);
        
        // 3. SMART GHOST HUNTING
        if (!isUncertain) {
            score += getGhostHuntingScore(state, pac);
        }

        // 4. EXPLORATION
        if (isUncertain && visited.getOrDefault(key, 0) == 0) {
            score += 200.0; 
        }

        return score;
    }

    // --- DANGER ANALYSIS (STRATEGIC POSITIONING) ---
    private static double getDangerScore(BeliefState state) {
        double danger = 0;
        Position pac = state.getPacmanPosition();
        String key = pac.getRow() + "," + pac.getColumn();

        // 1. STRATEGIC POSITIONING
        // Check Information State: Do we know where dangerous ghosts are?
        boolean isTacticalSituationSafe = checkTacticalSafety(state);

        // If Unsafe (Invisible Ghosts), analyze Map Topology
        if (!isTacticalSituationSafe) {
            int exits = countExits(state, pac);
            
            if (exits >= 3) {
                // JUNCTION: Safe Haven (Bonus)
                danger -= 500.0; 
            } else if (exits == 2) {
                // CORRIDOR: Death Trap (Penalty)
                danger += 2000.0; 
            } else if (exits == 1) {
                // DEAD END (Penalty)
                danger += 5000.0;
            }
        }

        // 2. VISITED PENALTY
        int nbrVisits = visited.getOrDefault(key, 0);
        if (nbrVisits > 0) {
            danger += Math.pow(nbrVisits, 2) * 50.0; 
        }

        // 3. GHOST PROXIMITY
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
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

    // --- HELPERS ---

    // Returns TRUE only if ALL dangerous ghosts are known (visible)
    private static boolean checkTacticalSafety(BeliefState state) {
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            if (state.getCompteurPeur(i) > 0) continue; 
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.size() > 1) return false; 
        }
        return true; 
    }

    // Returns TRUE only if ALL dangerous ghosts are within Distance 2
    private static boolean areAllGhostsClose(BeliefState state) {
        Position pac = state.getPacmanPosition();
        int ghostCount = state.getNbrOfGhost();
        for(int i=0; i<ghostCount; i++) {
            if (state.getCompteurPeur(i) > 0) continue; 
            TreeSet<Position> positions = state.getGhostPositions(i);
            if (positions.isEmpty()) return false; 
            int minDist = Integer.MAX_VALUE;
            for(Position p : positions) {
                int d = Math.abs(p.getRow() - pac.getRow()) + Math.abs(p.getColumn() - pac.getColumn());
                if(d < minDist) minDist = d;
            }
            if (minDist > 2) return false;
        }
        return true; 
    }

    private static int countExits(BeliefState state, Position p) {
        int exits = 0;
        char[][] map = state.getMap();
        int r = p.getRow(); int c = p.getColumn();
        if (isValid(r+1, c, map)) exits++;
        if (isValid(r-1, c, map)) exits++;
        if (isValid(r, c+1, map)) exits++;
        if (isValid(r, c-1, map)) exits++;
        return exits;
    }

    private static boolean isValid(int r, int c, char[][] map) {
        return r >= 0 && r < map.length && c >= 0 && c < map[0].length && map[r][c] != '#';
    }

    private static double getCoinDensityScore(BeliefState state, Position pac) {
        char[][] map = state.getMap();
        int radius = 3; 
        int coinCount = 0;
        double minDist = Double.MAX_VALUE;
        for (int r = pac.getRow() - radius; r <= pac.getRow() + radius; r++) {
            for (int c = pac.getColumn() - radius; c <= pac.getColumn() + radius; c++) {
                if (r >= 0 && r < map.length && c >= 0 && c < map[0].length) {
                    char cell = map[r][c];
                    if (cell == '.' || cell == '*') {
                        coinCount++;
                        double d = Math.abs(r - pac.getRow()) + Math.abs(c - pac.getColumn());
                        if (d < minDist) minDist = d;
                    }
                }
            }
        }
        double score = 0;
        if (coinCount > 0) score += coinCount * 100.0; 
        if (minDist != Double.MAX_VALUE) score += (2000.0 / (minDist + 1));
        return score;
    }

    private static double getGhostHuntingScore(BeliefState state, Position pac) {
        double score = 0;
        for (int i = 0; i < state.getNbrOfGhost(); i++) {
            int fearTimer = state.getCompteurPeur(i);
            if (fearTimer > 0) {
                TreeSet<Position> positions = state.getGhostPositions(i);
                if (positions.size() == 1) {
                    Position ghostPos = positions.first(); 
                    int dist = Math.abs(ghostPos.getRow() - pac.getRow()) + Math.abs(ghostPos.getColumn() - pac.getColumn());
                    int requiredTime = dist + 2;
                    if (fearTimer >= requiredTime) {
                        score += 200000.0; 
                        score += (10000.0 / (dist + 1));
                    }
                }
            }
        }
        return score;
    }

    public static void feedback(BeliefState currentState) {
        System.out.println("\n=== ANALYSE AND-OR (Final Strategy) ===");
        Position cur = currentState.getPacmanPosition();
        System.out.println("Pos Pacman: " + cur.getRow() + "," + cur.getColumn());

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
                    if (startsInvisible) val = -500000.0; 
                    else val = -1000000000.0; 
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