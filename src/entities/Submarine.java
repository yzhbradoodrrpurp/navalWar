package entities;

import entities.Entity.Rect;

public class Submarine extends Entity {
    public enum Type { RED, BLACK }

    private double vx;
    private double bobPhase;
    private int hp;
    private final Type type;
    private final int scoreValue;

    // side: -1 表示从左向右，+1 表示从右向左
    public Submarine(int sideSign, int worldW, int worldH, int seaTop){
        this.w = 80; this.h = 40;
        this.hp = Math.random() < 0.15 ? 2 : 1;
        // 随机类型分配：约 40% 概率为 RED（快速高分），其余为 BLACK（慢低分）
        if (Math.random() < 0.4) {
            this.type = Type.RED;
            this.scoreValue = 20;
        } else {
            this.type = Type.BLACK;
            this.scoreValue = 10;
        }
        // 基础速度
        double base = 1.2 + Math.random() * 1.6; // 1.2 ~ 2.8
        // 根据类型调节速度：RED 更快，BLACK 更慢；保留方向 sign
        double mult = (this.type == Type.RED) ? 1.4 : 0.75;
        this.vx = base * mult * (sideSign < 0 ? 1 : -1);

        this.x = sideSign < 0 ? -this.w : worldW + this.w;
        this.y = seaTop + 20 + Math.random()*(worldH - seaTop - 80);
        this.bobPhase = Math.random()*Math.PI*2;
        this.active = true;
    }

    @Override
    public void update(long dt){
        // 使用 dt 粒度会更稳定，但这里保持原有简单推进
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

    // 新增访问器
    public Type getType(){ return type; }
    public int getScoreValue(){ return scoreValue; }
}
