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
              <v-col cols="12" class="pb-0 mb-0">
                <v-radio-group v-model="overdueType" row>
                  <v-radio
                    label="领取"
                    value="collection"
                    color="green"
                  ></v-radio>
                  <v-radio label="归还" value="return" color="green"></v-radio>
                </v-radio-group>
              </v-col>
              <v-col cols="12" class="pb-0 mb-0">
                <v-radio-group v-model="overdueUserType" row>
                  <v-radio label="用户" value="user" color="green"></v-radio>
                  <v-radio label="全部" value="all" color="green"></v-radio>
                </v-radio-group>
              </v-col>
              <v-col
                cols="12"
                sm="4"
                class="pt-0 mt-0"
                v-if="overdueUserType == 'user'"
              >
                <v-text-field
                  label="用户 ID"
                  v-model="overdueUser"
                  :rules="[rules.required]"
                ></v-text-field>
              </v-col>
              <v-col cols="12" sm="4">
                <v-btn
                  text
                  color="green"
                  type="submit"
                  class="pa-0"
                  dark
                  :disabled="!valid"
                >
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
  name: "FindOrdersOverdue",
  data() {
    return {
      headers: [
        {
          text: "订单 ID",
          align: "start",
          value: "id",
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
          text: "最晚领取",
          value: "collectBy",
          sortable: false,
          class: "indigo--text darken-4"
        },
        {
          text: "最晚归还",
          value: "returnBy",
          sortable: false,
          class: "indigo--text darken-4"
        }
      ],
      orders: [],
      loading: false,
      valid: false,
      message: null,
      isError: false,
      rules: ruleUtil.rules,
      overdueType: "collection",
      overdueUserType: "all",
      overdueUser: null
    };
  },
  methods: {
    findOrderHistory() {
      this.setMarkerParams(true, null, false);
      orderService
        .findOrderOverdue(
          this.overdueType,
          this.overdueUserType,
          this.overdueUser
        )
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
  watch: {
    orderType(newValue) {
      switch (newValue) {
        case "all":
          this.orderTypeLabel = "订单 ID";
          break;
        case "user":
          this.orderTypeLabel = "用户 ID";
          break;
        case "book":
          this.orderTypeLabel = "图书 ID";
          break;
      }
    }
  },
  mounted() {
    this.findOrderHistory();
  }
};
</script>

<style></style>
