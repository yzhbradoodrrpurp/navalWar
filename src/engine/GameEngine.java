package engine;

import entities.*;
import entities.Entity.Rect;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameEngine {
    private final int width, height;
    private final int seaTop;
    private final Ship ship;
    private final List<Bomb> bombs = new CopyOnWriteArrayList<>();
    private final List<Submarine> subs = new CopyOnWriteArrayList<>();
    private final List<Explosion> exps = new CopyOnWriteArrayList<>();
    private final Random rand = new Random();
    private long subSpawnTimer = 0;
    private long nextSubDelay = 1000 + rand.nextInt(1500);
    private int score = 0;
    private boolean running = false;

    public GameEngine(int width, int height){
        this.width = width; this.height = height;
        this.seaTop = (int)Math.floor(height * 0.38);
        this.ship = new Ship((width - 100)/2, seaTop - 30, width);
    }

    public void start(){ running = true; }

    public void stop(){ running = false; }

    // 单步推进，dt 毫秒
    public void tick(long dt){
        if (!running) return;
        // update ship (no dt-velocity based movement needed here)
        ship.update(dt);

        // update bombs, subs, explosions
        for (Bomb b : bombs) b.update(dt);
        for (Submarine s : subs) s.update(dt);
        for (Explosion e : exps) e.update(dt);

        // ---- 新增：检查炸弹到达深度自动爆炸 ----
        double explosionDepth = seaTop + (height - seaTop) * 0.45;
        List<Bomb> bombsSnapshot = new ArrayList<>(bombs); // 避免并发修改问题
        for (Bomb b : bombsSnapshot) {
            if (!b.isActive()) continue;
            Rect box = b.getBox();
            if (box.y >= (int)explosionDepth) {
                // 触发爆炸并创建 Explosion（由 Explosion 构造时处理伤害与注册）
                b.explode(70);
                int cx = box.x + box.w/2;
                int cy = box.y + box.h/2;
                new Explosion(cx, cy, 70, subs, this);
            }
        }

        // 碰撞检测：炸弹直接命中潜艇触发爆炸（局部爆炸）
        for (Bomb b : bombs){
            if (!b.isActive()) continue;
            Rect bbox = b.getBox();
            for (Submarine s : subs){
                if (!s.isActive()) continue;
                Rect sbox = s.getBox();
                if (rectOverlap(bbox, sbox)){
                    // 触发炸弹爆炸并由 Explosion 处理范围伤害
                    b.explode(60);
                    int cx = bbox.x + bbox.w/2;
                    int cy = bbox.y + bbox.h/2;
                    new Explosion(cx, cy, 60, subs, this);
                    break;
                }
            }
        }

        // 炸弹到达深度或命中后会创建 Explosion，Explosion 构造时会对潜艇造成伤害并更新分数
        // 清理无效对象并统计死亡以计分（Explosion 已处理计分）
        bombs.removeIf(b -> !b.isActive());
        List<Submarine> before = new ArrayList<>(subs);
        subs.removeIf(s -> !s.isActive());
        exps.removeIf(e -> e.isDone());

        // 生成潜艇
        subSpawnTimer += dt;
        if (subSpawnTimer >= nextSubDelay){
            spawnSub();
            subSpawnTimer = 0;
            nextSubDelay = 800 + rand.nextInt(2000);
        }
    }

    private void spawnSub(){
        boolean fromLeft = rand.nextBoolean();
        Submarine s = new Submarine(fromLeft ? -1 : 1, width, height, seaTop);
        subs.add(s);
    }

    // 矩形碰撞
    private boolean rectOverlap(Rect a, Rect b){
        return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
    }

    // 玩家接口
    public void playerMoveLeft(){ ship.move(-1); }
    public void playerMoveRight(){ ship.move(1); }
    public void playerDropBomb(){
        Bomb b = ship.dropBomb();
        if (b != null) bombs.add(b);
    }

    // Explosion 回调用于计分与移除潜艇（由 Explosion 本身调用 engine.registerKill）
    public void registerKill(Submarine s, int gain){
        score += gain;
        s.setActive(false);
    }

    // 供 Explosion 访问 engine（弱耦合）
    public int getWidth(){ return width; }
    public int getHeight(){ return height; }
    public int getSeaTop(){ return seaTop; }
    public void addExplosion(Explosion e){ exps.add(e); }

    // 状态输出
    public String statusString(){
        return String.format("Score=%d ShipX=%.1f bombs=%d subs=%d exps=%d",
                score, ship.getX(), bombs.size(), subs.size(), exps.size());
    }

    // 新增：供 UI 渲染用的只读快照访问器
    public Ship getShip(){ return ship; }
    public List<Bomb> getBombs(){ return new ArrayList<>(bombs); }
    public List<Submarine> getSubs(){ return new ArrayList<>(subs); }
    public List<Explosion> getExplosions(){ return new ArrayList<>(exps); }
}
