package entities;

import entities.Entity.Rect;
import entities.Explosion;
import engine.GameEngine;

public class Bomb extends Entity {
    private double vy = 2.2;
    private boolean exploded = false;
    private static final double GRAVITY = 0.06;

    public Bomb(double x, double y){
        this.x = x; this.y = y;
        this.w = 24; this.h = 36;
        this.active = true;
    }

    @Override
    public void update(long dt){
        if (!active) return;
        // update position (dt in ms, use simple model)
        double steps = Math.max(1, dt/16.0);
        this.y += vy * steps;
        vy += GRAVITY * steps;
    }

    public void explode(int radius){
        if (exploded) return;
        exploded = true;
        active = false;
        // 爆炸由外部 engine 负责创建 Explosion 并对潜艇造成效果
        // 为了保持脱耦，这里不直接访问 engine；由使用方创建 Explosion
    }

    @Override
    public Rect getBox(){ return new Rect((int)x, (int)y, (int)w, (int)h); }
}

