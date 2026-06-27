<template>
  <v-card class="mb-12">
    <v-card-text>
      <v-card-title>订单详情</v-card-title>
      <v-list v-if="selectedNewBook">
        <v-list-item>
          <v-list-item-icon>
            <v-icon>mdi-book-open-page-variant</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>名称</v-list-item-title>
            <v-list-item-subtitle>
              <span class="green--text">
                {{ selectedNewBook.name }}
              </span>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-icon>
            <v-icon>mdi-account</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>作者</v-list-item-title>
            <v-list-item-subtitle>
              <span class="green--text">
                {{ selectedNewBook.author }}
              </span>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <v-list-item-icon>
            <v-icon>mdi-home</v-icon>
          </v-list-item-icon>
          <v-list-item-content>
            <v-list-item-title>出版社</v-list-item-title>
            <v-list-item-subtitle>
              <span class="green--text">
                {{ selectedNewBook.publication }}
              </span>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card-text>
    <v-card-actions>
      <v-btn text color="green" @click="orderBook">
        下单
      </v-btn>
      <v-btn text @click="goBack">
        返回
      </v-btn>
    </v-card-actions>
  </v-card>
</template>

<script>
import * as orderService from "@/service/order";
export default {
  name: "OrderBook",
  props: {
    selectedNewBook: {
      type: Object,
      default: null
    }
  },
  methods: {
    orderBook() {
      orderService
        .orderNewBook(this.selectedNewBook)
        .then(() => {
          this.$emit("orderBook");
        })
        .catch(err => {
          this.$store.commit("setErrorMessage", err.error_description);
        });
    },
    goBack() {
      this.$emit("goBack");
    }
  }
};
</script>

<style></style>
