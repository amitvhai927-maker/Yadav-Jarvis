import express from "express";
import rateLimit from "express-rate-limit";

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "12kb" }));

const port = process.env.PORT || 3000;
const apiKey = process.env.OPENAI_API_KEY;
const appToken = process.env.APP_TOKEN;
const model = process.env.AI_MODEL || "gpt-4.1-mini";

app.get("/", (_req, res) => {
  res.json({ service: "Yadav AI backend", status: "ok" });
});

app.use("/chat", rateLimit({
  windowMs: 60 * 1000,
  limit: 20,
  standardHeaders: "draft-7",
  legacyHeaders: false,
  message: { error: "Too many requests. Please wait a minute." }
}));

app.post("/chat", async (req, res) => {
  if (!appToken || req.get("X-App-Token") !== appToken) {
    return res.status(401).json({ error: "Unauthorized app request." });
  }
  if (!apiKey) {
    return res.status(503).json({ error: "AI service is not configured." });
  }

  const message = typeof req.body?.message === "string" ? req.body.message.trim() : "";
  if (!message) return res.status(400).json({ error: "message is required." });
  if (message.length > 4000) return res.status(413).json({ error: "Message too long." });

  try {
    const upstream = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${apiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model,
        instructions: "You are Yadav AI, a helpful, safe voice assistant. Keep answers concise and natural for text-to-speech. Do not claim you performed phone actions unless the app reports that action separately.",
        input: message,
        max_output_tokens: 500
      })
    });

    const data = await upstream.json();
    if (!upstream.ok) {
      console.error("AI provider error status:", upstream.status);
      return res.status(502).json({ error: "AI provider request failed." });
    }

    const reply = typeof data.output_text === "string" ? data.output_text.trim() : "";
    return res.json({ reply: reply || "I could not generate a response. Please try again." });
  } catch (err) {
    console.error("AI request failed:", err?.message || "unknown error");
    return res.status(502).json({ error: "Could not contact AI provider." });
  }
});

app.listen(port, "0.0.0.0", () => {
  console.log(`Yadav AI backend listening on port ${port}`);
});
