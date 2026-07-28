// ============================================================================
// NeoCatroid Telegram Proxy — Cloudflare Worker
// ----------------------------------------------------------------------------
// Прокси для Telegram Bot API (обходит CORS). Токен вшит в код.
// ============================================================================

// SECURITY: tokens must come from Worker environment bindings, never hardcoded.
// Configure via wrangler.toml / Cloudflare dashboard: TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID
const BOT_TOKEN = (env) => env.TELEGRAM_BOT_TOKEN;
const CHAT_ID = (env) => env.TELEGRAM_CHAT_ID;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
      "Access-Control-Allow-Headers": "*",
    };

    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    // ---------- POST /api/telegram/upload ----------
    if (request.method === "POST" && url.pathname === "/api/telegram/upload") {
      return handleUpload(request, env, corsHeaders);
    }

    // ---------- GET /download?file_id=... ----------
    if (request.method === "GET" && url.pathname === "/download") {
      return handleDownload(request, env, corsHeaders);
    }

    // ---------- GET /check?file_id=...  (validate only) ----------
    if (request.method === "GET" && url.pathname === "/check") {
      return handleCheck(request, env, corsHeaders);
    }

    return new Response("Not found", { status: 404, headers: corsHeaders });
  },
};

async function handleUpload(request, env, cors) {
  let form;
  try {
    form = await request.formData();
  } catch (e) {
    return json({ ok: false, error: "invalid_form" }, 400, cors);
  }

  const document = form.get("document");
  if (!document) {
    return json({ ok: false, error: "no_file" }, 400, cors);
  }

  try {
    const tgForm = new FormData();
    tgForm.append("chat_id", CHAT_ID(env));
    tgForm.append("document", document);

    const res = await fetch(`https://api.telegram.org/bot${BOT_TOKEN(env)}/sendDocument`, {
      method: "POST",
      body: tgForm,
    });
    const result = await res.json();
    return json(result, res.status, cors);
  } catch (e) {
    return json({ ok: false, error: "telegram_error" }, 502, cors);
  }
}

async function handleDownload(request, env, cors) {
  const fileId = request.url.searchParams.get("file_id");
  if (!fileId || !/^[A-Za-z0-9_-]+$/i.test(fileId)) {
    return json({ ok: false, error: "bad_request" }, 400, cors);
  }
  try {
    const res = await fetch(`https://api.telegram.org/bot${BOT_TOKEN(env)}/getFile?file_id=${encodeURIComponent(fileId)}`);
    const data = await res.json();
    if (!data.ok || !data.result || !data.result.file_path) {
      return json({ ok: false, error: "invalid" }, 404, cors);
    }
    const fileUrl = `https://api.telegram.org/file/bot${BOT_TOKEN(env)}/${data.result.file_path}`;
    return Response.redirect(fileUrl, 302);
  } catch (e) {
    return json({ ok: false, error: "telegram_error" }, 502, cors);
  }
}

async function handleCheck(request, env, cors) {
  const fileId = request.url.searchParams.get("file_id");
  if (!fileId || !/^[A-Za-z0-9_-]+$/i.test(fileId)) {
    return json({ ok: false, error: "bad_request" }, 400, cors);
  }
  try {
    const res = await fetch(`https://api.telegram.org/bot${BOT_TOKEN(env)}/getFile?file_id=${encodeURIComponent(fileId)}`);
    const data = await res.json();
    return json({ ok: data.ok && data.result && !!data.result.file_path }, data.ok ? 200 : 404, cors);
  } catch (e) {
    return json({ ok: false, error: "telegram_error" }, 502, cors);
  }
}

function json(obj, status, cors) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}

