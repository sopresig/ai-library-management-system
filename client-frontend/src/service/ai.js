import resource from "@/resource/resource";
import { sendResponse } from "@/util/responseUtil.js";

export const askLibraryAi = async (sessionId, message, modelProvider) => {
  let response = await resource.post("/ai/chat", {
    sessionId,
    message,
    modelProvider
  });
  return sendResponse(response, 200);
};

export const streamLibraryAi = async (
  sessionId,
  message,
  { modelProvider, onToken, onDone, onError } = {}
) => {
  const response = await fetch(`${resource.defaults.baseURL}/ai/chat/stream`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream"
    },
    body: JSON.stringify({
      sessionId,
      message,
      modelProvider
    })
  });

  if (!response.ok) {
    throw new Error(`AI stream request failed with status ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  let reading = true;
  while (reading) {
    const { done, value } = await reader.read();
    if (done) {
      reading = false;
      break;
    }
    buffer += decoder.decode(value, { stream: true });
    buffer = consumeSseBuffer(buffer, { onToken, onDone, onError });
  }

  buffer += decoder.decode();
  consumeSseBuffer(`${buffer}\n\n`, { onToken, onDone, onError });
};

const consumeSseBuffer = (buffer, handlers) => {
  let normalized = buffer.replace(/\r\n/g, "\n");
  let boundary = normalized.indexOf("\n\n");

  while (boundary !== -1) {
    const rawEvent = normalized.slice(0, boundary);
    dispatchSseEvent(rawEvent, handlers);
    normalized = normalized.slice(boundary + 2);
    boundary = normalized.indexOf("\n\n");
  }

  return normalized;
};

const dispatchSseEvent = (rawEvent, { onToken, onDone, onError }) => {
  if (!rawEvent.trim()) {
    return;
  }

  const lines = rawEvent.split("\n");
  const eventLine = lines.find(line => line.startsWith("event:"));
  const eventName = eventLine
    ? eventLine.slice("event:".length).trim()
    : "message";
  const data = lines
    .filter(line => line.startsWith("data:"))
    .map(line => line.slice("data:".length).replace(/^ /, ""))
    .join("\n");

  if (eventName === "token") {
    onToken && onToken(data);
  } else if (eventName === "done") {
    onDone && onDone(JSON.parse(data));
  } else if (eventName === "error") {
    onError && onError(data);
  }
};
