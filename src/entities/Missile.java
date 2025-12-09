package entities;

import engine.GameEngine;
import java.util.List;

/**
 * 简单的追踪导弹实体（由 UI 创建并维护）。
 * 导弹会寻找最近的 active Submarine 并朝其方向移动。
 * 命中检测与爆炸由创建者（GameWindow）在检测到碰撞时负责触发 Explosion。
 */
public class Missile extends Entity {
    private double vx = 0, vy = -3;
    private double speed = 5.0; // pixels per tick unit (scaled by dt in UI)
    private int lifetime = 8000; // ms
    private int age = 0;
    private final GameEngine engine;

    public Missile(double cx, double cy, GameEngine engine){
        this.w = 24; this.h = 10;
        this.x = cx - this.w/2.0;
        this.y = cy - this.h/2.0;
        this.active = true;
        this.engine = engine;
    }

    /**
     * dt in ms
     */
    public void update(long dt){
        if (!active) return;
        age += dt;
        if (age >= lifetime) { active = false; return; }

        // 找到最近的目标
        List<Submarine> subs = engine.getSubs();
        Submarine target = null;
        double bestDist = Double.MAX_VALUE;
        for (Submarine s : subs){
            if (!s.isActive()) continue;
            double sx = s.getBox().x + s.getBox().w/2.0;
            double sy = s.getBox().y + s.getBox().h/2.0;
            double mx = this.x + this.w/2.0;
            double my = this.y + this.h/2.0;
            double dx = sx - mx;
            double dy = sy - my;
            double d2 = dx*dx + dy*dy;
            if (d2 < bestDist){ bestDist = d2; target = s; }
        }

        double mv = (double)dt / 16.0; // 标准化到帧率基准，保持速度稳定
        if (target != null){
            double sx = target.getBox().x + target.getBox().w/2.0;
            double sy = target.getBox().y + target.getBox().h/2.0;
            double mx = this.x + this.w/2.0;
            double my = this.y + this.h/2.0;
            double dx = sx - mx;
            double dy = sy - my;
            double dist = Math.sqrt(dx*dx + dy*dy);
            if (dist > 0.1){
                double nx = dx / dist;
                double ny = dy / dist;
                this.vx = nx * speed;
                this.vy = ny * speed;
            }
        } else {
            // 没有目标则向上
            this.vx = 0;
            this.vy = -speed;
        }

        this.x += vx * mv;
        this.y += vy * mv;

        // 出界或超时失效
        if (this.y < -100 || this.y > engine.getHeight() + 100 || this.x < -2000 || this.x > 2000) active = false;
    }

    public boolean isActive(){ return active; }
    public void setActive(boolean v){ this.active = v; }

    // 新增：返回当前朝向角（弧度），用于 UI 旋转图片（atan2(vy, vx)）
    public double getAngle(){
        // 若速度为零则向上（-PI/2），避免 NaN
        if (vx == 0 && vy == 0) return -Math.PI/2.0;
        return Math.atan2(vy, vx);
    }

    @Override
    public Rect getBox(){ return new Rect((int)Math.round(x), (int)Math.round(y), (int)Math.round(w), (int)Math.round(h)); }
}
