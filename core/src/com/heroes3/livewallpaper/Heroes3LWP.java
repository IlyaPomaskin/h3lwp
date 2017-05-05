package com.heroes3.livewallpaper;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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
    private long rectChangeTime = 0;
    private String currentMap;

    @Override
    public void create() {
        camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.setToOrtho(true);
        camera.update();

        batch = new SpriteBatch();

        mapRender = new MapRender(camera);

        setNewRandomRect(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

//        Gdx.input.setInputProcessor(new InputAdapter() {
//            @Override
//            public boolean touchUp(int x, int y, int pointer, int button) {
//                if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
//                    setNewRandomRect(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
//                }
//                return true;
//            }
//        });

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

    private float deltaTime = 0;

    @Override
    public void render() {
        mapRender.assets.update();

        try {
            Thread.sleep(180);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        camera.update();

        batch.setTransformMatrix(camera.view);
        batch.setProjectionMatrix(camera.projection);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        mapRender.drawCache();

        batch.enableBlending();
        batch.begin();
        mapRender.renderSprites(batch);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.update();
        setNewRandomRect(width, height);
    }

    @Override
    public void resume() {
        setNewRandomRect(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void setNewRandomRect(int width, int height) {
        if (TimeUtils.timeSinceMillis(rectChangeTime) > RECT_CHANGE_INTERVAL) {
            rectChangeTime = TimeUtils.millis();
            String nextMap = getRandomMapName();
            if (!nextMap.equals(currentMap)) {
                currentMap = nextMap;
                mapRender.setMap(readMap("maps/" + currentMap));
            }
            mapRender.setRandomRect(width, height);
        }
        ;
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
