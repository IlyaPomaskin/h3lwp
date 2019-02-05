package com.heroes3.livewallpaper;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Random;

import MapReader.Map;
import MapReader.MapReader;

public class Heroes3LWP extends ApplicationAdapter {
    private final static int RECT_CHANGE_INTERVAL = 1000 * 60 * 30; // 30 minutes in milliseconds
    private SpriteBatch batch;
    private MapRender mapRender;
    private OrthographicCamera camera;
    private long lastRectChangeTime = 0;
    private float zoomLevel = 0.5f;

    @Override
    public void create() {
        clj.core a = new clj.core();
        System.out.println(a.getStringWithPrefix("str"));

        camera = new OrthographicCamera(
                Gdx.graphics.getWidth() * zoomLevel,
                Gdx.graphics.getHeight() * zoomLevel
        );
        camera.zoom = zoomLevel;
        camera.setToOrtho(true);
        camera.update();
        batch = new SpriteBatch();
        mapRender = new MapRender(readMap("maps/" + getRandomMapName()));
        setRandomCameraPosition();
        Gdx.graphics.setContinuousRendering(true);
    }

    private Map readMap(String filename) {
        InputStream file = Gdx.files.internal(filename).read();

        try {
            return new MapReader(file).read();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void render() {
        mapRender.assets.finishLoading();

        try {
            Thread.sleep(180);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        camera.update();

        batch.setTransformMatrix(camera.view);
        batch.setProjectionMatrix(camera.projection);

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.enableBlending();
        batch.begin();
        mapRender.renderTerrain(batch);
        mapRender.renderSprites(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.update();
        setRandomCameraPosition();
    }

    @Override
    public void resume() {
        setRandomCameraPosition();
    }

    private void setRandomCameraPosition() {
        if (TimeUtils.timeSinceMillis(lastRectChangeTime) < RECT_CHANGE_INTERVAL) {
            return;
        }

        lastRectChangeTime = TimeUtils.millis();

        int offset = 3 * MapRender.TILE_SIZE;
        int screenWidth = Math.round(Gdx.graphics.getWidth() * camera.zoom);
        int screenHeight = Math.round(Gdx.graphics.getHeight() * camera.zoom);
        float rectX = MathUtils.random(offset, mapRender.map.size * MapRender.TILE_SIZE - screenWidth - offset);
        float rectY = MathUtils.random(offset + screenWidth, mapRender.map.size * MapRender.TILE_SIZE - screenHeight - offset);

        camera.position.set(
                (camera.viewportWidth / 2f * camera.zoom) + rectX,
                (camera.viewportHeight / 2f * camera.zoom) + rectY,
                0
        );
    }

    private String getRandomMapName() {
        FileHandle[] maps = Gdx.files.internal("maps").list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".h3m");
            }
        });

        int id = new Random(TimeUtils.millis()).nextInt(maps.length);
        return maps[id].name();
    }
}
