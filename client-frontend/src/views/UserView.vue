<template>
  <v-container class="pa-0">
    <v-row v-if="errorMessage">
      <v-col cols="12">
        <v-alert dense outlined transition="scale-transition" type="error">
          {{ errorMessage ? errorMessage : "出现错误，请稍后重试！" }}
        </v-alert>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <Tabs :tabs="tabs" :right="true" />
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import Tabs from "@/components/common/Tabs";
import UserBooksPage from "@/components/user/books/UserBooksPage";
import MyOrdersPage from "@/components/user/orders/MyOrdersPage";
import AccountPage from "@/components/user/account/AccountPage";
import LibraryAiAssistant from "@/components/common/LibraryAiAssistant";
export default {
  methods: {},
  components: {
    Tabs
  },
  computed: {
    errorMessage() {
      return this.$store.getters.getErrorMessage;
    }
  },
  data() {
    return {
      tabs: [
        {
          id: 0,
          name: "图书",
          icon: "mdi-book-open-page-variant",
          component: UserBooksPage
        },
        {
          id: 1,
          name: "我的订单",
          icon: "mdi-book-multiple",
          component: MyOrdersPage
        },
        {
          id: 2,
          name: "账户",
          icon: "mdi-account-edit",
          component: AccountPage
        },
        {
          id: 3,
          name: "AI 助手",
          icon: "mdi-robot-outline",
          component: LibraryAiAssistant
        }
      ]
    };
  }
};
</script>
