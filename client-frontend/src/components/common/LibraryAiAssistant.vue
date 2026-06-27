<template>
  <v-container class="ai-shell px-0">
    <v-row>
      <v-col cols="12" md="8">
        <v-card outlined>
          <v-card-title class="py-3 ai-title">
            <div class="d-flex align-center">
              <v-icon left color="primary">mdi-robot-outline</v-icon>
              <span>AI 图书馆助手</span>
            </div>
            <v-spacer />
            <v-btn-toggle
              v-model="selectedModelProvider"
              mandatory
              dense
              class="model-toggle"
            >
              <v-btn
                v-for="option in modelOptions"
                :key="option.value"
                :value="option.value"
                small
              >
                <v-icon left small>{{ option.icon }}</v-icon>
                {{ option.label }}
              </v-btn>
            </v-btn-toggle>
          </v-card-title>
          <v-divider />
          <v-card-text class="chat-window">
            <div
              v-for="item in messages"
              :key="item.id"
              :class="['chat-line', item.role]"
            >
              <div class="chat-bubble">
                <div class="caption font-weight-bold mb-1">
                  {{ item.role === "user" ? "你" : "AI 助手" }}
                </div>
                <div v-if="item.text" class="body-2 pre-line">
                  {{ item.text }}
                </div>
                <div v-else-if="item.status" class="body-2 pending-line">
                  <v-progress-circular
                    indeterminate
                    size="14"
                    width="2"
                    class="mr-2"
                  />
                  {{ item.status }}
                </div>
              </div>
            </div>
          </v-card-text>
          <v-divider />
          <v-card-actions class="align-start">
            <v-textarea
              v-model="question"
              rows="2"
              auto-grow
              outlined
              dense
              hide-details
              label="询问图书推荐、馆藏可借数量、借阅规则..."
              @keydown.ctrl.enter.prevent="send"
            />
            <v-btn
              class="ml-3"
              color="primary"
              :loading="loading"
              :disabled="!question.trim()"
              @click="send"
            >
              <v-icon left>mdi-send</v-icon>
              发送
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card outlined>
          <v-card-title class="py-3">
            <v-icon left color="primary">mdi-source-branch</v-icon>
            AI 调用轨迹
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="mb-3">
              <div class="caption grey--text">当前选择</div>
              <v-chip small color="primary" outlined>{{
                selectedModelLabel
              }}</v-chip>
            </div>
            <div class="mb-3">
              <div class="caption grey--text">意图</div>
              <v-chip small color="primary" outlined>{{
                latest.intent
              }}</v-chip>
            </div>
            <div class="mb-3">
              <div class="caption grey--text">实际模型</div>
              <v-chip small outlined>{{ modelProviderLabel }}</v-chip>
            </div>
            <div class="mb-3">
              <div class="caption grey--text mb-1">业务工具调用</div>
              <v-alert
                v-for="tool in latest.toolCalls"
                :key="tool.name + tool.arguments"
                dense
                outlined
                type="info"
                class="mb-2"
              >
                <strong>{{ tool.name }}</strong>
                <div class="caption">
                  {{ tool.arguments }} -> {{ tool.result }}
                </div>
              </v-alert>
            </div>
            <div>
              <div class="caption grey--text mb-1">知识库来源</div>
              <v-list dense two-line>
                <v-list-item
                  v-for="source in latest.sources"
                  :key="source.title"
                >
                  <v-list-item-content>
                    <v-list-item-title>{{ source.title }}</v-list-item-title>
                    <v-list-item-subtitle>{{
                      source.content
                    }}</v-list-item-subtitle>
                  </v-list-item-content>
                </v-list-item>
              </v-list>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <v-row>
      <v-col cols="12">
        <v-chip
          v-for="sample in samples"
          :key="sample"
          class="mr-2 mb-2"
          outlined
          @click="question = sample"
        >
          {{ sample }}
        </v-chip>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { streamLibraryAi } from "@/service/ai";

