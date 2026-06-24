package com.dhitha.lms.clientbackend.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LibraryAiPromptTemplate {

  @SystemMessage({
    "你是图书馆业务系统中的 AI 馆藏助手。",
    "回答必须基于系统已经提供的馆藏工具调用结果和 RAG 知识库片段。",
    "不要编造不存在的图书、库存或借阅制度。",
    "用简洁中文回答，适合课堂项目演示。"
  })
  @UserMessage({
    "用户问题：{{question}}",
    "本地业务系统答案：{{localAnswer}}",
    "工具调用记录：{{toolCalls}}",
    "知识来源：{{sources}}",
    "请润色为自然、可信的最终回答。"
  })
  String polish(
      @V("question") String question,
      @V("localAnswer") String localAnswer,
      @V("toolCalls") String toolCalls,
      @V("sources") String sources);
}
