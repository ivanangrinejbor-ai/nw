package org.catrobat.catroid.raptor;

public interface PlayableAudio {
    void play();
    void stop();
    void dispose();
    boolean isPlaying();

    void setInstanceName(String name);
    String getInstanceName();

    void setPosition(com.badlogic.gdx.math.Vector3 position);
    com.badlogic.gdx.math.Vector3 getPosition();

    void setAttachedObjectId(String id);
    String getAttachedObjectId();

    void update3D(float volume, float pan);
}