export default {
  name: "LibraryAiAssistant",
  data() {
    return {
      question: "",
      loading: false,
      selectedModelProvider: "xiaomi-mimo",
      modelOptions: [
        {
          value: "xiaomi-mimo",
          label: "小米 MiMo",
          icon: "mdi-cloud-outline"
        },
        {
          value: "local-qwen",
          label: "本地 Qwen",
          icon: "mdi-laptop"
        },
        {
          value: "local-rule-rag-demo",
          label: "规则/RAG",
          icon: "mdi-source-branch"
        }
      ],
      sessionId: `session-${Date.now()}`,
      messages: [
        {
          id: 0,
          role: "assistant",
          text:
            "你好，我可以根据兴趣推荐图书、查询馆藏可借数量，也可以回答借阅规则和续借问题。"
        }
      ],
      latest: {
        intent: "就绪",
        modelProvider: "xiaomi-mimo",
        toolCalls: [],
        sources: []
      },
      samples: [
        "请推荐机器学习和 Java 入门书",
        "Clean Code 是否可借？",
        "借阅规则是什么？可以续借吗？"
      ]
    };
  },
  computed: {
    selectedModelLabel() {
      return this.displayModelProvider(this.selectedModelProvider);
    },
    modelProviderLabel() {
      return this.displayModelProvider(this.latest.modelProvider);
    }
  },
  methods: {
    async send() {
      const text = this.question.trim();
      if (!text) {
        return;
      }
      this.loading = true;
      this.messages.push({ id: Date.now(), role: "user", text });
      const assistantMessage = {
        id: Date.now() + 1,
        role: "assistant",
        text: "",
        status: this.pendingMessage()
      };
      this.messages.push(assistantMessage);
      this.latest = {
        ...this.latest,
        intent: "生成中",
        modelProvider: this.selectedModelProvider
      };
      this.question = "";
      try {
        await streamLibraryAi(this.sessionId, text, {
          modelProvider: this.selectedModelProvider,
          onToken: token => {
            assistantMessage.status = "";
            assistantMessage.text += token;
          },
          onDone: response => {
            this.latest = response;
            assistantMessage.text = response.answer || assistantMessage.text;
            assistantMessage.status = "";
          },
          onError: message => {
            assistantMessage.text = message || "AI 流式请求失败。";
            assistantMessage.status = "";
          }
        });
      } catch (error) {
        console.error(error);
        assistantMessage.text = "AI 流式请求失败，请检查后端服务和登录状态。";
        assistantMessage.status = "";
      } finally {
        this.loading = false;
      }
    },
    pendingMessage() {
      if (this.selectedModelProvider === "local-qwen") {
        return "本地 Qwen 正在思考，正式回答生成后会逐字显示...";
      }
      if (this.selectedModelProvider === "xiaomi-mimo") {
        return "小米 MiMo 正在生成回答...";
      }
      return "正在从本地规则和知识库生成回答...";
    },
    displayModelProvider(provider) {
      if (provider === "xiaomi-mimo") {
        return "小米 MiMo";
      }
      if (provider === "local-qwen" || provider === "lm-studio") {
        return "本地 Qwen";
      }
      if (provider === "local-rule-rag-demo") {
        return "本地规则/RAG";
      }
      return provider || "-";
    }
  }
};
</script>

<style scoped>
.ai-title {
  gap: 12px;
}

.model-toggle {
  flex-wrap: nowrap;
}

.chat-window {
  min-height: 360px;
  max-height: 520px;
  overflow-y: auto;
  background: #111827;
  border-top: 1px solid #30363d;
  border-bottom: 1px solid #30363d;
}

.chat-line {
  display: flex;
  margin-bottom: 12px;
}

.chat-line.user {
  justify-content: flex-end;
}

.chat-bubble {
  max-width: 78%;
  padding: 12px 14px;
  border-radius: 8px;
  background: #1f2937;
  border: 1px solid #374151;
  color: #f9fafb;
  line-height: 1.65;
  word-break: break-word;
}

.chat-bubble .caption {
  color: #93c5fd;
}

.chat-line.user .chat-bubble {
  background: #0b4f7a;
  border-color: #1d9bf0;
  color: #ffffff;
}

.chat-line.user .chat-bubble .caption {
  color: #dbeafe;
}

.pre-line {
  white-space: pre-line;
}

.pending-line {
  display: flex;
  align-items: center;
  color: #cbd5e1;
}
</style>
