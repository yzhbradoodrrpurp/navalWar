package entities;

public abstract class Entity {
    protected double x, y;
    protected double w, h;
    protected boolean active = true;

    public abstract void update(long dt);
    public boolean isActive(){ return active; }
    public void setActive(boolean v){ this.active = v; }

    public Rect getBox(){ return new Rect((int)x, (int)y, (int)w, (int)h); }

    public static class Rect {
        public final int x, y, w, h;
        public Rect(int x, int y, int w, int h){ this.x = x; this.y = y; this.w = w; this.h = h; }
    }
}

