const {onRequest} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");

const GEMINI_MODEL = "gemini-2.5-flash";
const REGION = "asia-northeast3";

exports.recommendPlaceCandidates = onRequest(
  {
    region: REGION,
    timeoutSeconds: 60,
    memory: "256MiB",
    invoker: "public",
    secrets: ["GEMINI_API_KEY"],
  },
  async (req, res) => {
    setCorsHeaders(res);
    if (req.method === "OPTIONS") {
      res.status(204).send("");
      return;
    }
    if (req.method !== "POST") {
      res.status(405).json({error: "POST 요청만 지원합니다."});
      return;
    }

    try {
      const input = normalizeRequest(req.body);
      const apiKey = process.env.GEMINI_API_KEY;
      if (!apiKey) {
        res.status(500).json({error: "GEMINI_API_KEY가 설정되지 않았습니다."});
        return;
      }

      const candidates = await requestGeminiRecommendations(apiKey, input);
      res.status(200).json({candidates});
    } catch (error) {
      logger.error("recommendPlaceCandidates failed", {
        message: error && error.message,
      });
      res.status(error.statusCode || 500).json({
        error: error.publicMessage || "장소 후보를 추천하지 못했습니다.",
      });
    }
  },
);

function setCorsHeaders(res) {
  res.set("Access-Control-Allow-Origin", "*");
  res.set("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
}

function normalizeRequest(body) {
  const roomTitle = asText(body && body.roomTitle, "약속");
  const meetingType = asText(body && body.meetingType, "OTHER");
  const limit = Math.min(Math.max(Number(body && body.limit) || 5, 1), 5);
  const startStations = Array.isArray(body && body.startStations) ?
    body.startStations.map((value) => String(value).trim()).filter(Boolean) :
    [];

  if (startStations.length < 2) {
    throw badRequest("참여자 2명 이상의 출발역이 필요합니다.");
  }

  return {
    roomTitle,
    meetingType,
    limit,
    startStations: [...new Set(startStations)].slice(0, 20),
  };
}

function asText(value, fallback) {
  const text = typeof value === "string" ? value.trim() : "";
  return text || fallback;
}

function badRequest(message) {
  const error = new Error(message);
  error.statusCode = 400;
  error.publicMessage = message;
  return error;
}

async function requestGeminiRecommendations(apiKey, input) {
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${apiKey}`;
  const prompt = buildPrompt(input);
  const response = await fetch(url, {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({
      contents: [{role: "user", parts: [{text: prompt}]}],
      generationConfig: {
        temperature: 0.35,
        responseMimeType: "application/json",
        responseSchema: {
          type: "OBJECT",
          properties: {
            candidates: {
              type: "ARRAY",
              items: {
                type: "OBJECT",
                properties: {
                  name: {type: "STRING"},
                  area: {type: "STRING"},
                  score: {type: "NUMBER"},
                  reason: {type: "STRING"},
                  travelTimes: {
                    type: "ARRAY",
                    items: {
                      type: "OBJECT",
                      properties: {
                        station: {type: "STRING"},
                        minutes: {type: "INTEGER"},
                      },
                      required: ["station", "minutes"],
                    },
                  },
                },
                required: ["name", "area", "score", "reason", "travelTimes"],
              },
            },
          },
          required: ["candidates"],
        },
      },
    }),
  });

  const responseText = await response.text();
  if (!response.ok) {
    logger.error("Gemini API failed", {
      status: response.status,
      body: responseText.slice(0, 500),
    });
    throw new Error("Gemini API 요청에 실패했습니다.");
  }

  const parsed = JSON.parse(responseText);
  const text = parsed.candidates &&
    parsed.candidates[0] &&
    parsed.candidates[0].content &&
    parsed.candidates[0].content.parts &&
    parsed.candidates[0].content.parts[0] &&
    parsed.candidates[0].content.parts[0].text;
  if (!text) throw new Error("Gemini 응답이 비어 있습니다.");

  const payload = JSON.parse(text);
  const rawCandidates = Array.isArray(payload.candidates) ? payload.candidates : [];
  const candidates = rawCandidates
    .map((candidate) => normalizeCandidate(candidate, input.startStations))
    .filter(Boolean)
    .sort((a, b) => b.score - a.score)
    .slice(0, input.limit);

  if (candidates.length === 0) {
    throw new Error("Gemini가 유효한 후보를 반환하지 않았습니다.");
  }
  return candidates;
}

function buildPrompt(input) {
  return [
    "너는 서울/수도권 모임 장소 추천 전문가야.",
    "사용자가 입력한 출발지는 지하철역 이름뿐이야.",
    "실제 경로 계산 API를 쓰지 말고, 지하철 접근성, 환승 편의, 중심성, 모임 유형 적합도를 종합해서 후보지를 추천해.",
    "각 후보지까지 각 출발역에서 대중교통으로 이동할 때의 대략적인 소요시간도 추정해.",
    "각 후보는 점수가 높은 순서로 정렬해.",
    "반드시 JSON만 반환해. 마크다운 금지.",
    "",
    `약속명: ${input.roomTitle}`,
    `모임 유형: ${input.meetingType}`,
    `출발역 목록: ${input.startStations.join(", ")}`,
    `후보 개수: ${input.limit}`,
    "",
    "응답 형식:",
    "{\"candidates\":[{\"name\":\"강남역\",\"area\":\"서울 강남구\",\"score\":92,\"reason\":\"환승 부담이 비교적 고르게 분산돼요.\",\"travelTimes\":[{\"station\":\"서울역\",\"minutes\":25},{\"station\":\"부평역\",\"minutes\":55}]}]}",
    "",
    "제약:",
    "- name은 역/상권/장소 후보지 이름으로 써.",
    "- area는 간단한 지역명으로 써.",
    "- score는 0부터 100까지 숫자.",
    "- reason은 소요시간을 제외한 추천 근거를 한국어 한 문장, 35자 이내로 써.",
    "- travelTimes에는 입력된 모든 출발역을 정확히 한 번씩 포함해.",
    "- minutes는 환승과 도보를 고려한 대략적인 정수 분 단위 시간이야.",
    "- 소요시간은 실제 경로 조회 결과가 아닌 추정치이므로 지나치게 정밀하게 표현하지 마.",
  ].join("\n");
}

function normalizeCandidate(value, startStations) {
  if (!value || typeof value !== "object") return null;
  const name = asText(value.name, "");
  const area = asText(value.area, "");
  const reason = asText(value.reason, "");
  const score = Number(value.score);
  const travelTimes = normalizeTravelTimes(value.travelTimes, startStations);
  if (!name || !reason || !Number.isFinite(score) || !travelTimes) return null;
  const travelSummary = travelTimes
    .map(({station, minutes}) => `${station} 약 ${minutes}분`)
    .join(" · ");
  return {
    name,
    area,
    score: Math.max(0, Math.min(100, score)),
    reason: `예상 이동: ${travelSummary} | ${reason}`,
  };
}

function normalizeTravelTimes(value, startStations) {
  if (!Array.isArray(value)) return null;
  const minutesByStation = new Map();
  value.forEach((item) => {
    if (!item || typeof item !== "object") return;
    const station = asText(item.station, "");
    const minutes = Number(item.minutes);
    if (!station || !Number.isFinite(minutes)) return;
    minutesByStation.set(normalizeStationKey(station), Math.round(minutes));
  });

  const travelTimes = startStations.map((station) => {
    const minutes = minutesByStation.get(normalizeStationKey(station));
    if (!Number.isFinite(minutes)) return null;
    return {
      station,
      minutes: Math.max(1, Math.min(180, minutes)),
    };
  });
  return travelTimes.every(Boolean) ? travelTimes : null;
}

function normalizeStationKey(value) {
  return String(value).trim().toLowerCase().replace(/\s+/g, "").replace(/역$/, "");
}
