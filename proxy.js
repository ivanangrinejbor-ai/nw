// ============================================================================
// NeoCatroid Telegram Proxy — Локальный сервер
// ----------------------------------------------------------------------------
// Запуск: node proxy.js
// Слушает на http://localhost:3000
// Пересылает запросы в Telegram Bot API (обходит CORS)
// ============================================================================

const http = require("http");
const https = require("https");

// SECURITY: never hardcode the bot token. Provide it via env:
//   TELEGRAM_BOT_TOKEN=123456:ABC... node proxy.js
const BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN;
if (!BOT_TOKEN) {
  console.warn("[proxy] TELEGRAM_BOT_TOKEN is not set — Telegram calls will fail. Export it before starting.");
}
const PORT = process.env.PORT || 3000;

function tgRequest(method, body, callback) {
  const url = new URL(`https://api.telegram.org/bot${BOT_TOKEN}/${method}`);
  const opts = {
    hostname: url.hostname,
    path: url.pathname + url.search,
    method: "POST",
    headers: body
      ? { "Content-Type": "multipart/form-data; boundary=" + body.boundary, "Content-Length": body.length }
      : {},
  };
  const req = https.request(opts, (res) => {
    let data = "";
    res.on("data", (c) => (data += c));
    res.on("end", () => {
      try {
        callback(null, { status: res.statusCode, body: JSON.parse(data) });
      } catch (e) {
        callback(null, { status: res.statusCode, body: data });
      }
    });
  });
  req.on("error", (e) => callback(e));
  if (body && body.data) req.write(body.data);
  req.end();
}

function parseBoundary(req) {
  const ct = req.headers["content-type"] || "";
  const m = ct.match(/boundary=(.+)/);
  return m ? m[1] : null;
}

function collectBody(req, callback) {
  const chunks = [];
  req.on("data", (c) => chunks.push(c));
  req.on("end", () => callback(null, Buffer.concat(chunks)));
  req.on("error", callback);
}

const server = http.createServer((req, res) => {
  // CORS
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "*");

  if (req.method === "OPTIONS") {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://localhost:${PORT}`);

  // GET / — health check + debug info
  if (req.method === "GET" && (url.pathname === "/" || url.pathname === "")) {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ ok: true, method: req.method, path: url.pathname, url: req.url, query: Object.fromEntries(url.searchParams) }));
    return;
  }

  // POST /api/telegram/upload — пересылает файл в Telegram
  if (req.method === "POST" && url.pathname === "/api/telegram/upload") {
    const boundary = parseBoundary(req);
    if (!boundary) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ok: false, error: "no_boundary" }));
      return;
    }
    collectBody(req, (err, raw) => {
      if (err) {
        res.writeHead(500);
        res.end(JSON.stringify({ ok: false, error: "server_error" }));
        return;
      }
      // Подменяем chat_id и отправляем в Telegram
      const bound = Buffer.from("--" + boundary);
      const chatLine = Buffer.from("\r\n\r\n-1003736105697");
      const chatField = Buffer.from('Content-Disposition: form-data; name="chat_id"\r\n\r\n-1003736105697');

      // Находим первый field (должен быть document) и добавляем chat_id перед ним
      // Формируем правильный multipart с chat_id первым
      const docStart = raw.indexOf(bound);
      if (docStart === -1) {
        res.writeHead(400);
        res.end(JSON.stringify({ ok: false, error: "parse_error" }));
        return;
      }

      const header = Buffer.from(
        `--${boundary}\r\nContent-Disposition: form-data; name="chat_id"\r\n\r\n-1003736105697\r\n`
      );
      // Всё остальное как есть, кроме первого boundary
      const bodyRaw = Buffer.concat([header, raw.slice(docStart)]);

      const tgBody = {
        data: bodyRaw,
        boundary: boundary,
        length: bodyRaw.length,
      };

      tgRequest("sendDocument", tgBody, (err, result) => {
        if (err) {
          res.writeHead(502, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: false, error: "telegram_error" }));
          return;
        }
        res.writeHead(result.status, { "Content-Type": "application/json" });
        res.end(JSON.stringify(result.body));
      });
    });
    return;
  }

  // GET /download?file_id=... — редиректит на файл из Telegram
  if (req.method === "GET" && url.pathname === "/download") {
    const fileId = url.searchParams.get("file_id");
    if (!fileId) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ok: false, error: "bad_request" }));
      return;
    }
    https.get(`https://api.telegram.org/bot${BOT_TOKEN}/getFile?file_id=${encodeURIComponent(fileId)}`, (tgRes) => {
      let data = "";
      tgRes.on("data", (c) => (data += c));
      tgRes.on("end", () => {
        try {
          const j = JSON.parse(data);
          if (j.ok && j.result && j.result.file_path) {
            res.writeHead(302, { Location: `https://api.telegram.org/file/bot${BOT_TOKEN}/${j.result.file_path}` });
            res.end();
          } else {
            res.writeHead(404, { "Content-Type": "application/json" });
            res.end(JSON.stringify({ ok: false, error: "invalid" }));
          }
        } catch (e) {
          res.writeHead(502, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: false, error: "parse_error" }));
        }
      });
    });
    return;
  }

  // GET /check?file_id=... — проверяет, валидный ли file_id
  if (req.method === "GET" && url.pathname === "/check") {
    const fileId = url.searchParams.get("file_id");
    if (!fileId) {
      res.writeHead(400, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ ok: false, error: "bad_request" }));
      return;
    }
    https.get(`https://api.telegram.org/bot${BOT_TOKEN}/getFile?file_id=${encodeURIComponent(fileId)}`, (tgRes) => {
      let data = "";
      tgRes.on("data", (c) => (data += c));
      tgRes.on("end", () => {
        try {
          const j = JSON.parse(data);
          res.writeHead(j.ok ? 200 : 404, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: j.ok && !!j.result?.file_path }));
        } catch (e) {
          res.writeHead(502, { "Content-Type": "application/json" });
          res.end(JSON.stringify({ ok: false }));
        }
      });
    });
    return;
  }

  res.writeHead(404);
  res.end("Not found");
});

server.listen(PORT, () => {
  console.log(`NeoCatroid proxy listening on http://localhost:${PORT}`);
});
