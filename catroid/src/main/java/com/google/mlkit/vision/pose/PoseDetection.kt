package com.google.mlkit.vision.pose

class PoseDetection {
    companion object {
        fun getClient(options: PoseDetectorOptions): Client = Client()
    }

    class Client {
        fun process(image: com.google.mlkit.vision.common.InputImage) = com.google.android.gms.tasks.Tasks.forResult(Pose())
        fun close() {}
    }
}