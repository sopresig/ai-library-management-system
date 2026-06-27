<template>
  <v-dialog v-model="dialog" fullscreen persistent overlay-opacity="1">
    <template v-slot:activator="{ on, attrs }">
      <v-btn dark color="deep-purple darken-4" v-bind="attrs" v-on="on">
        <v-icon left dark>
          mdi-book-plus
        </v-icon>
        新建订单
      </v-btn>
    </template>
    <!-- Re render on open-->
    <v-card v-if="dialog">
      <v-toolbar flat>
        <v-toolbar-title>新建订单</v-toolbar-title>
        <v-divider class="mx-5" inset vertical></v-divider>
        <v-spacer />
        <h3 v-if="$vuetify.breakpoint.mobile && stepNumber === 1">
          选择图书
        </h3>
        <h3 v-if="$vuetify.breakpoint.mobile && stepNumber === 2">
          查询馆藏
        </h3>
        <h3 v-if="$vuetify.breakpoint.mobile && stepNumber === 3">
          确认订单
        </h3>
        <v-spacer />
        <v-btn icon @click="close">
          <v-icon>mdi-close-circle</v-icon>
        </v-btn>
      </v-toolbar>
      <v-alert
        dense
        outlined
        type="error"
        transition="scale-transition"
        v-if="errorMessage"
      >
        {{
          errorMessage
            ? errorMessage
            : "出现错误，请稍后重试！"
        }}
      </v-alert>
      <v-stepper v-model="stepNumber">
        <v-stepper-header>
          <v-stepper-step :complete="stepNumber > 1" step="1" color="green">
            选择图书
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step :complete="stepNumber > 2" step="2" color="green">
            查询馆藏
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step step="3" color="green">
            确认订单
          </v-stepper-step>
        </v-stepper-header>

        <v-stepper-items>
          <!-- 选择图书 -->
          <v-stepper-content step="1" class="pa-2">
            <ChooseBook @searchInventory="searchInventory" />
          </v-stepper-content>
          <!-- 查询馆藏 -->
          <v-stepper-content step="2">
            <SearchInventory
              :selectedNewBook="selectedNewBook"
              @goBack="goBack"
              @finalizeOrder="finalizeOrder"
            />
          </v-stepper-content>
          <!-- 下单 -->
          <v-stepper-content step="3">
            <OrderBook
              :selectedNewBook="selectedNewBook"
              @goBack="goBack"
              @orderBook="orderBook"
            />
          </v-stepper-content>
        </v-stepper-items>
      </v-stepper>
    </v-card>
  </v-dialog>
</template>

<script>
import ChooseBook from "./ChooseBook";
import SearchInventory from "./SearchInventory";
import OrderBook from "./OrderBook";
export default {
  name: "NewOrder",
  components: {
    ChooseBook,
    SearchInventory,
    OrderBook
  },
  data: () => ({
    dialog: false,
    stepNumber: 1,
    selectedNewBook: null,
    error: null
  }),
  computed: {
    errorMessage() {
      return this.$store.getters.getErrorMessage;
    }
  },
  methods: {
    close() {
      this.stepNumber = 1;
      this.dialog = false;
      this.$store.commit("setErrorMessage", null);
    },
    searchInventory(book) {
      this.goContinue();
      this.selectedNewBook = book;
    },
    finalizeOrder() {
      this.goContinue();
    },
    orderBook() {
      this.$emit("newAdded");
      this.close();
    },
    goBack() {
      this.stepNumber--;
    },
    goContinue() {
      this.stepNumber++;
    }
  }
};
</script>

<style></style>
