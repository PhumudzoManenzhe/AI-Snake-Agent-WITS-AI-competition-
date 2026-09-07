package myAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;

import za.ac.wits.snake.DevelopmentAgent;

public class MyAgent extends DevelopmentAgent {
    int[][] map;
    int mapW, mapH;
    String[][] snakeState;
    int[][] snakeHeads;
    int mySnakeNum;
    int[] apple;
    int[] directions;
    int appleAge = 0;
    double appleValue = 5.0;
    int[] prevApple = {-1, -1};

    int[] bodyLengths;
    int[] curLengths;
    boolean aggressiveMode = false;

    int myMaxSize = 0;
    int oppMaxSize = 0;

    int[][] componentMap;
    Map<Integer, Integer> componentSizes;

    public static void main(String args[]) {
        MyAgent agent = new MyAgent();
        MyAgent.start(agent, args);
    }

    @Override
    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String initString = br.readLine();
            System.err.println(initString);
            String[] temp = initString.split(" ");
            int nSnakes = Integer.parseInt(temp[0]);
            mapW = Integer.parseInt(temp[1]);
            mapH = Integer.parseInt(temp[2]);
            map = new int[mapH][mapW];

            componentMap = new int[mapH][mapW];
            componentSizes = new HashMap<>();

            int length = 0;
            
            long startTime = System.currentTimeMillis();

