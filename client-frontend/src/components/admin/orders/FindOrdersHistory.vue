<template>
  <v-card>
    <v-container>
      <v-row v-if="message">
        <v-col cols="12">
          <v-card class="elevation-5">
            <v-alert
              dense
              outlined
              dismissible
              transition="scale-transition"
              :type="isError ? 'error' : 'success'"
            >
              {{ message }}
            </v-alert>
          </v-card>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="12">
          <v-form
            @submit.prevent="findOrderHistory"
            v-model="valid"
            ref="searchForm"
          >
            <v-row>
              <v-col cols="12" sm="4" class="pt-0 mt-0">
                <v-text-field
                  label="用户 ID"
                  v-model="orderData"
                  :rules="[rules.required]"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="4">
                <v-btn text color="green" type="submit" dark :disabled="!valid">
                  搜索
                </v-btn>
              </v-col>
            </v-row>
          </v-form>
        </v-col>
      </v-row>
    </v-container>
    <v-data-table
      :headers="headers"
      :items="orders"
      class="elevation-20"
      item-key="id"
      :loading="loading"
      fixed-header
      height="50vh"
      :footer-props="{
        'items-per-page-text': '每页订单数'
      }"
      dense
    >
    </v-data-table>
  </v-card>
</template>

<script>
import * as ruleUtil from "@/util/ruleUtil";
import * as orderService from "@/service/order";
export default {
  name: "FindOrdersHistory",
  data() {
    return {
      headers: [
        {
          text: "订单 ID",
          align: "start",
          value: "orderId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "用户 ID",
          value: "userId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "图书 ID",
          value: "bookId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "图书名称",
          value: "bookName",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "ISBN",
          value: "bookIsbn",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "馆藏编号",
          value: "bookReferenceId",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "下单时间",
          value: "orderedAt",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "领取时间",
          value: "collectedAt",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "归还时间",
          value: "returnedAt",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "逾期费用",
          value: "lateFees",
          sortable: false,
          class: "indigo--text darken-4"
        }
      ],
      orders: [],
      loading: false,
      valid: false,
      message: null,
      isError: false,
      orderData: 2,
      rules: ruleUtil.rules
    };
  },
  methods: {
    findOrderHistory() {
      this.setMarkerParams(true, null, false);
      orderService
        .findOrdersHistoryOfUser(this.orderData)
        .then(data => {
          this.orders = data;
          this.loading = false;
        })
        .catch(err => {
          this.setMarkerParams(false, err.error_description, true);
        });
    },
    setMarkerParams(loading, message, isError) {
      this.loading = loading;
      this.message = message;
      this.isError = isError;
    }
  },
  mounted() {
    this.findOrderHistory();
  }
};
</script>

<style></style>
