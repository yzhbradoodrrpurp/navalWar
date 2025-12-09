package entities;

import entities.Entity.Rect;

public class Submarine extends Entity {
    private double vx;
    private double bobPhase;
    private int hp;

    // 新增：潜艇类型，红色（快速、20分）或黑色（慢速、10分）
    public enum Type { RED, BLACK }
    private final Type type;

    // side: -1 表示从左向右，+1 表示从右向左
    public Submarine(int sideSign, int worldW, int worldH, int seaTop){
        this.w = 80; this.h = 40;
        this.hp = Math.random() < 0.15 ? 2 : 1;

        // 随机决定类型并设置速度：RED 更快、BLACK 更慢
        if (Math.random() < 0.5) {
            this.type = Type.RED;
            // 红色更快
            double speed = 2.0 + Math.random() * 1.6; // ~2.0 - 3.6
            this.vx = speed * (sideSign < 0 ? 1 : -1);
        } else {
            this.type = Type.BLACK;
            // 黑色更慢
            double speed = 0.6 + Math.random() * 0.8; // ~0.6 - 1.4
            this.vx = speed * (sideSign < 0 ? 1 : -1);
        }

        this.x = sideSign < 0 ? -this.w : worldW + this.w;
        this.y = seaTop + 20 + Math.random()*(worldH - seaTop - 80);
        this.bobPhase = Math.random()*Math.PI*2;
        this.active = true;
    }

    @Override
    public void update(long dt){
        this.x += vx;
        // bobbing
        bobPhase += 0.03;
        this.y += Math.sin(bobPhase) * 0.5;
        // offscreen kill
        if (this.x < -200 || this.x > 2000) this.active = false;
    }

    public void damage(int n){ hp -= n; if (hp <= 0) this.active = false; }

    @Override
    public Rect getBox(){ return new Rect((int)x, (int)y, (int)w, (int)h); }

    // 新增：返回该潜艇被击落后应获得的分数
    public int getScoreValue(){
        return (this.type == Type.RED) ? 20 : 10;
    }

    // 可选：外部可用以区分渲染或调试
    public Type getType(){ return this.type; }
}
