#include <jni.h>
#include <llama.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <mutex>
#include <ctime>


#define TAG "AiAgentJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::mutex g_mutex;
static struct llama_model * g_model = nullptr;
static struct llama_context * g_ctx = nullptr;
static bool g_backend_initialized = false;

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_catrobat_catroid_ai_model_ModelRuntime_nativeLoadModel(
    JNIEnv * env,
    jclass /*cls*/,
    jstring path_jstr,
    jint n_ctx) {

    std::lock_guard<std::mutex> lock(g_mutex);

    const char * path = env->GetStringUTFChars(path_jstr, nullptr);
    LOGI("Loading model from: %s", path);

    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
        // Seed the C PRNG once so temperature sampling is non-deterministic.
        srand((unsigned int)time(nullptr));
    }

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;

    g_model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(path_jstr, path);

    if (!g_model) {
        LOGE("Failed to load model");
        return 0;
    }
    LOGI("Model loaded successfully");

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t)(n_ctx > 0 ? n_ctx : 4096);
    ctx_params.n_batch = 512;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create context");
        llama_model_free(g_model);
        g_model = nullptr;
        return 0;
    }
    LOGI("Context created with n_ctx=%u", ctx_params.n_ctx);

    return (jlong)(intptr_t)g_ctx;
}

JNIEXPORT void JNICALL
Java_org_catrobat_catroid_ai_model_ModelRuntime_nativeUnloadModel(
    JNIEnv * /*env*/,
    jclass /*cls*/,
    jlong context_ptr) {

    std::lock_guard<std::mutex> lock(g_mutex);

    if (context_ptr != 0) {
        struct llama_context * ctx = (struct llama_context *)(intptr_t)context_ptr;
        if (ctx == g_ctx) {
            g_ctx = nullptr;
        }
        llama_free(ctx);
        LOGI("Context freed");
    }
    if (g_model) {
        llama_model_free(g_model);
        g_model = nullptr;
        LOGI("Model freed");
    }
}

JNIEXPORT jstring JNICALL
Java_org_catrobat_catroid_ai_model_ModelRuntime_nativeGenerate(
    JNIEnv * env,
    jclass /*cls*/,
    jlong context_ptr,
    jstring prompt_jstr,
    jfloat temperature,
    jint max_tokens) {

    // Lock so concurrent unloadModel() cannot free g_model/g_ctx while we generate.
    std::lock_guard<std::mutex> lock(g_mutex);

    if (context_ptr == 0 || g_ctx == nullptr || g_model == nullptr) {
        return env->NewStringUTF("ERROR: null context");
    }

    const char * prompt_cstr = env->GetStringUTFChars(prompt_jstr, nullptr);
    std::string prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt_jstr, prompt_cstr);

    struct llama_context * ctx = (struct llama_context *)(intptr_t)context_ptr;
    const struct llama_vocab * vocab = llama_model_get_vocab(g_model);
    if (!vocab) {
        return env->NewStringUTF("ERROR: null vocab");
    }

    // Get n_ctx from the context so we can enforce the hard limit.
    uint32_t n_ctx = llama_n_ctx(ctx);

    int n_tokens = prompt.length() + 256;
    std::vector<llama_token> tokens(n_tokens);

    int token_count = llama_tokenize(
        vocab,
        prompt.c_str(),
        (int32_t)prompt.length(),
        tokens.data(),
        (int32_t)n_tokens,
        false,
        false);

    if (token_count < 0) {
        n_tokens = -token_count;
        tokens.resize(n_tokens);
        token_count = llama_tokenize(
            vocab,
            prompt.c_str(),
            (int32_t)prompt.length(),
            tokens.data(),
            (int32_t)n_tokens,
            false,
            false);
        if (token_count < 0) {
            LOGE("Tokenization failed: %d", token_count);
            return env->NewStringUTF("ERROR: tokenization failed");
        }
    }

    // Guard: if the prompt alone exceeds the context window, truncate it from
    // the FRONT (keep the most recent tokens) to leave room for at least 32
    // generated tokens. This prevents llama_decode from crashing.
    int32_t max_gen = max_tokens > 0 ? max_tokens : 256;
    if (token_count >= (int32_t)n_ctx) {
        int keep = (int32_t)n_ctx - 32;
        if (keep <= 0) {
            return env->NewStringUTF("ERROR: context too small for this prompt");
        }
        // Keep the LAST `keep` tokens (tail of the prompt)
        int drop = token_count - keep;
        tokens.erase(tokens.begin(), tokens.begin() + drop);
        token_count = keep;
        LOGI("Prompt truncated: dropped first %d token(s) to fit n_ctx=%u", drop, n_ctx);
    }

    tokens.resize(token_count);

    // max_gen already calculated above before truncation guard.
    std::string result;
    result.reserve(max_gen * 8);

    bool is_first = true;
    for (int32_t i = 0; i < max_gen; i++) {
        llama_batch batch = llama_batch_get_one(tokens.data(), (int32_t)tokens.size());
        int32_t decode_result = llama_decode(ctx, batch);

        if (decode_result < 0) {
            LOGE("llama_decode error: %d", decode_result);
            break;
        }

        auto * logits = llama_get_logits_ith(ctx, -1);
        if (!logits) {
            LOGE("Failed to get logits");
            break;
        }

        int32_t n_vocab = llama_vocab_n_tokens(vocab);
        if (temperature < 0.001f) {
            int32_t max_idx = 0;
            for (int32_t j = 1; j < n_vocab; j++) {
                if (logits[j] > logits[max_idx]) {
                    max_idx = j;
                }
            }
            tokens[0] = max_idx;
        } else {
            std::vector<float> probs(n_vocab);
            float max_logit = logits[0];
            for (int32_t j = 1; j < n_vocab; j++) {
                if (logits[j] > max_logit) max_logit = logits[j];
            }
            float sum = 0.0f;
            for (int32_t j = 0; j < n_vocab; j++) {
                probs[j] = expf((logits[j] - max_logit) / temperature);
                sum += probs[j];
            }
            float r = (float)rand() / (float)RAND_MAX * sum;
            float cum = 0.0f;
            int32_t chosen = n_vocab - 1;
            for (int32_t j = 0; j < n_vocab; j++) {
                cum += probs[j];
                if (r <= cum) { chosen = j; break; }
            }
            tokens[0] = chosen;
        }

        if (llama_vocab_is_eog(vocab, tokens[0])) {
            break;
        }

        // 64 bytes is large enough for any UTF-8 sequence (emoji, CJK, etc.)
        char piece[64];
        int32_t piece_len = llama_token_to_piece(
            vocab,
            tokens[0],
            piece,
            sizeof(piece),
            (is_first && llama_vocab_get_add_bos(vocab)) ? 1 : 0,
            false);
        is_first = false;

        if (piece_len > 0) {
            result.append(piece, piece_len);
        }
        tokens.resize(1);
    }

    LOGI("Generated %zu bytes", result.size());
    return env->NewStringUTF(result.c_str());
}

} // extern "C"
