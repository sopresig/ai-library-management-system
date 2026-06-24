<template>
  <v-container class="ai-shell px-0">
    <v-row>
      <v-col cols="12" md="8">
        <v-card outlined>
          <v-card-title class="py-3">
            <v-icon left color="primary">mdi-robot-outline</v-icon>
            AI Library Assistant
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
                  {{ item.role === "user" ? "You" : "AI Assistant" }}
                </div>
                <div class="body-2 pre-line">{{ item.text }}</div>
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
              label="Ask about recommendations, availability, borrowing rules..."
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
              Send
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
      <v-col cols="12" md="4">
        <v-card outlined>
          <v-card-title class="py-3">
            <v-icon left color="primary">mdi-source-branch</v-icon>
            AI Trace
          </v-card-title>
          <v-divider />
          <v-card-text>
            <div class="mb-3">
              <div class="caption grey--text">Intent</div>
              <v-chip small color="primary" outlined>{{
                latest.intent
              }}</v-chip>
            </div>
            <div class="mb-3">
              <div class="caption grey--text">Model</div>
              <v-chip small outlined>{{ latest.modelProvider }}</v-chip>
            </div>
            <div class="mb-3">
              <div class="caption grey--text mb-1">Function Calling</div>
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
              <div class="caption grey--text mb-1">RAG Sources</div>
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
import { askLibraryAi } from "@/service/ai";

export default {
  name: "LibraryAiAssistant",
  data() {
    return {
      question: "",
      loading: false,
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
        intent: "READY",
        modelProvider: "local-rule-rag-demo",
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
  methods: {
    async send() {
      const text = this.question.trim();
      if (!text) {
        return;
      }
      this.loading = true;
      this.messages.push({ id: Date.now(), role: "user", text });
      this.question = "";
      try {
        const response = await askLibraryAi(this.sessionId, text);
        this.latest = response;
        this.messages.push({
          id: Date.now() + 1,
          role: "assistant",
          text: response.answer
        });
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.chat-window {
  min-height: 360px;
  max-height: 520px;
  overflow-y: auto;
  background: #f8fafc;
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
  background: white;
  border: 1px solid #e0e0e0;
}

.chat-line.user .chat-bubble {
  background: #e3f2fd;
  border-color: #bbdefb;
}

.pre-line {
  white-space: pre-line;
}
</style>
