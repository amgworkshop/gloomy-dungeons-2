package zame.game.engine;

import android.content.res.Resources;
import javax.microedition.khronos.opengles.GL10;
import zame.game.MyApplication;
import zame.game.store.Achievements;

public class Overlay implements EngineObject {
	public static final int BLOOD = 1;
	public static final int ITEM = 2;
	public static final int MARK = 3;

	protected static float[][] COLORS = new float[][] {
		new float[] { 1.0f, 0.0f, 0.0f },	// BLOOD
		new float[] { 1.0f, 1.0f, 1.0f },	// ITEM
		new float[] { 1.0f, 1.0f, 1.0f }	// MARK
	};

	protected Engine engine;
	protected Config config;
	protected Renderer renderer;
	protected State state;
	protected Labels labels;
	protected Resources resources;
	protected int overlayType = 0;
	protected long overlayTime = 0;
	protected String shownLabel = null;
	protected long labelTime = 0;

	public void setEngine(Engine engine) {
		this.engine = engine;
		this.config = engine.config;
		this.renderer = engine.renderer;
		this.state = engine.state;
		this.labels = engine.labels;
		this.resources = MyApplication.self.getResources();
	}

	public void init() {
		overlayType = 0;
		shownLabel = null;
	}

	public void showOverlay(int type) {
		overlayType = type;
		overlayTime = engine.elapsedTime;
	}

	public void showLabel(int type) {
		shownLabel = labels.map[type];
		labelTime = engine.elapsedTime;
	}

	public void showAchievement(int resId) {
		shownLabel = String.format(
			labels.map[Labels.LABEL_ACHIEVEMENT_UNLOCKED],
			Achievements.cleanupTitle(resources.getString(resId))
		);

		labelTime = engine.elapsedTime;
	}

	protected void appendOverlayColor(float r, float g, float b, float a) {
		float d = renderer.a1 + a - renderer.a1 * a;

		if (d < 0.001) {
			return;
		}

		renderer.r1 = (renderer.r1 * renderer.a1 - renderer.r1 * renderer.a1 * a + r * a) / d;
		renderer.g1 = (renderer.g1 * renderer.a1 - renderer.g1 * renderer.a1 * a + g * a) / d;
		renderer.b1 = (renderer.b1 * renderer.a1 - renderer.b1 * renderer.a1 * a + b * a) / d;
		renderer.a1 = d;
	}

	public void renderOverlay(GL10 gl) {
		renderer.r1 = 0.0f;
		renderer.g1 = 0.0f;
		renderer.b1 = 0.0f;
		renderer.a1 = 0.0f;

		float bloodAlpha = Math.max(0.0f, 0.4f - ((float)state.heroHealth / 20.0f) * 0.4f);	// less than 20 health - show blood overlay

		if (bloodAlpha > 0.0f) {
			appendOverlayColor(COLORS[BLOOD - 1][0], COLORS[BLOOD - 1][1], COLORS[BLOOD - 1][2], bloodAlpha);
		}

		if (overlayType != 0 && !engine.inWallpaperMode) {
			float alpha = 0.5f - (float)(engine.elapsedTime - overlayTime) / 300.0f;

			if (alpha > 0.0f) {
				appendOverlayColor(COLORS[overlayType - 1][0], COLORS[overlayType - 1][1], COLORS[overlayType - 1][2], alpha);
			} else {
				overlayType = 0;
			}
		}

		if (renderer.a1 < 0.001f) {
			return;
		}

		renderer.initOrtho(gl, true, false, 0.0f, 1.0f, 0.0f, 1.0f, 0f, 1.0f);
		renderer.init();

		renderer.r2 = renderer.r1; renderer.g2 = renderer.g1; renderer.b2 = renderer.b1; renderer.a2 = renderer.a1;
		renderer.r3 = renderer.r1; renderer.g3 = renderer.g1; renderer.b3 = renderer.b1; renderer.a3 = renderer.a1;
		renderer.r4 = renderer.r1; renderer.g4 = renderer.g1; renderer.b4 = renderer.b1; renderer.a4 = renderer.a1;

		renderer.setQuadOrthoCoords(0.0f, 0.0f, 1.0f, 1.0f);
		renderer.drawQuad();

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		renderer.flush(gl, false);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
	}

	protected void renderLabelLine(GL10 gl, float sy, float ey, String str, float op) {
		renderer.initOrtho(gl, true, false, -1.0f, 1.0f, -1.0f, 1.0f, 0f, 1.0f);
		renderer.init();

		if (engine.game.renderMode == Game.RENDER_MODE_GAME) {
			renderer.setQuadRGBA(0.0f, 0.0f, 0.0f, op * 0.25f);
		} else {
			renderer.setQuadRGBA(1.0f, 0.0f, 0.0f, op * 0.5f);
		}

		float my = sy + (ey - sy) * 0.5f;
		renderer.setQuadOrthoCoords(-1.0f, my - 0.25f, 1.0f, my + 0.25f);
		renderer.drawQuad();

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		renderer.flush(gl, false);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();

		labels.beginDrawing(gl);
		renderer.setQuadRGBA(1.0f, 1.0f, 1.0f, op);
		labels.draw(gl, -engine.ratio + 0.1f, sy, engine.ratio - 0.1f, ey, str, 0.25f, Labels.ALIGN_CC);
		labels.endDrawing(gl);
	}

	public void renderLabels(GL10 gl) {
		if (engine.inWallpaperMode || shownLabel == null && state.shownMessageId < 0) {
			return;
		}

		if (shownLabel != null) {
			float op = Math.min(1.0f, 3.0f - (float)(engine.elapsedTime - labelTime) / 500.0f);

			if (op <= 0.0f) {
				shownLabel = null;
			} else {
				renderLabelLine(gl, -0.75f, (state.shownMessageId >= 0 ? 0.0f : 1.0f), shownLabel, op);
			}
		}

		if (state.shownMessageId >= 0 && state.shownMessageId < Labels.LABEL_LAST) {
			renderLabelLine(gl, 0.0f, 0.75f, labels.map[state.shownMessageId], 1.0f);
		}

		labels.endDrawing(gl);
	}

	public void renderEndLevelLayer(GL10 gl, float dt) {
		renderer.initOrtho(gl, true, false, 0.0f, 1.0f, 0.0f, 1.0f, 0f, 1.0f);
		renderer.init();

		renderer.a2 = Math.min(1.0f, dt) * 0.9f;
		renderer.a3 = renderer.a2;

		renderer.a1 = Math.min(1.0f, dt * 0.5f) * 0.5f;
		renderer.a4 = renderer.a1;

		renderer.setQuadRGB(0.0f, 0.0f, 0.0f);
		renderer.setQuadOrthoCoords(0.0f, 0.0f, 1.0f, 1.0f);
		renderer.drawQuad();

		gl.glShadeModel(GL10.GL_SMOOTH);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		renderer.flush(gl, false);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
	}

	public void renderGammaLayer(GL10 gl) {
		renderer.initOrtho(gl, true, false, 0.0f, 1.0f, 0.0f, 1.0f, 0f, 1.0f);
		renderer.init();

		renderer.setQuadRGBA(1.0f, 1.0f, 1.0f, config.gamma);
		renderer.setQuadOrthoCoords(0.0f, 0.0f, 1.0f, 1.0f);
		renderer.drawQuad();

		gl.glShadeModel(GL10.GL_FLAT);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE);
		renderer.flush(gl, false);

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glPopMatrix();
	}
}