            while (true) {
                String line = br.readLine();
                System.err.println(line);

                if (line.contains("Game Over")) {
                    break;
                }

                String[] appleCoords = line.split(" ");
                apple = new int[]{Integer.parseInt(appleCoords[0]), Integer.parseInt(appleCoords[1])};

                if (apple[0] != prevApple[0] || apple[1] != prevApple[1]) {
                    appleAge = 0;
                    appleValue = 5.0;
                    prevApple = apple.clone();
                } else {
                    appleAge++;
                    appleValue = Math.max(0, appleValue - 0.1);
                    int effectiveAppleValue = (int) Math.ceil(appleValue);
                    if (effectiveAppleValue == 0) appleValue = 0;
                }

                mySnakeNum = Integer.parseInt(br.readLine());
                System.err.println(mySnakeNum);

                snakeState = new String[4][];
                snakeHeads = new int[nSnakes][2];
                directions = new int[nSnakes];
                curLengths = new int[4];
                bodyLengths = new int[4];

                for (int i = 0; i < mapH; i++)
                    Arrays.fill(map[i], 0);

                if (apple[0] >= 0 && apple[1] >= 0) map[apple[1]][apple[0]] = -1;

                for (int i = 0; i < nSnakes; i++) {
                    snakeState[i] = br.readLine().split(" ");

                    length = Integer.parseInt(snakeState[i][1]);
                    curLengths[i] = length;

                    if (i == mySnakeNum && (length > myMaxSize)) {
                        myMaxSize = length;
                    } else if (i != mySnakeNum && (length > oppMaxSize)) {
                        oppMaxSize = length;
                    }

                    if (snakeState[i].length > 3 && !snakeState[i][0].equals("dead")) {
                        String[] body = Arrays.copyOfRange(snakeState[i], 3, snakeState[i].length);
                        bodyLengths[i] = body.length;

                        for (int j = 0; j < body.length; j++) {
                            String[] coord = body[j].split(",");
                            int x = Integer.parseInt(coord[0]);
                            int y = Integer.parseInt(coord[1]);
                            if (j == 0) {
                                snakeHeads[i] = new int[]{x, y};
                                map[y][x] = 2;
                            } else {
                                map[y][x] = 1;
                            }
                            if (j < body.length - 1) {
                                String[] next = body[j + 1].split(",");
                                int nx = Integer.parseInt(next[0]);
                                int ny = Integer.parseInt(next[1]);
                                if (x == nx) {
                                    for (int k = Math.min(y, ny); k <= Math.max(y, ny); k++) map[k][x] = 1;
                                } else if (y == ny) {
                                    for (int k = Math.min(x, nx); k <= Math.max(x, nx); k++) map[y][k] = 1;
                                }
                            }
                        }
                        if (body.length > 1) {
                            String[] head = body[0].split(",");
                            String[] first = body[1].split(",");
                            int hx = Integer.parseInt(head[0]);
                            int hy = Integer.parseInt(head[1]);
                            int fx = Integer.parseInt(first[0]);
                            int fy = Integer.parseInt(first[1]);
                            if (hy < fy) directions[i] = 0;
                            else if (hy > fy) directions[i] = 1;
                            else if (hx < fx) directions[i] = 2;
                            else directions[i] = 3;
                        }
                    } else {
                        bodyLengths[i] = 0;
                    }
                }
                
                precomputeComponentSizes();

                int myLen = curLengths[mySnakeNum];
                
                long elapsedTime = System.currentTimeMillis() - startTime;
                boolean eightMinutesPassed = elapsedTime > 480000;

                int myRank = 1;
                for (int i = 0; i < nSnakes; i++) {
                    if (i != mySnakeNum && snakeState[i][0].equals("alive") && curLengths[i] > myLen) {
                        myRank++;
                    }
                }
                boolean isRank3 = (myRank == 3);

                int rank4SnakeIndex = -1;
                int shortestLen = Integer.MAX_VALUE;
                for (int i = 0; i < nSnakes; i++) {
                    if (snakeState[i][0].equals("alive")) {
                        if (curLengths[i] < shortestLen) {
                            shortestLen = curLengths[i];
                            rank4SnakeIndex = i;
                        }
                    }
                }

                Arrays.sort(curLengths);
                int maxLen = curLengths[3];
                int move = 0;
                
                if (eightMinutesPassed && isRank3 && rank4SnakeIndex != -1) {
                    move = huntSnake(rank4SnakeIndex); 
                } else {
                    if (aggressiveMode) {
                        if (oppMaxSize > myMaxSize) {
                            aggressiveMode = false;
                            move = getNormalMove(nSnakes, myLen);
                        } else {
                            move = getAggroMove(nSnakes);
                        }
                    } else {
                        if (myMaxSize >= oppMaxSize + 20 || maxLen >= 70) {
                            aggressiveMode = true;
                            move = getAggroMove(nSnakes);
                        } else {
                            move = getNormalMove(nSnakes, myLen);
                        }
                    }
                }

                System.out.println(move);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int huntSnake(int targetIdx) {
        int[] pos = snakeHeads[mySnakeNum];

        ArrayList<int[]> pathToTarget = new ArrayList<>();
        if (targetIdx != -1 && snakeHeads[targetIdx] != null && snakeState[targetIdx][0].equals("alive")) {
            int[] targetHead = snakeHeads[targetIdx];
            int saved = map[targetHead[1]][targetHead[0]];
            map[targetHead[1]][targetHead[0]] = 0;
            pathToTarget = huntPath(pos, targetHead, map);
            map[targetHead[1]][targetHead[0]] = saved;
        }

        if (!pathToTarget.isEmpty()) {
            int[] next = pathToTarget.get(pathToTarget.size() - 1);
            return directionTo(next, pos);
        }

        return safeMove(pos); 
    }

    private int getAggroMove(int nSnakes) {
        int[] pos = snakeHeads[mySnakeNum];
        int targetIdx = -1;
        int maxLen = 0;
        for (int i = 0; i < nSnakes; i++) {
            if (i == mySnakeNum) continue;
            if (bodyLengths[i] > maxLen && snakeState[i][0].equals("alive")) {
                maxLen = bodyLengths[i];
                targetIdx = i;
            }
        }
        ArrayList<int[]> pathToTarget = new ArrayList<>();
        if (targetIdx != -1 && snakeHeads[targetIdx] != null) {
            int[] targetHead = snakeHeads[targetIdx];
            int saved = map[targetHead[1]][targetHead[0]];
            map[targetHead[1]][targetHead[0]] = 0;
            pathToTarget = huntPath(pos, targetHead, map);
            map[targetHead[1]][targetHead[0]] = saved;
        }
        if (!pathToTarget.isEmpty()) {
            int[] next = pathToTarget.get(pathToTarget.size() - 1);
            return directionTo(next, pos);
        }
        int x = pos[0], y = pos[1];
        int[][] moves = {{x, y - 1}, {x, y + 1}, {x - 1, y}, {x + 1, y}};
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH && map[ny][nx] == 0) return dir;
        }
        return 0;
    }

    private ArrayList<int[]> huntPath(int[] root, int[] dest, int[][] map) {
        ArrayList<int[]> empty = new ArrayList<>();
        if (root[0] < 0 || root[1] < 0 || root[0] >= mapW || root[1] >= mapH) return empty;

        boolean[][] closed = new boolean[mapH][mapW];
        int[][][] parent = new int[mapH][mapW][2];
        for (int i = 0; i < mapW; i++)
            for (int j = 0; j < mapH; j++) parent[j][i] = new int[]{-1, -1};

        int[][] gScore = new int[mapH][mapW];
        for (int i = 0; i < mapH; i++) Arrays.fill(gScore[i], Integer.MAX_VALUE);
        gScore[root[1]][root[0]] = 0;

        class Node implements Comparable<Node> {
            int x, y, f;
            Node(int x, int y, int f) { this.x = x; this.y = y; this.f = f; }
            public int compareTo(Node o) { return Integer.compare(this.f, o.f); }
        }

        PriorityQueue<Node> open = new PriorityQueue<>();
        int h0 = Math.abs(root[0] - dest[0]) + Math.abs(root[1] - dest[1]);
        open.add(new Node(root[0], root[1], h0));

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        boolean found = false;
        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (closed[cur.y][cur.x]) continue;
            closed[cur.y][cur.x] = true;
            if (cur.x == dest[0] && cur.y == dest[1]) { found = true; break; }

            for (int[] d : dirs) {
                int nx = cur.x + d[0];
                int ny = cur.y + d[1];
                if (nx < 0 || nx >= mapW || ny < 0 || ny >= mapH) continue;
                if (closed[ny][nx]) continue;
                if (map[ny][nx] != 0) {
                    if (!(nx == dest[0] && ny == dest[1])) continue;
                }
                int tentativeG = gScore[cur.y][cur.x] + 1;
                if (tentativeG < gScore[ny][nx]) {
                    gScore[ny][nx] = tentativeG;
                    parent[ny][nx] = new int[]{cur.x, cur.y};
                    int f = tentativeG + Math.abs(nx - dest[0]) + Math.abs(ny - dest[1]);
                    open.add(new Node(nx, ny, f));
                }
            }
        }
        ArrayList<int[]> path = new ArrayList<>();
        if (!found) return path;
        int cx = dest[0], cy = dest[1];
        while (!(cx == root[0] && cy == root[1])) {
            path.add(new int[]{cx, cy});
            int[] p = parent[cy][cx];
            cx = p[0]; cy = p[1];
            if (cx == -1 && cy == -1) { path.clear(); break; }
        }
        return path;
    }

    private int getNormalMove(int nSnakes, int myLen) {
        int[] pos = snakeHeads[mySnakeNum];
        int snakeLength = snakeState[mySnakeNum].length - 3;
        
        int reachableSpace = getAreaSize(pos, map);
        boolean dominant = reachableSpace > (mapW * mapH / 2) || snakeLength > 10;
        boolean threatDetected = false;
        for (int i = 0; i < nSnakes; i++) {
            if (i == mySnakeNum) continue;
            if (snakeState[i][0].equals("alive")) {
                int oppLength = snakeState[i].length - 3;
                int[] oppPos = snakeHeads[i];
                int dist = Math.abs(pos[0] - oppPos[0]) + Math.abs(pos[1] - oppPos[1]);
                if (oppLength >= snakeLength - 2 && dist < 6) {
                    threatDetected = true;
                    break;
                }
            }
        }
        boolean aggressiveMode = dominant && threatDetected && snakeLength > 5;
        if (aggressiveMode) {
            int bestDir = -1;
            int minDist = Integer.MAX_VALUE;
            for (int i = 0; i < nSnakes; i++) {
                if (i == mySnakeNum || !snakeState[i][0].equals("alive")) continue;
                int[] target = snakeHeads[i];
                ArrayList<int[]> path = findPath(pos, target, map);
                if (!path.isEmpty() && path.size() < minDist) {
                    int[] nextStep = path.get(path.size() - 1);
                    if (!isHeadCollision(nextStep, snakeHeads)) {
                        bestDir = directionTo(nextStep, pos);
                        minDist = path.size();
                    }
                }
            }
            if (bestDir != -1) return bestDir;
        }
        ArrayList<int[]> pathToApple = findPath(pos, apple, map);
        boolean safeApple = true;
        if (appleValue > 0 && !pathToApple.isEmpty()) {
            int myDist = pathToApple.size();
            for (int i = 0; i < nSnakes; i++) {
                if (i != mySnakeNum && snakeState[i][0].equals("alive")) {
                    ArrayList<int[]> oppPath = findPath(snakeHeads[i], apple, map);
                    int oppDist = oppPath.size();
                    if (oppDist > 0 && oppDist + 1 < myDist) {
                        safeApple = false;
                        break;
                    }
                }
            }
            if (safeApple) {
                int[] next = pathToApple.get(pathToApple.size() - 1);
                if (!isHeadCollision(next, snakeHeads) && getAreaSize(next, map) > snakeLength + 2)
                    return directionTo(next, pos);
            }
        }
        if (appleValue <= 0 || myLen >= 70) {
            roamMove(pos);
        } else if (!safeApple) {
            Random rand = new Random();
            int[] roamTarget = null;
            int maxTries = 20;
            for (int i = 0; i < maxTries; i++) {
                int tx = 10 + rand.nextInt(Math.max(1, Math.min(40, mapW - 11) - 10));
                int ty = 4 + rand.nextInt(Math.max(1, Math.min(36, mapH - 5) - 4));
                if (tx >= mapW || ty >= mapH) continue;
                if (map[ty][tx] != 0 && map[ty][tx] != -1) continue;
                
                int space = getAreaSize(new int[]{tx, ty}, map);
                if (space > snakeLength * 1.5) {
                    roamTarget = new int[]{tx, ty};
                    break;
                }
            }
            if (roamTarget != null) {
                ArrayList<int[]> roamPath = findPath(pos, roamTarget, map);
                if (!roamPath.isEmpty()) {
                    int[] next = roamPath.get(roamPath.size() - 1);
                    if (!isHeadCollision(next, snakeHeads))
                        return directionTo(next, pos);
                }
            }
        }
        return safeMove(pos);
    }

    private int safeMove(int[] pos) {
        int x = pos[0], y = pos[1];
        int snakeLength = snakeState[mySnakeNum].length - 3;
        int[][] moves = {{x, y - 1}, {x, y + 1}, {x - 1, y}, {x + 1, y}};
        int bestDir = -1;
        int maxSpace = -1;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            int[] next = {nx, ny};
            if (nx >= 3 && nx < 47 && ny >= 3 && ny < 47 && map[ny][nx] == 0
                    && !isHeadCollision(next, snakeHeads)) {
                int freeSpace = getAreaSize(new int[]{nx, ny}, map);
                if (freeSpace > snakeLength + 2 && freeSpace > maxSpace) {
                    maxSpace = freeSpace;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != -1) return bestDir;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH && map[ny][nx] == 0)
                return dir;
        }
        return new Random().nextInt(4);
    }

    private int roamMove(int[] pos) {
        int x = pos[0], y = pos[1];
        int snakeLength = snakeState[mySnakeNum].length - 3;
        int[][] moves = {{x, y - 1}, {x, y + 1}, {x - 1, y}, {x + 1, y}};
        int bestDir = -1;
        int maxSpace = -1;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 3 && nx < 47 && ny >= 3 && ny < 47 && map[ny][nx] == 0) {
                int freeSpace = getAreaSize(new int[]{nx, ny}, map);
                if (freeSpace > snakeLength + 2 && freeSpace > maxSpace) {
                    maxSpace = freeSpace;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != -1) return bestDir;
        for (int dir = 0; dir < 4; dir++) {
            int nx = moves[dir][0];
            int ny = moves[dir][1];
            if (nx >= 0 && nx < mapW && ny >= 0 && ny < mapH && map[ny][nx] == 0)
                return dir;
        }
        return 0;
    }

    private int getAreaSize(int[] start, int[][] map) {
        if (start[0] < 0 || start[0] >= mapW || start[1] < 0 || start[1] >= mapH || map[start[1]][start[0]] > 0) {
             return 0;
        }
        int componentID = componentMap[start[1]][start[0]];
        return componentSizes.getOrDefault(componentID, 0);
    }
    
    private void precomputeComponentSizes() {
        for (int i = 0; i < mapH; i++) {
            Arrays.fill(componentMap[i], 0);
        }
        componentSizes.clear();
        int componentID = 1;
        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                if ((map[y][x] == 0 || map[y][x] == -1) && componentMap[y][x] == 0) {
                    int size = bfsFill(x, y, componentID);
                    componentSizes.put(componentID, size);
                    componentID++;
                }
            }
        }
    }

    private int bfsFill(int startX, int startY, int componentID) {
        int count = 0;
        LinkedList<Long> queue = new LinkedList<>();
        
        long startCoord = ((long) startX << 32) | (startY & 0xFFFFFFFFL);
        queue.add(startCoord);
        componentMap[startY][startX] = componentID;

        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        while (!queue.isEmpty()) {
            long coord = queue.poll();
            int x = (int) (coord >> 32);
            int y = (int) coord;
            count++;
            
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx < 0 || ny < 0 || nx >= mapW || ny >= mapH) continue;
                if ((map[ny][nx] == 0 || map[ny][nx] == -1) && componentMap[ny][nx] == 0) {
                    componentMap[ny][nx] = componentID;
                    queue.add(((long) nx << 32) | (ny & 0xFFFFFFFFL));
                }
            }
        }
        return count;
    }


    private int directionTo(int[] next, int[] pos) {
        if (next[1] - pos[1] == 1) return 1;
        if (next[1] - pos[1] == -1) return 0;
        if (next[0] - pos[0] == 1) return 3;
        if (next[0] - pos[0] == -1) return 2;
        return new Random().nextInt(4);
    }

    private ArrayList<int[]> findPath(int[] start, int[] goal, int[][] map) {
        if (start[0] < 0 || start[1] < 0 || start[0] >= mapW || start[1] >= mapH)
            return new ArrayList<>();

        class Node {
            int x, y;
            int g, f;
            Node parent;

            Node(int x, int y, int g, int f, Node parent) {
                this.x = x; this.y = y;
                this.g = g; this.f = f;
                this.parent = parent;
            }
        }

        boolean[][] visited = new boolean[mapH][mapW];
        Node[][] nodeMap = new Node[mapH][mapW];
        PriorityQueue<Node> open = new PriorityQueue<>((a, b) -> {
            int cmp = Integer.compare(a.f, b.f);
            if (cmp == 0) cmp = Integer.compare(a.g, b.g);
            return cmp;
        });

        int sx = start[0], sy = start[1];
        int gx = goal[0], gy = goal[1];

        Node startNode = new Node(sx, sy, 0, Math.abs(sx - gx) + Math.abs(sy - gy), null);
        open.add(startNode);
        nodeMap[sy][sx] = startNode;

        while (!open.isEmpty()) {
            Node current = open.poll();
            int x = current.x;
            int y = current.y;

            if (x == gx && y == gy) {
                ArrayList<int[]> path = new ArrayList<>();
                while (current.parent != null) {
                    path.add(new int[]{current.x, current.y});
                    current = current.parent;
                }
                return path;
            }
            if (visited[y][x]) continue;
            visited[y][x] = true;

            int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
            for (int[] d : dirs) {
                int nx = x + d[0];
                int ny = y + d[1];
                if (nx < 0 || ny < 0 || nx >= mapW || ny >= mapH) continue;
                if (map[ny][nx] != 0 && map[ny][nx] != -1) continue;

                int g = current.g + 1;
                int heur = Math.abs(nx - gx) + Math.abs(ny - gy);
                int openBias = (map[y][x] == 0) ? 0 : 2;
                int f = g + heur + openBias;

                if (nodeMap[ny][nx] == null || g < nodeMap[ny][nx].g) {
                    Node neighbor = new Node(nx, ny, g, f, current);
                    nodeMap[ny][nx] = neighbor;
                    open.add(neighbor);
                }
            }
        }
        return new ArrayList<>();
    }

    private boolean isHeadCollision(int[] nextPos, int[][] snakeHeads) {
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int i = 0; i < snakeHeads.length; i++) {
            if (i == mySnakeNum) continue;
            if (snakeState[i] == null || snakeState[i][0].equals("dead")) continue;
            int[] h = snakeHeads[i];
            if (h == null) continue;
            for (int[] d : dirs) {
                int nx = h[0] + d[0];
                int ny = h[1] + d[1];
                if (nx == nextPos[0] && ny == nextPos[1]) return true;
            }
            int dir = directions[i];
            int px = h[0];
            int py = h[1];
            if (dir == 0) py -= 1;
            else if (dir == 1) py += 1;
            else if (dir == 2) px -= 1;
            else if (dir == 3) px += 1;
            if (px == nextPos[0] && py == nextPos[1]) return true;
        }
        return false;
    }
}
