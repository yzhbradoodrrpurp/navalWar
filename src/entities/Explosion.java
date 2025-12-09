package entities;

import entities.Entity.Rect;
import engine.GameEngine;
import java.util.List;

public class Explosion extends Entity {
    private int radius;
    private int lifetime = 500; // ms total
    private int age = 0;

    // 构造时立即对周围潜艇造成伤害，并通过 engine.registerKill 标记得分
    public Explosion(double x, double y, int radius, List<Submarine> subs, GameEngine engine){
        this.x = x; this.y = y; this.radius = radius;
        this.w = radius*2; this.h = radius*2;
        this.active = true;

        // 范围伤害判定（中心到潜艇中心）
        for (Submarine s : subs){
            if (!s.isActive()) continue;
            double sx = s.getBox().x + s.getBox().w/2.0;
            double sy = s.getBox().y + s.getBox().h/2.0;
            double dx = sx - this.x;
            double dy = sy - this.y;
            double dist = Math.sqrt(dx*dx + dy*dy);
            double threshold = radius + Math.max(s.getBox().w, s.getBox().h)/2.0;
            if (dist <= threshold){
                s.damage(1);
                if (!s.isActive()){
                    engine.registerKill(s, 10);
                }
            }
        }
        // engine 可存储该 Explosion 以便渲染/生命周期管理
        engine.addExplosion(this);
    }

    @Override
    public void update(long dt){
        age += dt;
        if (age >= lifetime) active = false;
    }

    public boolean isDone(){ return !active; }
}

