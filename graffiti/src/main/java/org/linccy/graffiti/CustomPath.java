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

    public void drawThisPath() {
        PathAction p;
        for (int i = actions.size() - 1; i >= 0; i--) {
            p = actions.get(i);
            if (p.getType().equals(PathAction.PathActionType.MOVE_TO)) {
                super.moveTo(p.getX(), p.getY());
            } else if (p.getType().equals(PathAction.PathActionType.LINE_TO)) {
                super.lineTo(p.getX(), p.getY());
            }
        }
    }

    public interface PathAction {
        enum PathActionType {LINE_TO, MOVE_TO}

        PathActionType getType();

        float getX();

        float getY();
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

    }
}