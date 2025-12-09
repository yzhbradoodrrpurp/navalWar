package entities;

import entities.Entity.Rect;

public class Submarine extends Entity {
    private double vx;
    private double bobPhase;
    private int hp;

    // side: -1 表示从左向右，+1 表示从右向左
    public Submarine(int sideSign, int worldW, int worldH, int seaTop){
        this.w = 80; this.h = 40;
        this.hp = Math.random() < 0.15 ? 2 : 1;
        this.vx = (1.2 + Math.random()*1.6) * (sideSign < 0 ? 1 : -1);
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
}

