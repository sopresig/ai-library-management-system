<template>
  <v-container class="px-0">
    <v-row v-if="errorMessage" class="mt-3">
      <v-col cols="12">
        <v-alert
          dense
          outlined
          dismissible
          transition="scale-transition"
          type="error"
        >
          {{
            errorMessage
              ? errorMessage
              : "出现错误，请稍后重试！"
          }}
        </v-alert>
      </v-col>
    </v-row>
    <v-row>
      <v-col>
        <Tabs :tabs="tabs" />
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import Tabs from "@/components/common/Tabs";
import OrderPage from "@/components/admin/orders/OrderPage";
import UserPage from "@/components/admin/users/UserPage";
import BookPage from "@/components/admin/books/BookPage";
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
          name: "订单",
          icon: "mdi-book-multiple",
          component: OrderPage
        },
        {
          id: 1,
          name: "图书",
          icon: "mdi-book-open-page-variant",
          component: BookPage
        },
        {
          id: 2,
          name: "用户",
          icon: "mdi-account-multiple",
          component: UserPage
        },
        {
          id: 3,
          name: "账户",
          icon: "mdi-account-edit",
          component: AccountPage
        },
        {
          id: 4,
          name: "AI 助手",
          icon: "mdi-robot-outline",
          component: LibraryAiAssistant
        }
      ]
    };
  }
};
</script>
