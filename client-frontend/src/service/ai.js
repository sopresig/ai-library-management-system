import resource from "@/resource/resource";
import { sendResponse } from "@/util/responseUtil.js";

export const askLibraryAi = async (sessionId, message) => {
  let response = await resource.post("/ai/chat", {
    sessionId,
    message
  });
  return sendResponse(response, 200);
};
