package org.catrobat.catroid.raptor;

public abstract class PostProcessingData {
    public boolean isEnabled = true;
    public abstract String getType();


    public static class Bloom extends PostProcessingData {
        public float threshold = 0.8f;
        public float intensity = 1.2f;
        public int blurPasses = 2;
        public float blurAmount = 1.5f;
        public float size = 1f;

        @Override public String getType() { return "Bloom"; }
    }

    public static class Vignette extends PostProcessingData {
        public float intensity = 0.5f;
        public float saturation = 0.2f;

        @Override public String getType() { return "Vignette"; }
    }

    public static class Grain extends PostProcessingData {
        public float amount = 0.03f;

        @Override public String getType() { return "Film Grain"; }
    }

    public static class Levels extends PostProcessingData {
        public float contrast = 1.0f;
        public float saturation = 1.0f;
        public float gamma = 1.0f;

        @Override public String getType() { return "Levels"; }
    }

    public static class Chromatic extends PostProcessingData {
        public float maxDistortion = 4.0f;
        public float strength = 0.003f;

        @Override public String getType() { return "Chromatic Aberration"; }
    }

    public static class Fxaa extends PostProcessingData {
        @Override public String getType() { return "FXAA"; }
    }

    public static class RadialBlur extends PostProcessingData {
        public float strength = 0.2f;
        public int blurPasses = 2;
        public float size = 1f;

        @Override public String getType() { return "Radial Blur"; }
    }

    public static class OldTv extends PostProcessingData {
        public float strength = 0.5f;
        @Override public String getType() { return "Old TV"; }
    }

    public static class Gaussian extends PostProcessingData {
        public int passes = 1;
        public float amount = 1.0f;
        public float size = 1f;

        @Override public String getType() { return "Gaussian Blur"; }
    }

    public static class Zoom extends PostProcessingData {
        public float zoom = 0.25f;
        public float originX = 0.5f;
        public float originY = 0.5f;
        @Override public String getType() { return "Zoom Blur"; }
    }

    public static class Crt extends PostProcessingData {
        public float distortion = 0.3f;
        public float zoom = 1.0f;
        @Override public String getType() { return "CRT Monitor"; }
    }

    public static class Fisheye extends PostProcessingData {
        public float intensity = 0.4f;
        @Override public String getType() { return "Fisheye"; }
    }

    public static class Water extends PostProcessingData {
        public float speed = 1.0f;
        public float amount = 1.0f;
        @Override public String getType() { return "Water Distortion"; }
    }

    public static class MotionBlur extends PostProcessingData {
        public float blurOpacity = 0.5f;
        @Override public String getType() { return "Motion Blur"; }
    }

    public static class LensFlare extends PostProcessingData {
        public float intensity = 1.0f;
        public float threshold = 0.9f;
        public float dispersal = 0.4f;
        public float size = 2f;

        @Override public String getType() { return "Lens Flare"; }
    }

    public static class EyeAdaptation extends PostProcessingData {
        public float targetLuminance = 0.05f;
        public float speed = 2.0f;
        public float minExposure = 0.2f;
        public float maxExposure = 8.0f;

        @Override public String getType() { return "Eye Adaptation"; }
    }

    public static class ACES extends PostProcessingData {
        @Override public String getType() { return "ACES Tonemapping"; }
    }
}