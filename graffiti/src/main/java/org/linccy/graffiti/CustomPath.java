package org.linccy.graffiti;

import android.graphics.Path;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: archko 2024/8/7 :16:38
 */
public class CustomPath extends Path implements Serializable {

    private static final long serialVersionUID = -5974912367682897467L;

    private List<PathAction> actions = new ArrayList<>();

    public List<PathAction> getActions() {
        return actions;
    }

    public void setActions(List<PathAction> actions) {
        this.actions = actions;
        if (this.actions == null) {
            this.actions = new ArrayList<>();
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        drawThisPath();
    }

    @Override
    public void moveTo(float x, float y) {
        actions.add(new ActionMove(x, y));
        super.moveTo(x, y);
    }

    @Override
    public void lineTo(float x, float y) {
        actions.add(new ActionLine(x, y));
        super.lineTo(x, y);
    }

    @Override
    public void quadTo(float x1, float y1, float x2, float y2) {
        actions.add(new ActionQuad(x1, y1, x2, y2));
        super.quadTo(x1, y1, x2, y2);
    }

    public void drawThisPath() {
        PathAction p;
        for (int i = 0; i < actions.size(); i++) {
            p = actions.get(i);
            if (p.getType().equals(PathAction.PathActionType.MOVE_TO)) {
                super.moveTo(p.getX(), p.getY());
            } else if (p.getType().equals(PathAction.PathActionType.LINE_TO)) {
                super.lineTo(p.getX(), p.getY());
            } else if (p.getType().equals(PathAction.PathActionType.QUAD_TO)) {
                super.quadTo(p.getX(), p.getY(), p.getX2(), p.getY2());
            }
        }
    }

    public interface PathAction {
        enum PathActionType {LINE_TO, MOVE_TO, QUAD_TO}

        PathActionType getType();

        float getX();

        float getY();

        float getX2();

        float getY2();
    }

    public static class ActionMove implements PathAction, Serializable {
        private static final long serialVersionUID = -7198142191254133295L;

        private final float x;
        private final float y;

        public ActionMove(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public PathActionType getType() {
            return PathActionType.MOVE_TO;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public float getX2() {
            return 0;
        }

        @Override
        public float getY2() {
            return 0;
        }
    }

    public static class ActionLine implements PathAction, Serializable {
        private static final long serialVersionUID = 8307137961494172589L;

        private final float x;
        private final float y;

        public ActionLine(float x, float y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public PathActionType getType() {
            return PathActionType.LINE_TO;
        }

        @Override
        public float getX() {
            return x;
        }

        @Override
        public float getY() {
            return y;
        }

        @Override
        public float getX2() {
            return 0;
        }

        @Override
        public float getY2() {
            return 0;
        }
    }

    public static class ActionQuad implements PathAction, Serializable {
        private static final long serialVersionUID = 8307137961494172589L;

        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;

        public ActionQuad(float x1, float y1, float x2, float y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public PathActionType getType() {
            return PathActionType.QUAD_TO;
        }

        @Override
        public float getX() {
            return x1;
        }

        @Override
        public float getY() {
            return y1;
        }

        @Override
        public float getX2() {
            return x2;
        }

        @Override
        public float getY2() {
            return y2;
        }
    }
}