package com.google.mlkit.vision.pose

class PoseDetection {
    class Options {
        companion object {
            fun defaultOptions(): Options = Options()
            fun builder(): Builder = Builder()
        }

        class Builder {
            fun build(): Options = Options()
        }
    }

    class Client {
        companion object {
            fun getClient(options: Options): Client = Client()
        }
    }
}